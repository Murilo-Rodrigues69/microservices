export type FiscalModel = "NFE" | "NFCE";
export type Ambiente = "HOMOLOGACAO" | "PRODUCAO";
export type UserRole = "ADMIN" | "USER";
export type FormaPagamento =
  | "DINHEIRO"
  | "PIX"
  | "CARTAO_CREDITO"
  | "CARTAO_DEBITO";

export interface ApiErrorPayload {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  path?: string;
}

export interface LoginPayload {
  username: string;
  password: string;
}

export interface AuthUser {
  id: number;
  login: string;
  name: string;
  role: UserRole;
  active: boolean;
  roles: string[];
  createdAt: string;
  updatedAt: string;
}

export interface LoginResponse {
  username: string;
  roles: string[];
  user: AuthUser;
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface BrasilApiCnpjResponse {
  cnpj: string;
  razaoSocial: string;
  nomeFantasia: string | null;
  situacaoCadastral: string;
  descricaoSituacaoCadastral: string;
  dataInicioAtividade: string;
  naturezaJuridica: string;
  porte: string;
  capitalSocial: number;
  email: string | null;
  telefone1: string | null;
  telefone2: string | null;
  cep: string;
  tipoLogradouro: string | null;
  logradouro: string;
  logradouroCompleto: string;
  numero: string;
  complemento: string | null;
  bairro: string;
  municipio: string;
  codigoMunicipioIbge: string;
  uf: string;
}

export interface ViaCepResponse {
  cep: string;
  logradouro: string;
  logradouroCompleto: string;
  complemento: string | null;
  bairro: string;
  localidade: string;
  uf: string;
  estado: string;
  regiao: string;
  ibge: string;
  gia: string | null;
  ddd: string;
  siafi: string;
}

export interface Empresa {
  id: number;
  nome: string;
  cnpj: string;
  razaoSocial: string;
  nomeFantasia: string | null;
  inscricaoEstadual: string | null;
  serie: string;
  naturezaOperacao: string;
  cep: string;
  logradouro: string;
  numero: string;
  bairro: string;
  codigoMunicipio: string;
  descricaoMunicipio: string;
  uf: string;
  modelo: FiscalModel;
  ambiente: Ambiente;
  certificadoPfxPath: string;
  cadeiaJksPath: string;
  cscId: number | null;
  ativo: boolean;
  certificadoSenhaConfigurada: boolean;
  cadeiaSenhaConfigurada: boolean;
  cscConfigurado: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface EmpresaPayload {
  nome: string;
  cnpj: string;
  razaoSocial: string;
  nomeFantasia: string | null;
  inscricaoEstadual: string | null;
  serie: string;
  naturezaOperacao: string;
  cep: string;
  logradouro: string;
  numero: string;
  bairro: string;
  codigoMunicipio: string;
  descricaoMunicipio: string;
  uf: string;
  modelo: FiscalModel;
  ambiente: Ambiente;
  certificadoPfxPath: string;
  certificadoSenha?: string;
  cadeiaJksPath: string;
  cadeiaSenha?: string;
  csc: string | null;
  cscId: number;
  ativo: boolean;
}

export interface ProdutoEmpresa {
  id: number;
  empresaId: number;
  descricao: string;
  codigoBarras: string | null;
  ncm: string;
  cest: string | null;
  origem: string;
  cfop: string;
  unidadeComercial: string;
  precoUnitarioSugerido: number;
  ativo: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ProdutoPayload {
  descricao: string;
  codigoBarras: string | null;
  ncm: string;
  cest: string | null;
  origem: string;
  cfop: string;
  unidadeComercial: string;
  precoUnitarioSugerido: number;
  ativo: boolean;
}

export interface ClienteEmpresa {
  id: number;
  empresaId: number;
  nome: string;
  cpfCnpj: string;
  inscricaoEstadual: string | null;
  cep: string;
  logradouro: string;
  numero: string;
  bairro: string;
  codigoMunicipio: string;
  descricaoMunicipio: string;
  uf: string;
  email: string | null;
  ativo: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ClientePayload {
  nome: string;
  cpfCnpj: string;
  inscricaoEstadual: string | null;
  cep: string;
  logradouro: string;
  numero: string;
  bairro: string;
  codigoMunicipio: string;
  descricaoMunicipio: string;
  uf: string;
  email: string | null;
  ativo: boolean;
}

export interface DestinatarioPayload {
  nome: string;
  cpfCnpj: string;
  inscricaoEstadual: string | null;
  cep: string;
  logradouro: string;
  numero: string;
  bairro: string;
  codigoMunicipio: string;
  descricaoMunicipio: string;
  uf: string;
  email: string | null;
}

export interface EmissaoItemPayload {
  itemId: number;
  produtoEmpresaId: number;
  quantidade: number;
  precoUnitario: number;
}

export interface EmissaoPayload {
  empresaId: number;
  vendaId: number;
  clienteEmpresaId?: number;
  cpfCnpjConsumidor?: string;
  destinatario?: DestinatarioPayload;
  numeroNota: string;
  dataHora: string;
  desconto: number;
  formaPagamento: FormaPagamento;
  observacao?: string;
  itens: EmissaoItemPayload[];
}

export interface ConsultaPayload {
  empresaId: number;
  chaveAcesso: string;
}

export interface CancelamentoPayload extends ConsultaPayload {
  protocoloAutorizacao: string;
  justificativa: string;
}

export interface FiscalStatus {
  empresaId: number;
  empresaNome: string;
  modelo: FiscalModel;
  ambiente: Ambiente;
  uf: string;
  cStat: number;
  motivo: string;
  versaoAplicacao: string;
}

export interface FiscalResponse {
  empresaId: number;
  empresaNome: string;
  modelo: FiscalModel;
  cStat: number | null;
  motivo: string;
  chaveAcesso: string | null;
  protocoloAutorizacao: string | null;
  qrCodeUrl: string | null;
  xml: string | null;
  xmlDownloadUrl: string | null;
  pdfDownloadUrl: string | null;
  xmlFilePath: string | null;
  pdfFilePath: string | null;
}

export interface NotaFiscal {
  id: number;
  empresaId: number;
  clienteEmpresaId: number | null;
  usuarioEmissorId: number | null;
  usuarioEmissorLogin: string | null;
  usuarioEmissorNome: string | null;
  modelo: FiscalModel;
  situacao: string;
  numeroNota: string | null;
  serie: string | null;
  chaveAcesso: string | null;
  protocoloAutorizacao: string | null;
  cStat: number | null;
  motivo: string | null;
  qrCodeUrl: string | null;
  destinatarioNome: string | null;
  destinatarioDocumento: string | null;
  valorTotal: number | null;
  dataEmissao: string | null;
  xmlDownloadUrl: string | null;
  pdfDownloadUrl: string | null;
  createdAt: string;
  updatedAt: string;
}
