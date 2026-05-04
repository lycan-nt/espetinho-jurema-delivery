package br.com.espetinhojurema.application.port.out;

import br.com.espetinhojurema.application.model.CaixaStatusView;
import java.math.BigDecimal;

public interface CaixaPersistencePort {

    CaixaStatusView statusAtual();

    CaixaStatusView abrirSessao(BigDecimal saldoAbertura);

    CaixaStatusView fecharSessao(BigDecimal saldoFechamento);
}
