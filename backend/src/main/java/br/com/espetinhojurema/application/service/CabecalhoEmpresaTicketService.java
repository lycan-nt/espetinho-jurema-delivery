package br.com.espetinhojurema.application.service;

import br.com.espetinhojurema.application.model.ComandaCabecalhoCampos;
import br.com.espetinhojurema.application.model.EmpresaDadosView;
import br.com.espetinhojurema.application.model.PedidoDetalheView;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Cabeçalho de bobina com dados da empresa (referência visual {@code contexto/contexto-cabecalho-comanda.jpg}).
 * Na comanda de cozinha, campos opcionais conforme seleção em Configurações; cupom usa todos os dados preenchidos.
 */
@Service
public class CabecalhoEmpresaTicketService {

    private static final int W = TicketTextoLayout.COLUNAS;
    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FMT_DATA_HORA_SEG =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZONA);
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final String FALLBACK_NOME = "ESPETINHO JUREMA";

    private final EmpresaDadosOperacaoService empresaDadosOperacaoService;

    public CabecalhoEmpresaTicketService(EmpresaDadosOperacaoService empresaDadosOperacaoService) {
        this.empresaDadosOperacaoService = empresaDadosOperacaoService;
    }

    /** Cupom/comprovante: exibe todos os campos cadastrados que tenham valor. */
    public void appendCabecalhoEmpresaEIdentificacao(StringBuilder sb, PedidoDetalheView pedido) {
        appendCabecalhoEmpresaEIdentificacao(sb, pedido, false);
    }

    /**
     * @param respeitarSelecaoCabecalhoComanda {@code true} só para comanda de cozinha (filtro da tela Dados da empresa).
     */
    public void appendCabecalhoEmpresaEIdentificacao(
            StringBuilder sb, PedidoDetalheView pedido, boolean respeitarSelecaoCabecalhoComanda) {
        EmpresaDadosView d = empresaDadosOperacaoService.obter();
        ComandaCabecalhoCampos filtro = respeitarSelecaoCabecalhoComanda ? d.comandaCabecalho() : null;
        appendBlocoDadosEmpresa(sb, d, filtro);
        sb.append(TicketTextoLayout.linhaMenos());
        sb.append(TicketTextoLayout.linhaDupla(
                FMT_DATA_HORA_SEG.format(pedido.criadoEm()), "Nº: " + pedido.id()));
        sb.append(TicketTextoLayout.linhaMenos());
    }

    private void appendBlocoDadosEmpresa(StringBuilder sb, EmpresaDadosView d, ComandaCabecalhoCampos filtro) {
        boolean livre = filtro == null;
        boolean exCnpj = livre || filtro.cnpj();
        boolean exNome = livre || filtro.nomeEmpresa();
        boolean exEnd = livre || filtro.endereco();
        boolean exTel = livre || filtro.telefone();
        boolean exEmail = livre || filtro.email();
        boolean exInst = livre || filtro.instagram();

        boolean algumaLinha = false;

        if (textoPresente(d.cnpj()) && exCnpj) {
            appendLinha(sb, "CNPJ: " + d.cnpj().strip());
            algumaLinha = true;
        }

        boolean temNomeCad = textoPresente(d.nomeEmpresa());
        if (temNomeCad && exNome) {
            appendQuebraPorPalavras(sb, d.nomeEmpresa().strip().toUpperCase(PT_BR));
            algumaLinha = true;
        } else if (!temNomeCad && exNome) {
            appendQuebraPorPalavras(sb, FALLBACK_NOME);
            algumaLinha = true;
        }

        if (textoPresente(d.endereco()) && exEnd) {
            for (String parte : d.endereco().split("[\r\n]+")) {
                if (!parte.isBlank()) {
                    appendQuebraPorPalavras(sb, parte.strip().toUpperCase(PT_BR));
                    algumaLinha = true;
                }
            }
        }

        String tel = textoPresente(d.telefone()) && exTel ? d.telefone().strip() : null;
        String email = textoPresente(d.email()) && exEmail ? d.email().strip() : null;
        if (tel != null || email != null) {
            appendTelEmail(sb, tel, email);
            algumaLinha = true;
        }

        if (textoPresente(d.instagram()) && exInst) {
            appendLinha(sb, "INSTAGRAM: " + d.instagram().strip());
            algumaLinha = true;
        }

        if (!algumaLinha) {
            appendLinha(sb, FALLBACK_NOME);
        }
    }

    private static boolean textoPresente(String s) {
        return s != null && !s.isBlank();
    }

    private static void appendTelEmail(StringBuilder sb, String tel, String email) {
        boolean temTel = tel != null && !tel.isBlank();
        boolean temEmail = email != null && !email.isBlank();
        if (!temTel && !temEmail) {
            return;
        }
        if (temTel && temEmail) {
            sb.append(TicketTextoLayout.linhaDupla("TEL: " + tel, "EMAIL: " + email));
            return;
        }
        if (temTel) {
            appendLinha(sb, "TEL: " + tel);
        } else {
            appendLinha(sb, "EMAIL: " + email);
        }
    }

    /** Uma linha; quebra tokens longos se precisar. */
    private static void appendLinha(StringBuilder sb, String texto) {
        String t = texto.strip();
        if (t.length() <= W) {
            sb.append(t).append('\n');
            return;
        }
        int i = 0;
        while (i < t.length()) {
            int fim = Math.min(i + W, t.length());
            sb.append(t, i, fim).append('\n');
            i = fim;
        }
    }

    private static void appendQuebraPorPalavras(StringBuilder sb, String texto) {
        String t = texto.strip();
        if (t.isEmpty()) {
            return;
        }
        String[] palavras = t.split("\\s+");
        StringBuilder linha = new StringBuilder();
        for (String pal : palavras) {
            if (pal.isEmpty()) {
                continue;
            }
            if (pal.length() > W) {
                if (!linha.isEmpty()) {
                    sb.append(linha).append('\n');
                    linha.setLength(0);
                }
                for (int i = 0; i < pal.length(); i += W) {
                    int fim = Math.min(i + W, pal.length());
                    sb.append(pal, i, fim).append('\n');
                }
                continue;
            }
            if (linha.isEmpty()) {
                linha.append(pal);
            } else if (linha.length() + 1 + pal.length() <= W) {
                linha.append(' ').append(pal);
            } else {
                sb.append(linha).append('\n');
                linha = new StringBuilder(pal);
            }
        }
        if (!linha.isEmpty()) {
            sb.append(linha).append('\n');
        }
    }
}
