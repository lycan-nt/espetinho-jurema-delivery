package br.com.espetinhojurema.application.service;

import br.com.espetinhojurema.application.model.PedidoDetalheView;
import br.com.espetinhojurema.domain.model.FormaPagamento;
import br.com.espetinhojurema.domain.model.PedidoStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class ComprovanteTextoService {

    private final CabecalhoEmpresaTicketService cabecalhoEmpresaTicketService;

    public ComprovanteTextoService(CabecalhoEmpresaTicketService cabecalhoEmpresaTicketService) {
        this.cabecalhoEmpresaTicketService = cabecalhoEmpresaTicketService;
    }

    public String gerar(PedidoDetalheView pedido, boolean destacarFiscal) {
        StringBuilder sb = new StringBuilder();
        cabecalhoEmpresaTicketService.appendCabecalhoEmpresaEIdentificacao(sb, pedido);
        sb.append("Pedido #").append(pedido.id()).append('\n');
        sb.append("Tipo: ").append(pedido.tipo()).append('\n');
        sb.append("Status: ").append(pedido.status()).append('\n');
        if (pedido.mesaNumero() != null) {
            sb.append("Mesa: ").append(pedido.mesaNumero()).append('\n');
        }
        if (pedido.clienteNome() != null) {
            sb.append("Cliente: ").append(pedido.clienteNome()).append('\n');
        }
        if (pedido.documentoFiscal() || destacarFiscal) {
            sb.append("*** Documento fiscal ***\n");
        } else {
            sb.append("*** Sem NF-e neste comprovante ***\n");
        }
        sb.append(TicketTextoLayout.linhaMenos());
        for (var item : pedido.itens()) {
            sb.append(item.quantidade()).append("x ").append(item.produtoNome());
            if (item.cancelado()) {
                sb.append(" [CANCELADO]");
            }
            sb.append('\n');
            sb.append("   ")
                    .append(item.precoUnitario().setScale(2, RoundingMode.HALF_UP))
                    .append(" un = ")
                    .append(item.precoUnitario()
                            .multiply(java.math.BigDecimal.valueOf(item.quantidade()))
                            .setScale(2, RoundingMode.HALF_UP));
            if (item.cancelado()) {
                sb.append(" (não entra no total)");
            }
            sb.append('\n');
            if (item.pontoCarne() != null) {
                sb.append("   Ponto: ").append(item.pontoCarne().rotulo()).append('\n');
            }
            if (item.observacao() != null && !item.observacao().isBlank()) {
                sb.append("   Obs: ").append(item.observacao()).append('\n');
            }
        }
        sb.append(TicketTextoLayout.linhaMenos());
        sb.append("TOTAL: ")
                .append(pedido.total().setScale(2, RoundingMode.HALF_UP))
                .append('\n');
        if (!pedido.pagamentos().isEmpty()) {
            sb.append("--- Pagamentos ---\n");
            for (var pg : pedido.pagamentos()) {
                sb.append(pg.forma().name())
                        .append(": ")
                        .append(pg.valor().setScale(2, RoundingMode.HALF_UP));
                if (pg.troco() != null && pg.troco().compareTo(BigDecimal.ZERO) > 0) {
                    sb.append(" (troco ").append(pg.troco().setScale(2, RoundingMode.HALF_UP)).append(")");
                }
                sb.append('\n');
            }
            sb.append("Pago: ")
                    .append(pedido.totalPago().setScale(2, RoundingMode.HALF_UP))
                    .append(" | Restante: ")
                    .append(pedido.restante().setScale(2, RoundingMode.HALF_UP))
                    .append('\n');
        } else if (pedido.status() != PedidoStatus.PAGO
                && pedido.status() != PedidoStatus.CANCELADO
                && pedido.total().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Ja pago: ")
                    .append(pedido.totalPago().setScale(2, RoundingMode.HALF_UP))
                    .append(" | Falta pagar: ")
                    .append(pedido.restante().setScale(2, RoundingMode.HALF_UP))
                    .append('\n');
        }
        sb.append(TicketTextoLayout.linhaIguais());
        sb.append("Obrigado!\n");
        return sb.toString();
    }

    /** Comprovante de fechamento: destaca totais e formas de pagamento. */
    public String gerarFechamento(PedidoDetalheView pedido, boolean destacarFiscal) {
        StringBuilder sb = new StringBuilder();
        cabecalhoEmpresaTicketService.appendCabecalhoEmpresaEIdentificacao(sb, pedido);
        sb.append(TicketTextoLayout.linhaTituloEntreIguais("COMANDA DE FECHAMENTO"));
        sb.append("Pedido #").append(pedido.id()).append('\n');
        sb.append("Tipo: ").append(pedido.tipo()).append('\n');
        sb.append("Status: ").append(pedido.status()).append('\n');
        if (pedido.mesaNumero() != null) {
            sb.append("Mesa: ").append(pedido.mesaNumero()).append('\n');
        }
        if (pedido.clienteNome() != null) {
            sb.append("Cliente: ").append(pedido.clienteNome()).append('\n');
        }
        if (pedido.documentoFiscal() || destacarFiscal) {
            sb.append("*** Documento fiscal ***\n");
        } else {
            sb.append("*** Sem NF-e neste comprovante ***\n");
        }
        sb.append(TicketTextoLayout.linhaMenos());
        sb.append("ITENS\n");
        sb.append(TicketTextoLayout.linhaMenos());
        for (var item : pedido.itens()) {
            BigDecimal sub = item.precoUnitario()
                    .multiply(java.math.BigDecimal.valueOf(item.quantidade()))
                    .setScale(2, RoundingMode.HALF_UP);
            sb.append(item.quantidade())
                    .append("x ")
                    .append(item.produtoNome())
                    .append(" ... ")
                    .append(sub);
            if (item.cancelado()) {
                sb.append(" [CANCELADO]");
            }
            sb.append('\n');
            if (item.pontoCarne() != null) {
                sb.append("   Ponto: ").append(item.pontoCarne().rotulo()).append('\n');
            }
            if (item.observacao() != null && !item.observacao().isBlank()) {
                sb.append("   Obs: ").append(item.observacao()).append('\n');
            }
        }
        sb.append(TicketTextoLayout.linhaMenos());
        sb.append("TOTAL DA CONTA: ")
                .append(pedido.total().setScale(2, RoundingMode.HALF_UP))
                .append('\n');
        sb.append(TicketTextoLayout.linhaMenos());
        sb.append("FORMAS DE PAGAMENTO\n");
        sb.append(TicketTextoLayout.linhaMenos());
        if (pedido.pagamentos().isEmpty()) {
            sb.append("(sem pagamentos registrados)\n");
        } else {
            for (var pg : pedido.pagamentos()) {
                sb.append(rotuloForma(pg.forma()))
                        .append(": ")
                        .append(pg.valor().setScale(2, RoundingMode.HALF_UP));
                if (pg.troco() != null && pg.troco().compareTo(BigDecimal.ZERO) > 0) {
                    sb.append("  |  Troco: ").append(pg.troco().setScale(2, RoundingMode.HALF_UP));
                }
                sb.append('\n');
            }
        }
        sb.append(TicketTextoLayout.linhaMenos());
        sb.append("TOTAL PAGO: ")
                .append(pedido.totalPago().setScale(2, RoundingMode.HALF_UP))
                .append('\n');
        sb.append("RESTANTE: ")
                .append(pedido.restante().setScale(2, RoundingMode.HALF_UP))
                .append('\n');
        sb.append(TicketTextoLayout.linhaIguais());
        sb.append("Obrigado pela preferência!\n");
        return sb.toString();
    }

    private static String rotuloForma(FormaPagamento f) {
        return switch (f) {
            case DINHEIRO -> "Dinheiro";
            case PIX -> "Pix";
            case DEBITO -> "Débito";
            case CREDITO -> "Crédito";
            case OUTRO -> "Outro";
        };
    }
}
