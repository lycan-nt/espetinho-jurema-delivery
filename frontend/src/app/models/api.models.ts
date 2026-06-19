export type PerfilUsuario = 'ATENDIMENTO' | 'GARCOM' | 'CHURRASQUEIRO';

export type MesaStatus = 'LIVRE' | 'OCUPADA' | 'ENCERRANDO_SERVICO';
export type PedidoTipo = 'MESA' | 'BALCAO' | 'DELIVERY';
export type PedidoStatus =
  | 'RASCUNHO'
  | 'ABERTO'
  | 'EM_PREPARO'
  | 'PRONTO'
  | 'PAGO'
  | 'CANCELADO';

export type FormaPagamento = 'DINHEIRO' | 'PIX' | 'DEBITO' | 'CREDITO' | 'OUTRO';

export interface PagamentoPedido {
  id: number;
  forma: FormaPagamento;
  valor: number;
  valorRecebidoDinheiro: number | null;
  troco: number | null;
}

export interface MesaComOcupacao {
  id: number;
  numero: number;
  status: MesaStatus;
  pedidoAbertoId: number | null;
  ocupada: boolean;
}

export interface MesaResumo {
  total: number;
  ocupadas: number;
  livres: number;
  encerrandoServico: number;
}

/** Histórico de troca de mesa (mesmo pedido). */
export interface MesaTransferencia {
  id: number;
  pedidoId: number;
  mesaOrigemNumero: number;
  mesaDestinoNumero: number;
  criadoEm: string;
  usuarioLogin: string;
}

export interface Colaborador {
  id: number;
  nome: string;
}

export interface UsuarioAdmin {
  id: number;
  login: string;
  nomeExibicao: string;
  perfil: PerfilUsuario;
  ativo: boolean;
}

export interface CategoriaCardapio {
  id: number;
  nome: string;
  ordem: number;
}

export interface Cliente {
  id: number;
  nome: string;
  telefone: string | null;
  endereco: string | null;
}

export type PontoCarne = 'MAL_PASSADA' | 'AO_PONTO' | 'BEM_PASSADA';

export interface Produto {
  id: number;
  nome: string;
  descricao: string | null;
  preco: number;
  categoriaId: number | null;
  categoriaNome: string | null;
  codigoImpressao: string | null;
  ativo: boolean;
  saldoEstoque?: number;
}

export interface EstoqueConfig {
  estoqueObrigatorio: boolean;
}

export type CouvertArtisticoModo = 'POR_MESA' | 'POR_PESSOA';

export interface CouvertArtisticoConfig {
  ativo: boolean;
  modo: CouvertArtisticoModo;
  valorPorPessoa: number;
}

export interface TaxaGarcomConfig {
  habilitada: boolean;
  percentual: number;
}

export interface FormaPagamentoTotal {
  forma: FormaPagamento;
  total: number;
}

export interface ProdutoVendaTotal {
  produtoId: number;
  produtoNome: string;
  total: number;
}

export interface FaturamentoResumo {
  receitaTotal: number;
  porForma: FormaPagamentoTotal[];
  pedidosEncerradosNoPeriodo: number;
  /** Presente a partir da API com relatório por produto. */
  porProduto?: ProdutoVendaTotal[];
}

export interface ItemPedido {
  id: number;
  produtoId: number;
  produtoNome: string;
  quantidade: number;
  precoUnitario: number;
  observacao: string | null;
  /** Ponto de cocção; obrigatório na API ao lançar item da categoria Espetinhos. */
  pontoCarne?: PontoCarne | null;
  /** Item cancelado (lançamento indevido); não entra no total nem na comanda. */
  cancelado?: boolean;
  canceladoEm?: string | null;
  canceladoPorLogin?: string | null;
}

