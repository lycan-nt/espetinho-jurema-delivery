package br.com.espetinhojurema.domain.model;

/**
 * Tipos de evento gravados como alerta pendente para o perfil ATENDIMENTO (balcão/PC).
 */
public enum TipoAlertaAtendimento {
    /** Comanda de cozinha após lançamento no celular — OK imprime texto da comanda. */
    COMANDA_ENVIADA,
    /** Solicitação do churrasqueiro para fechar a mesa — alerta no balcão; OK gera/imprime mesma comanda de cozinha que o fluxo de comanda enviada. */
    SOLICITACAO_FECHAMENTO_COMANDA
}
