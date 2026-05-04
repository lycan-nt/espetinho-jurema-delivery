package br.com.delivere.acai.loja;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LojaRepository extends JpaRepository<Loja, String> {

    List<Loja> findAllByOrderByIdAsc();
}
