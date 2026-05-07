package br.com.espetinhojurema.application.service;

import br.com.espetinhojurema.application.model.PedidoDetalheView;
import br.com.espetinhojurema.domain.model.PedidoStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

/**
 * Gera o texto da comanda de cozinha seguindo o layout de referência:
 *
 * <pre>
 * ESPETINHO JUREMA    PED:#1
 * ................................
 *          MESA - 1
 * ................................
 * QTD  ITEM
 * ................................
 *   1  Espetinho de Carne
 *      - Ponto: bem passada
 *      - Obs.: sem cebola (exemplo livre)
 * ................................
 * N.Pessoas: 2
 * 29/04/2026               21:22
 * ................................
 *  ** NAO E DOCUMENTO FISCAL **
 * </pre>
 */
@Service
public class ComandaCozinhaTextoService {

    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZONA);
    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("HH:mm").withZone(ZONA);

    /** Largura de colunas — igual ao TicketTextoLayout.COLUNAS (32). */
    private static final int W = 32;

    /** Avanço para poder rasgar a bobina confortavelmente. */
    private static final String FEED_FINAL = "\n".repeat(8);

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

        // ── Cabeçalho: empresa à esquerda, pedido à direita ──────────────────
        String empresa = "ESPETINHO JUREMA";
        String ped     = "PED:#" + p.id();
        sb.append(linhaDupla(empresa, ped)).append('\n');

        // ── Separador ────────────────────────────────────────────────────────
        sb.append(separador()).append('\n');

        // ── Mesa / tipo centralizado ──────────────────────────────────────────
        String tituloMesa = p.mesaNumero() != null
                ? "MESA - " + p.mesaNumero()
                : p.tipo().name();
        sb.append(centralizar(tituloMesa)).append('\n');

        // ── Separador ────────────────────────────────────────────────────────
        sb.append(separador()).append('\n');

        // ── Cabeçalho de colunas ──────────────────────────────────────────────
        sb.append("QTD  ITEM\n");

        // ── Separador ────────────────────────────────────────────────────────
        sb.append(separador()).append('\n');

        // ── Itens ─────────────────────────────────────────────────────────────
        boolean temItens = false;
        for (var item : p.itens()) {
            if (item.cancelado()) continue;
            if (itemIdCorte != null && item.id() <= itemIdCorte) continue;
            temItens = true;
            sb.append(String.format("%3dx %s%n", item.quantidade(), item.produtoNome()));
            if (item.pontoCarne() != null) {
                sb.append("     - Ponto: ").append(item.pontoCarne().rotulo()).append('\n');
            }
            if (item.observacao() != null && !item.observacao().isBlank()) {
                // cada linha da observação indentada
                for (String linha : item.observacao().split("[\r\n]+")) {
                    if (!linha.isBlank()) {
                        sb.append("     - ").append(linha.trim()).append('\n');
                    }
                }
            }
        }
        if (!temItens) {
            sb.append("  (sem itens)\n");
        }

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

        // ── Data e hora na mesma linha ────────────────────────────────────────
        String data = FMT_DATA.format(p.criadoEm());
        String hora = FMT_HORA.format(p.criadoEm());
        sb.append(linhaDupla(data, hora)).append('\n');

        // ── Separador ────────────────────────────────────────────────────────
        sb.append(separador()).append('\n');

        // ── Disclaimer ───────────────────────────────────────────────────────
        sb.append(centralizar("** NAO E DOCUMENTO FISCAL **")).append('\n');

        // ── Feed final ───────────────────────────────────────────────────────
        sb.append(FEED_FINAL);

        return sb.toString();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

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

    /**
     * Esquerda alinhada e direita alinhada na mesma linha de {@link #W} colunas.
     * Se não couber, imprime cada uma numa linha separada.
     */
    private static String linhaDupla(String esq, String dir) {
        int espacos = W - esq.length() - dir.length();
        if (espacos < 1) {
            return esq + "\n" + dir;
        }
        return esq + " ".repeat(espacos) + dir;
    }
}
