package br.com.espetinhojurema.application.port.out;

import br.com.espetinhojurema.application.model.ColaboradorView;
import java.util.List;

public interface ColaboradorPersistencePort {

    List<ColaboradorView> listarAtivos();
}
