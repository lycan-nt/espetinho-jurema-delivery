package br.com.delivere.acai.comanda;

import java.math.BigDecimal;

/**
 * Dados resumidos para a tela inicial (dashboard) após o login.
 */
public class DashboardDTO {

    private long comandasAbertasHoje;
    private long comandasAbertasMaisDeUmDia;
    private long pedidosPendentesNfce;
    private boolean caixaAberto;
    private BigDecimal totalVendasHoje;

    public DashboardDTO() {
    }

    public DashboardDTO(long comandasAbertasHoje, long comandasAbertasMaisDeUmDia,
                        long pedidosPendentesNfce, boolean caixaAberto, BigDecimal totalVendasHoje) {
        this.comandasAbertasHoje = comandasAbertasHoje;
        this.comandasAbertasMaisDeUmDia = comandasAbertasMaisDeUmDia;
        this.pedidosPendentesNfce = pedidosPendentesNfce;
        this.caixaAberto = caixaAberto;
        this.totalVendasHoje = totalVendasHoje != null ? totalVendasHoje : BigDecimal.ZERO;
    }

    public long getComandasAbertasHoje() {
        return comandasAbertasHoje;
    }

    public void setComandasAbertasHoje(long comandasAbertasHoje) {
        this.comandasAbertasHoje = comandasAbertasHoje;
    }

    public long getComandasAbertasMaisDeUmDia() {
        return comandasAbertasMaisDeUmDia;
    }

    public void setComandasAbertasMaisDeUmDia(long comandasAbertasMaisDeUmDia) {
        this.comandasAbertasMaisDeUmDia = comandasAbertasMaisDeUmDia;
    }

    public long getPedidosPendentesNfce() {
        return pedidosPendentesNfce;
    }

    public void setPedidosPendentesNfce(long pedidosPendentesNfce) {
        this.pedidosPendentesNfce = pedidosPendentesNfce;
    }

    public boolean isCaixaAberto() {
        return caixaAberto;
    }

    public void setCaixaAberto(boolean caixaAberto) {
        this.caixaAberto = caixaAberto;
    }

    public BigDecimal getTotalVendasHoje() {
        return totalVendasHoje;
    }

    public void setTotalVendasHoje(BigDecimal totalVendasHoje) {
        this.totalVendasHoje = totalVendasHoje;
    }
}
