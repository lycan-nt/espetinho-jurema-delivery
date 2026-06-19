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

    private static final String[] COLUNAS_TAXA_GARCOM = {
        "taxa_garcom_habilitada BOOLEAN DEFAULT FALSE NOT NULL",
        "taxa_garcom_percentual DECIMAL(5,2) DEFAULT 10"
    };

    private static final String[] COLUNAS_COUVERT = {
        "couvert_artistico_ativo BOOLEAN DEFAULT FALSE NOT NULL",
        "couvert_artistico_valor DECIMAL(10,2) DEFAULT 0",
        "couvert_artistico_modo VARCHAR(20) DEFAULT 'POR_PESSOA' NOT NULL"
    };

    private static final String[] COLUNAS_BACKUP = {
        "backup_diretorio VARCHAR(2000)",
        "backup_ultimo_sucesso TIMESTAMP",
        "backup_ultimo_erro_em TIMESTAMP",
        "backup_ultimo_erro_msg VARCHAR(2000)",
        "backup_agend_h1 INT DEFAULT 19",
        "backup_agend_m1 INT DEFAULT 0",
        "backup_agend_h2 INT DEFAULT 21",
        "backup_agend_m2 INT DEFAULT 0",
        "backup_agend_dias VARCHAR(64) DEFAULT 'MON,TUE,WED,THU,FRI,SAT,SUN'"
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
            for (String def : COLUNAS_TAXA_GARCOM) {
                st.execute("ALTER TABLE configuracao_sistema ADD COLUMN IF NOT EXISTS " + def);
            }
            for (String def : COLUNAS_COUVERT) {
                st.execute("ALTER TABLE configuracao_sistema ADD COLUMN IF NOT EXISTS " + def);
            }
            for (String def : COLUNAS_BACKUP) {
                st.execute("ALTER TABLE configuracao_sistema ADD COLUMN IF NOT EXISTS " + def);
            }
        }
    }
}
