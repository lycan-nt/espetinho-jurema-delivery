package br.com.espetinhojurema.application.service;

import br.com.espetinhojurema.application.model.ItemPedidoView;
import br.com.espetinhojurema.application.model.PedidoDetalheView;
import br.com.espetinhojurema.domain.model.PedidoStatus;
import br.com.espetinhojurema.domain.model.PontoCarne;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
     * Itens sem ponto da carne na ordem do pedido; em seguida espetinhos agrupados por {@link PontoCarne}.
     * Dentro de cada ponto, o mesmo produto com a mesma observação é consolidado em uma única linha (quantidades somadas).
     * Linhas de pontos entre grupos (e antes do primeiro quando há itens sem ponto).
     */
    private static void appendSecaoItens(
            StringBuilder sb, PedidoDetalheView p, boolean incluirTotal, Long itemIdCorte) {
        List<ItemPedidoView> visiveis = new ArrayList<>();
        for (ItemPedidoView item : p.itens()) {
            if (item.cancelado()) {
                continue;
            }
            if (itemIdCorte != null && item.id() <= itemIdCorte) {
                continue;
            }
            visiveis.add(item);
        }
        if (visiveis.isEmpty()) {
            sb.append("  (sem itens)\n");
            return;
        }

        List<ItemPedidoView> semPonto = new ArrayList<>();
        Map<PontoCarne, List<ItemPedidoView>> comPonto = new LinkedHashMap<>();
        for (ItemPedidoView item : visiveis) {
            if (item.pontoCarne() == null) {
                semPonto.add(item);
            } else {
                comPonto.computeIfAbsent(item.pontoCarne(), k -> new ArrayList<>()).add(item);
            }
        }

        for (ItemPedidoView item : semPonto) {
            appendCabecalhoItem(sb, item, incluirTotal);
            appendObservacoesItem(sb, item);
        }

        boolean primeiroGrupoPonto = true;
        for (Map.Entry<PontoCarne, List<ItemPedidoView>> e : comPonto.entrySet()) {
            boolean linhaPontosAntes = !semPonto.isEmpty() || !primeiroGrupoPonto;
            if (linhaPontosAntes) {
                sb.append(separador()).append('\n');
            }
            primeiroGrupoPonto = false;

            PontoCarne ponto = e.getKey();
            sb.append(" - Ponto: ").append(ponto.rotulo()).append(":\n");
            for (LinhaAgregadaPonto agg : agregarPorProdutoObservacao(e.getValue())) {
                appendLinhaItemBlocoPontoAgregado(sb, agg, incluirTotal);
            }
        }
    }

    /** Soma quantidades no mesmo bloco de ponto quando produto e observação coincidem (ordem da primeira ocorrência). */
    private static List<LinhaAgregadaPonto> agregarPorProdutoObservacao(List<ItemPedidoView> itens) {
        Map<String, LinhaAgregadaPonto> map = new LinkedHashMap<>();
        for (ItemPedidoView item : itens) {
            String key = item.produtoId() + "|" + observacaoChave(item.observacao());
            LinhaAgregadaPonto agg = map.get(key);
            if (agg == null) {
                map.put(key, LinhaAgregadaPonto.criar(item));
            } else {
                agg.somar(item);
            }
        }
        return new ArrayList<>(map.values());
    }

    private static String observacaoChave(String obs) {
        if (obs == null || obs.isBlank()) {
            return "";
        }
        return obs.strip();
    }

    private static final class LinhaAgregadaPonto {
        final String produtoNome;
        final String observacaoExibir;
        int quantidade;
        BigDecimal subtotal;

        private LinhaAgregadaPonto(String produtoNome, String observacaoExibir) {
            this.produtoNome = produtoNome;
            this.observacaoExibir = observacaoExibir;
        }

        static LinhaAgregadaPonto criar(ItemPedidoView i) {
            String obsEx =
                    i.observacao() == null || i.observacao().isBlank() ? "" : i.observacao().strip();
            LinhaAgregadaPonto l = new LinhaAgregadaPonto(i.produtoNome(), obsEx);
            l.quantidade = i.quantidade();
            l.subtotal = i.precoUnitario().multiply(BigDecimal.valueOf(i.quantidade()));
            return l;
        }

        void somar(ItemPedidoView i) {
            quantidade += i.quantidade();
            subtotal = subtotal.add(i.precoUnitario().multiply(BigDecimal.valueOf(i.quantidade())));
        }
    }

    private static void appendLinhaItemBlocoPontoAgregado(
            StringBuilder sb, LinhaAgregadaPonto agg, boolean incluirTotal) {
        if (incluirTotal) {
            BigDecimal unit = agg.subtotal
                    .divide(BigDecimal.valueOf(agg.quantidade), 4, RoundingMode.HALF_UP)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal subtotalFmt = agg.subtotal.setScale(2, RoundingMode.HALF_UP);
            sb.append("  ")
                    .append(agg.quantidade)
                    .append("x ")
                    .append(agg.produtoNome)
                    .append('\n');
            sb.append("     un R$ ")
                    .append(formatValorPt(unit))
                    .append(" x ")
                    .append(agg.quantidade)
                    .append(" = R$ ")
                    .append(formatValorPt(subtotalFmt))
                    .append('\n');
        } else {
            sb.append("  ")
                    .append(agg.quantidade)
                    .append("x ")
                    .append(agg.produtoNome)
                    .append('\n');
        }
        if (!agg.observacaoExibir.isBlank()) {
            appendObservacoesTexto(sb, agg.observacaoExibir, "    ");
        }
    }

    /** Observações por item no layout “QTD ITEM” (indentação original). */
    private static void appendObservacoesItem(StringBuilder sb, ItemPedidoView item) {
        appendObservacoesItemIndent(sb, item, "     ");
    }

    private static void appendObservacoesItemIndent(StringBuilder sb, ItemPedidoView item, String indentLista) {
        if (item.observacao() == null || item.observacao().isBlank()) {
            return;
        }
        appendObservacoesTexto(sb, item.observacao(), indentLista);
    }

    private static void appendObservacoesTexto(StringBuilder sb, String observacao, String indentLista) {
        for (String linha : observacao.split("[\r\n]+")) {
            if (!linha.isBlank()) {
                sb.append(indentLista).append("- ").append(linha.trim()).append('\n');
            }
        }
    }

    /**
     * Comanda normal: linha compacta (quantidade alinhada). Fechamento: quantidade visível sem espaços
     * à esquerda (algumas térmicas cortam padding) + linha com valor unitário e total do item.
     */
    private static void appendCabecalhoItem(StringBuilder sb, ItemPedidoView item, boolean incluirTotal) {
        if (incluirTotal) {
            BigDecimal unit =
                    item.precoUnitario().setScale(2, RoundingMode.HALF_UP);
            BigDecimal subtotal = unit
                    .multiply(BigDecimal.valueOf(item.quantidade()))
                    .setScale(2, RoundingMode.HALF_UP);
            sb.append(item.quantidade())
                    .append("x ")
                    .append(item.produtoNome())
                    .append('\n');
            sb.append("   un R$ ")
                    .append(formatValorPt(unit))
                    .append(" x ")
                    .append(item.quantidade())
                    .append(" = R$ ")
                    .append(formatValorPt(subtotal))
                    .append('\n');
        } else {
            sb.append(String.format("%3dx %s%n", item.quantidade(), item.produtoNome()));
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
}
