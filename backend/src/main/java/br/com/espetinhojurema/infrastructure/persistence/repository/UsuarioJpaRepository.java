package br.com.espetinhojurema.infrastructure.persistence.repository;

import br.com.espetinhojurema.domain.model.PerfilUsuario;
import br.com.espetinhojurema.infrastructure.persistence.entity.UsuarioEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, Long> {

    Optional<UsuarioEntity> findByLoginIgnoreCaseAndAtivoTrue(String login);

    Optional<UsuarioEntity> findByLoginIgnoreCase(String login);

    List<UsuarioEntity> findAllByOrderByLoginAsc();

    boolean existsByLoginIgnoreCase(String login);

    long countByPerfilAndAtivoTrueAndIdNot(PerfilUsuario perfil, Long id);
}