export interface PedidoDetalhe {
  id: number;
  tipo: PedidoTipo;
  status: PedidoStatus;
  mesaId: number | null;
  mesaNumero: number | null;
  clienteId: number | null;
  clienteNome: string | null;
  colaboradorId: number | null;
  colaboradorNome: string | null;
  descricaoMesa: string | null;
  pessoas: number | null;
  documentoFiscal: boolean;
  criadoEm: string;
  itens: ItemPedido[];
  /** Soma dos itens (sem couvert). */
  subtotalItens?: number;
  /** Valor do couvert artístico neste pedido (0 se não aplicável). */
  valorCouvertArtistico?: number;
  valorCouvertPorPessoa?: number | null;
  couvertModo?: CouvertArtisticoModo | null;
  couvertPessoasCobradas?: number | null;
  valorTaxaGarcom?: number;
  taxaGarcomPercentualAplicado?: number | null;
  total: number;
  /** Presente nas respostas atuais da API; fallback em tela se ausente. */
  pagamentos?: PagamentoPedido[];
  totalPago?: number;
  restante?: number;
}

export interface PedidoLista {
  id: number;
  tipo: PedidoTipo;
  status: PedidoStatus;
  mesaId: number | null;
  mesaNumero: number | null;
  colaboradorNome: string | null;
  criadoEm: string;
  subtotalItens: number;
  valorCouvertArtistico: number;
  valorTaxaGarcom: number;
  /** Total da conta (itens + couvert + taxa garçom). */
  total: number;
  /** @deprecated use total — mantido para compatibilidade com respostas antigas */
  totalItens?: number;
}

export interface CaixaStatus {
  aberto: boolean;
  abertoEm: string | null;
  fechadoEm: string | null;
  saldoAbertura: number | null;
  saldoFechamento: number | null;
  sessaoId: number | null;
}

export interface PedidoWsPayload {
  tipo: string;
  pedidoId: number;
  quando: string;
}

/** STOMP `/topic/atendimento/alertas` (ex.: mesa aberta no mobile). */
export interface AlertaAtendimentoWsPayload {
  tipo: string;
  pedidoId: number;
  mesaNumero: number;
  alertaId: string;
  quando: string;
}

export interface ReconhecerAlertaResponse {
  textoComanda: string;
  jaReconhecido: boolean;
  /** Quando `true`, o texto já foi enviado ao CUPS no servidor (térmica). */
  impressoServidor?: boolean;
}

export interface ImpressaoConfig {
  nomeImpressoraLp: string | null;
}

/** Campos do cadastro que podem aparecer na comanda de cozinha (cupom ignora e mostra todos os preenchidos). */
export interface ComandaCabecalhoCampos {
  cnpj: boolean;
  nomeEmpresa: boolean;
  endereco: boolean;
  telefone: boolean;
  email: boolean;
  instagram: boolean;
}

/** Cadastro da empresa (painel atendimento). */
export interface EmpresaDados {
  cnpj: string | null;
  nomeEmpresa: string | null;
  endereco: string | null;
  telefone: string | null;
  email: string | null;
  instagram: string | null;
  comandaCabecalho: ComandaCabecalhoCampos;
}

export interface ImpressaoFilasResponse {
  filas: string[];
}

export interface ImprimirLocalResponse {
  impressoServidor: boolean;
}

export type DiaBackupApi = 'MON' | 'TUE' | 'WED' | 'THU' | 'FRI' | 'SAT' | 'SUN';

/** Configuração de backup H2 (perfil atendimento). Timestamps ISO da API (Instant). */
export interface BackupConfig {
  diretorioGravadoNoBanco: string | null;
  diretorioEfetivo: string;
  ultimoBackupSucessoEm: string | null;
  ultimoErroEm: string | null;
  ultimoErroMensagem: string | null;
  agendamentoAtivo: boolean;
  backupHora1: number;
  backupMinuto1: number;
  backupHora2: number;
  backupMinuto2: number;
  backupDiasSemana: DiaBackupApi[];
  agendamentoResumo: string;
  statusRotina: string;
  retencaoDias: number;
  fusoHorarioAgendamento: string;
}

export interface BackupFolderPickResponse {
  path: string | null;
  cancelado: boolean;
}
