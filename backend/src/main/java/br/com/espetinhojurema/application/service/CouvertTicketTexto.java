package br.com.espetinhojurema.application.service;

import br.com.espetinhojurema.application.model.PedidoDetalheView;
import br.com.espetinhojurema.domain.model.CouvertArtisticoModo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Function;

/**
 * Formatação do couvert artístico em tickets — taxa única da conta (não item de cardápio com quantidade).
 */
public final class CouvertTicketTexto {

    private CouvertTicketTexto() {}

    /** Dados do couvert quando aplicável; {@code null} se não houver cobrança. */
    public static CouvertLinha dados(PedidoDetalheView pedido) {
        BigDecimal valor = pedido.valorCouvertArtistico();
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        CouvertArtisticoModo modo = pedido.couvertModo() != null
                ? pedido.couvertModo()
                : CouvertArtisticoModo.POR_PESSOA;
        BigDecimal unit = pedido.valorCouvertPorPessoa() != null
                ? pedido.valorCouvertPorPessoa()
                : valor;
        int pessoas = pedido.couvertPessoasCobradas() != null ? pedido.couvertPessoasCobradas() : 0;
        return new CouvertLinha(modo, valor, unit, pessoas);
    }

    /**
     * Comanda de fechamento — mesmo estilo da taxa do garçom (sem prefixo {@code Nx} de item).
     */
    public static void appendComandaFechamento(
            StringBuilder sb, CouvertLinha linha, Function<BigDecimal, String> formatValorPt) {
        sb.append("COUVERT ARTISTICO").append('\n');
        if (linha.modo() == CouvertArtisticoModo.POR_MESA) {
            sb.append("   R$ ")
                    .append(formatValorPt.apply(linha.valorTotal()))
                    .append(" (por mesa)")
                    .append('\n');
        } else if (linha.pessoas() > 1) {
            sb.append("   ")
                    .append(linha.pessoas())
                    .append(" pessoa(s) x R$ ")
                    .append(formatValorPt.apply(linha.valorUnitario()))
                    .append(" = R$ ")
                    .append(formatValorPt.apply(linha.valorTotal()))
                    .append('\n');
        } else {
            sb.append("   R$ ").append(formatValorPt.apply(linha.valorTotal())).append('\n');
        }
    }

    /** Comprovante detalhado (itens listados um a um). */
    public static void appendComprovanteDetalhado(StringBuilder sb, CouvertLinha linha) {
        sb.append("COUVERT ARTISTICO (taxa da mesa)\n");
        if (linha.modo() == CouvertArtisticoModo.POR_MESA) {
            sb.append("   ")
                    .append(linha.valorTotal().setScale(2, RoundingMode.HALF_UP))
                    .append(" (por mesa)")
                    .append('\n');
        } else if (linha.pessoas() > 1) {
            sb.append("   ")
                    .append(linha.pessoas())
                    .append(" pessoa(s) x ")
                    .append(linha.valorUnitario().setScale(2, RoundingMode.HALF_UP))
                    .append(" = ")
                    .append(linha.valorTotal().setScale(2, RoundingMode.HALF_UP))
                    .append('\n');
        } else {
            sb.append("   ")
                    .append(linha.valorTotal().setScale(2, RoundingMode.HALF_UP))
                    .append('\n');
        }
    }

    /** Comprovante de fechamento compacto ({@code ... valor}). */
    public static void appendComprovanteFechamento(StringBuilder sb, CouvertLinha linha) {
        if (linha.modo() == CouvertArtisticoModo.POR_MESA) {
            sb.append("COUVERT ARTISTICO (mesa) ... ")
                    .append(linha.valorTotal().setScale(2, RoundingMode.HALF_UP))
                    .append('\n');
        } else if (linha.pessoas() > 1) {
            sb.append("COUVERT ARTISTICO (")
                    .append(linha.pessoas())
                    .append(" pess) ... ")
                    .append(linha.valorTotal().setScale(2, RoundingMode.HALF_UP))
                    .append('\n');
        } else {
            sb.append("COUVERT ARTISTICO ... ")
                    .append(linha.valorTotal().setScale(2, RoundingMode.HALF_UP))
                    .append('\n');
        }
    }

    public record CouvertLinha(
            CouvertArtisticoModo modo, BigDecimal valorTotal, BigDecimal valorUnitario, int pessoas) {}
}
