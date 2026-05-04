package br.com.espetinhojurema.application.port.out;

import br.com.espetinhojurema.application.model.ClienteView;
import java.util.List;

public interface ClientePersistencePort {

    List<ClienteView> listar();

    ClienteView criar(String nome, String telefone, String endereco);
}
