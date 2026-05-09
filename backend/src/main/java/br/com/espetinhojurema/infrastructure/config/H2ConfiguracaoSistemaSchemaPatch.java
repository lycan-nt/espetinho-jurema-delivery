package br.com.espetinhojurema.infrastructure.config;

import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Garante colunas booleanas da comanda no H2 quando o banco já tinha linha em {@code configuracao_sistema}
 * antes desses campos: {@code ddl-auto: update} pode falhar ao adicionar {@code NOT NULL} sem default.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class H2ConfiguracaoSistemaSchemaPatch implements ApplicationRunner {

    private static final String[] COLUNAS_COMANDA_CABECALHO = {
        "comanda_cab_exibir_cnpj",
        "comanda_cab_exibir_nome",
        "comanda_cab_exibir_endereco",
        "comanda_cab_exibir_telefone",
        "comanda_cab_exibir_email",
        "comanda_cab_exibir_instagram"
    };

    private final DataSource dataSource;
    private final String jdbcUrl;

    public H2ConfiguracaoSistemaSchemaPatch(
            DataSource dataSource, @Value("${spring.datasource.url:}") String jdbcUrl) {
        this.dataSource = dataSource;
        this.jdbcUrl = jdbcUrl;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!jdbcUrl.contains("jdbc:h2:")) {
            return;
        }
        try (Connection c = dataSource.getConnection();
                Statement st = c.createStatement()) {
            for (String col : COLUNAS_COMANDA_CABECALHO) {
                st.execute(
                        "ALTER TABLE configuracao_sistema ADD COLUMN IF NOT EXISTS "
                                + col
                                + " BOOLEAN DEFAULT TRUE NOT NULL");
            }
        }
    }
}
