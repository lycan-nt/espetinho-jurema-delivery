package br.com.espetinhojurema.application.service;

import br.com.espetinhojurema.application.model.ItemPedidoView;
import br.com.espetinhojurema.application.model.PedidoDetalheView;
import br.com.espetinhojurema.domain.model.PedidoStatus;
import br.com.espetinhojurema.domain.model.PontoCarne;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Service;

/**
 * Gera o texto da comanda de cozinha: cabeçalho com dados da empresa (ver {@link CabecalhoEmpresaTicketService}),
 * data/hora e Nº do pedido, mesa, itens e rodapé.
 *
 * <pre>
 * CNPJ: ...
 * NOME DA EMPRESA
 * ...
 * TEL: ... EMAIL: ...
 * ------------------------------
 * 29/04/2026 17:46:08     Nº: 123
 * ------------------------------
 *          MESA - 1
 * ................................
 * </pre>
 */
@Service
public class ComandaCozinhaTextoService {

    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FMT_DATA_HORA_SEG =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZONA);

    /** Largura de colunas — igual ao TicketTextoLayout.COLUNAS (32). */
    private static final int W = 32;

    /** Avanço para poder rasgar a bobina confortavelmente. */
    private static final String FEED_FINAL = "\n".repeat(8);

    private final CabecalhoEmpresaTicketService cabecalhoEmpresaTicketService;

    public ComandaCozinhaTextoService(CabecalhoEmpresaTicketService cabecalhoEmpresaTicketService) {
        this.cabecalhoEmpresaTicketService = cabecalhoEmpresaTicketService;
    }

    /** Gera comanda sem total e sem filtro de itens — usado para solicitação de fechamento. */
    public String gerar(PedidoDetalheView p) {
        return gerar(p, false, null);
    }

    /**
     * @param incluirTotal {@code true} para solicitação de fechamento (precisa do total);
     *                     {@code false} para comanda de cozinha comum.
     */
    public String gerar(PedidoDetalheView p, boolean incluirTotal) {
        return gerar(p, incluirTotal, null);
    }

    /**
     * @param incluirTotal  {@code true} para solicitação de fechamento (imprime o total).
     * @param itemIdCorte   Quando não-nulo, imprime apenas itens com {@code id > itemIdCorte} (itens novos).
     *                      {@code null} imprime todos os itens ativos.
     */
    public String gerar(PedidoDetalheView p, boolean incluirTotal, Long itemIdCorte) {
        StringBuilder sb = new StringBuilder();

        cabecalhoEmpresaTicketService.appendCabecalhoEmpresaEIdentificacao(sb, p, true);

        // ── Mesa / tipo centralizado ──────────────────────────────────────────
        String tituloMesa = p.mesaNumero() != null
                ? "MESA - " + p.mesaNumero()
                : p.tipo().name();
        sb.append(centralizar(tituloMesa)).append('\n');

        // ── Separador ────────────────────────────────────────────────────────
        sb.append(separador()).append('\n');

        // ── Cabeçalho de colunas ──────────────────────────────────────────────
        if (incluirTotal) {
            sb.append(centralizar("FECHAMENTO / VALORES")).append('\n');
        } else {
            sb.append("QTD  ITEM\n");
        }

        // ── Separador ────────────────────────────────────────────────────────
        sb.append(separador()).append('\n');

        // ── Itens ─────────────────────────────────────────────────────────────
        appendSecaoItens(sb, p, incluirTotal, itemIdCorte);
        appendCouvertArtistico(sb, p, incluirTotal);
        appendTaxaGarcom(sb, p, incluirTotal);

        // ── Separador ────────────────────────────────────────────────────────
        sb.append(separador()).append('\n');

        // ── Total e valores já quitados: solicitação de fechamento ────────────
        if (incluirTotal) {
            BigDecimal total = p.total() != null ? p.total() : BigDecimal.ZERO;
            BigDecimal pago = p.totalPago() != null ? p.totalPago() : BigDecimal.ZERO;
            BigDecimal rest = p.restante() != null ? p.restante() : total.subtract(pago);
            if (rest.compareTo(BigDecimal.ZERO) < 0) {
                rest = BigDecimal.ZERO;
            }
            sb.append("TOTAL DA CONTA: R$ ").append(formatValorPt(total)).append('\n');
            sb.append("JA PAGO: R$ ").append(formatValorPt(pago)).append('\n');
            sb.append("FALTA PAGAR: R$ ").append(formatValorPt(rest)).append('\n');
            if (p.status() != PedidoStatus.PAGO && pago.compareTo(BigDecimal.ZERO) > 0) {
                sb.append("(conta em aberto - parcial)\n");
            }
            sb.append(separador()).append('\n');
        }

        // ── Rodapé: info do pedido ────────────────────────────────────────────
        if (p.clienteNome() != null && !p.clienteNome().isBlank()) {
            sb.append("Cliente: ").append(p.clienteNome()).append('\n');
        }
        if (p.pessoas() != null && p.pessoas() > 0) {
            sb.append("N.Pessoas: ").append(p.pessoas()).append('\n');
        }

        if (p.descricaoMesa() != null && !p.descricaoMesa().isBlank()) {
            sb.append("Obs.mesa: ").append(p.descricaoMesa()).append('\n');
        }

        // ── Data e hora (rodapé) ────────────────────────────────────────────────
        sb.append(FMT_DATA_HORA_SEG.format(p.criadoEm())).append('\n');

        // ── Separador ────────────────────────────────────────────────────────
        sb.append(separador()).append('\n');

        // ── Disclaimer ───────────────────────────────────────────────────────
        sb.append(centralizar("** NAO E DOCUMENTO FISCAL **")).append('\n');

        // ── Feed final ───────────────────────────────────────────────────────
        sb.append(FEED_FINAL);

        return sb.toString();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Agrupa por (produtoId + ponto da carne + observação), somando quantidades. A ordem das linhas é a
     * da primeira ocorrência de cada grupo no pedido (ex.: carne ao ponto, frango, novo carne ao ponto
     * → uma linha com a quantidade total de carne ao ponto antes do frango).
     * Formato: linha {@code QTD ITEM}, depois {@code - Ponto: ...} quando houver, depois obs.
     */
    private static void appendSecaoItens(
            StringBuilder sb, PedidoDetalheView p, boolean incluirTotal, Long itemIdCorte) {
        LinkedHashMap<String, GrupoItem> mapa = new LinkedHashMap<>();
        for (ItemPedidoView item : p.itens()) {
            if (item.cancelado()) {
                continue;
            }
            if (itemIdCorte != null && item.id() <= itemIdCorte) {
                continue;
            }
            String pontoKey = item.pontoCarne() == null ? "" : item.pontoCarne().name();
            String obsKey = observacaoChave(item.observacao());
            String chave = item.produtoId() + "|" + pontoKey + "|" + obsKey;
            GrupoItem g = mapa.get(chave);
            if (g == null) {
                mapa.put(chave, GrupoItem.criar(item));
            } else {
                g.adicionar(item);
            }
        }

        if (mapa.isEmpty()) {
            sb.append("  (sem itens)\n");
            return;
        }

        for (GrupoItem g : mapa.values()) {
            appendLinhaGrupo(sb, g, incluirTotal);
        }
    }

    private static void appendLinhaGrupo(StringBuilder sb, GrupoItem g, boolean incluirTotal) {
        if (incluirTotal) {
            BigDecimal unit = g.subtotal
                    .divide(BigDecimal.valueOf(g.quantidade), 4, RoundingMode.HALF_UP)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal subtotalFmt = g.subtotal.setScale(2, RoundingMode.HALF_UP);
            sb.append(g.quantidade).append("x ").append(g.produtoNome).append('\n');
            sb.append("   un R$ ").append(formatValorPt(unit))
                    .append(" x ").append(g.quantidade)
                    .append(" = R$ ").append(formatValorPt(subtotalFmt)).append('\n');
        } else {
            sb.append(String.format("%3dx %s%n", g.quantidade, g.produtoNome));
        }
        if (g.pontoCarne != null) {
            sb.append("     - Ponto: ").append(g.pontoCarne.rotulo()).append('\n');
        }
        if (!g.observacao.isBlank()) {
            for (String linha : g.observacao.split("[\r\n]+")) {
                if (!linha.isBlank()) {
                    sb.append("     - ").append(linha.trim()).append('\n');
                }
            }
        }
    }

    private static String observacaoChave(String obs) {
        if (obs == null || obs.isBlank()) {
            return "";
        }
        return obs.strip();
    }

    private static final class GrupoItem {
        final String produtoNome;
        final PontoCarne pontoCarne;
        final String observacao;
        int quantidade;
        BigDecimal subtotal;

        private GrupoItem(String produtoNome, PontoCarne pontoCarne, String observacao) {
            this.produtoNome = produtoNome;
            this.pontoCarne = pontoCarne;
            this.observacao = observacao;
        }

        static GrupoItem criar(ItemPedidoView i) {
            String obsEx = observacaoChave(i.observacao());
            GrupoItem g = new GrupoItem(i.produtoNome(), i.pontoCarne(), obsEx);
            g.quantidade = i.quantidade();
            g.subtotal = i.precoUnitario().multiply(BigDecimal.valueOf(i.quantidade()));
            return g;
        }

        void adicionar(ItemPedidoView i) {
            quantidade += i.quantidade();
            subtotal = subtotal.add(i.precoUnitario().multiply(BigDecimal.valueOf(i.quantidade())));
        }
    }

    private static String formatValorPt(BigDecimal valor) {
        String s = valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
        int p = s.indexOf('.');
        if (p < 0) return s + ",00";
        return s.substring(0, p) + ',' + s.substring(p + 1);
    }

    /** Linha de pontos com {@link #W} caracteres. */
    private static String separador() {
        return ".".repeat(W);
    }

    /**
     * Texto centralizado na largura {@link #W}.
     * Trunca se ultrapassar, sem padding de espaços no final (não necessário para RAW).
     */
    private static String centralizar(String texto) {
        String t = texto.strip();
        if (t.length() >= W) return t;
        int pad = (W - t.length()) / 2;
        return " ".repeat(pad) + t;
    }

    private static void appendCouvertArtistico(StringBuilder sb, PedidoDetalheView p, boolean incluirTotal) {
        if (!incluirTotal) {
            return;
        }
        BigDecimal valor = p.valorCouvertArtistico();
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        int pessoas = p.couvertPessoasCobradas() != null ? p.couvertPessoasCobradas() : 1;
        BigDecimal unit = p.valorCouvertPorPessoa() != null ? p.valorCouvertPorPessoa() : valor;
        sb.append(pessoas).append("x COUVERT ARTISTICO").append('\n');
        sb.append("   un R$ ")
                .append(formatValorPt(unit))
                .append(" x ")
                .append(pessoas)
                .append(" = R$ ")
                .append(formatValorPt(valor))
                .append('\n');
    }

    private static void appendTaxaGarcom(StringBuilder sb, PedidoDetalheView p, boolean incluirTotal) {
        if (!incluirTotal) {
            return;
        }
        BigDecimal valor = p.valorTaxaGarcom();
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal pct = p.taxaGarcomPercentualAplicado();
        String rotuloPct = pct != null ? formatValorPt(pct) + "%" : "";
        sb.append("TAXA GARCOM (").append(rotuloPct).append(")").append('\n');
        sb.append("   R$ ").append(formatValorPt(valor)).append('\n');
    }
}
