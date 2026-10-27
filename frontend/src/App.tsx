import {
  type Dispatch,
  type FormEvent,
  type ReactNode,
  type SetStateAction,
  useEffect,
  useState,
} from "react";
import {
  API_BASE_LABEL,
  ApiRequestError,
  absoluteApiUrl,
  ambienteLabel,
  api,
  fiscalModelLabel,
} from "./api";
import type {
  Ambiente,
  AuthUser,
  ClienteEmpresa,
  ClientePayload,
  DestinatarioPayload,
  EmissaoPayload,
  Empresa,
  EmpresaPayload,
  FiscalModel,
  FiscalResponse,
  FiscalStatus,
  FormaPagamento,
  NotaFiscal,
  PageResponse,
  ProdutoEmpresa,
  ProdutoPayload,
} from "./types";

type ViewMode =
  | "empresas"
  | "produtos"
  | "clientes"
  | "emissao"
  | "consulta"
  | "notas";
type CadastroMode = Extract<ViewMode, "empresas" | "produtos" | "clientes">;
type FiscalMode = Extract<ViewMode, "emissao" | "consulta" | "notas">;
type NoticeKind = "success" | "error" | "info";
type AuthStatus = "checking" | "authenticated" | "anonymous";

interface Notice {
  kind: NoticeKind;
  text: string;
}

interface LoginForm {
  username: string;
  password: string;
}

interface CompanyForm {
  nome: string;
  cnpj: string;
  razaoSocial: string;
  nomeFantasia: string;
  inscricaoEstadual: string;
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
  certificadoSenha: string;
  cadeiaJksPath: string;
  cadeiaSenha: string;
  csc: string;
  cscId: string;
  ativo: boolean;
}

interface ProductForm {
  descricao: string;
  codigoBarras: string;
  ncm: string;
  cest: string;
  origem: string;
  cfop: string;
  unidadeComercial: string;
  precoUnitarioSugerido: string;
  ativo: boolean;
}

interface ClientForm {
  nome: string;
  cpfCnpj: string;
  inscricaoEstadual: string;
  cep: string;
  logradouro: string;
  numero: string;
  bairro: string;
  codigoMunicipio: string;
  descricaoMunicipio: string;
  uf: string;
  email: string;
  ativo: boolean;
}

interface DestinatarioForm {
  nome: string;
  cpfCnpj: string;
  inscricaoEstadual: string;
  cep: string;
  logradouro: string;
  numero: string;
  bairro: string;
  codigoMunicipio: string;
  descricaoMunicipio: string;
  uf: string;
  email: string;
}

interface EmissionForm {
  vendaId: string;
  numeroNota: string;
  dataHora: string;
  desconto: string;
  formaPagamento: FormaPagamento;
  observacao: string;
  clienteEmpresaId: string;
  cpfCnpjConsumidor: string;
  useDestinatario: boolean;
  destinatario: DestinatarioForm;
}

interface EmissionItemForm {
  itemId: string;
  produtoEmpresaId: string;
  quantidade: string;
  precoUnitario: string;
}

const paymentOptions: Array<{ value: FormaPagamento; label: string }> = [
  { value: "DINHEIRO", label: "Dinheiro" },
  { value: "PIX", label: "PIX" },
  { value: "CARTAO_CREDITO", label: "Cartao de credito" },
  { value: "CARTAO_DEBITO", label: "Cartao de debito" },
];

const DEFAULT_PAGE_SIZE = 20;

const moduleCatalog: Array<{
  key: ViewMode;
  group: "Cadastros" | "Fiscal";
  label: string;
  kicker: string;
  description: string;
}> = [
  {
    key: "empresas",
    group: "Cadastros",
    label: "Empresas",
    kicker: "Emitentes",
    description: "Cadastre emitentes, certificado, ambiente e modelo fiscal.",
  },
  {
    key: "produtos",
    group: "Cadastros",
    label: "Produtos",
    kicker: "Catalogo",
    description: "Gerencie produtos fiscais vinculados a empresa selecionada.",
  },
  {
    key: "clientes",
    group: "Cadastros",
    label: "Clientes",
    kicker: "Destinatarios",
    description: "Mantenha destinatarios reutilizaveis para emissao de notas.",
  },
  {
    key: "emissao",
    group: "Fiscal",
    label: "Emissao NF-e/NFC-e",
    kicker: "Operacao fiscal",
    description: "Monte a venda, valide XML e emita usando o modelo da empresa.",
  },
  {
    key: "consulta",
    group: "Fiscal",
    label: "Consulta e cancelamento",
    kicker: "Pos-emissao",
    description: "Consulte notas por chave e envie cancelamento quando preciso.",
  },
  {
    key: "notas",
    group: "Fiscal",
    label: "Notas emitidas",
    kicker: "Historico",
    description: "Veja as notas ja emitidas com paginacao e links de documentos.",
  },
];

function App() {
  const [authStatus, setAuthStatus] = useState<AuthStatus>("checking");
  const [authUser, setAuthUser] = useState<AuthUser | null>(null);
  const [loginForm, setLoginForm] = useState<LoginForm>({
    username: "",
    password: "",
  });
  const [authBusy, setAuthBusy] = useState(false);
  const [authError, setAuthError] = useState<string | null>(null);
  const [viewMode, setViewMode] = useState<ViewMode>("empresas");
  const [notice, setNotice] = useState<Notice | null>(null);
  const [isLoadingCompanies, setIsLoadingCompanies] = useState(false);
  const [isLoadingRelations, setIsLoadingRelations] = useState(false);
  const [formBusy, setFormBusy] = useState<string | null>(null);
  const [integrationBusy, setIntegrationBusy] = useState<string | null>(null);
  const [fiscalBusy, setFiscalBusy] = useState<string | null>(null);

  const [empresas, setEmpresas] = useState<Empresa[]>([]);
  const [selectedCompanyId, setSelectedCompanyId] = useState<number | null>(null);
  const selectedCompany =
    empresas.find((empresa) => empresa.id === selectedCompanyId) ?? null;

  const [produtos, setProdutos] = useState<ProdutoEmpresa[]>([]);
  const [clientes, setClientes] = useState<ClienteEmpresa[]>([]);
  const [produtosPage, setProdutosPage] = useState<PageResponse<ProdutoEmpresa>>(
    () => createEmptyPage<ProdutoEmpresa>(),
  );
  const [clientesPage, setClientesPage] = useState<PageResponse<ClienteEmpresa>>(
    () => createEmptyPage<ClienteEmpresa>(),
  );
  const [notasPageData, setNotasPageData] = useState<PageResponse<NotaFiscal>>(
    () => createEmptyPage<NotaFiscal>(),
  );
  const [productNameFilter, setProductNameFilter] = useState("");
  const [clientNameFilter, setClientNameFilter] = useState("");
  const [productPage, setProductPage] = useState(0);
  const [clientPage, setClientPage] = useState(0);
  const [notasPage, setNotasPage] = useState(0);
  const [isLoadingProductPage, setIsLoadingProductPage] = useState(false);
  const [isLoadingClientPage, setIsLoadingClientPage] = useState(false);
  const [isLoadingNotas, setIsLoadingNotas] = useState(false);

  const [companyEditingId, setCompanyEditingId] = useState<number | null>(null);
  const [productEditingId, setProductEditingId] = useState<number | null>(null);
  const [clientEditingId, setClientEditingId] = useState<number | null>(null);

  const [companyForm, setCompanyForm] = useState<CompanyForm>(
    createEmptyCompanyForm(),
  );
  const [productForm, setProductForm] = useState<ProductForm>(
    createEmptyProductForm(),
  );
  const [clientForm, setClientForm] = useState<ClientForm>(
    createEmptyClientForm(),
  );

  const [emissionForm, setEmissionForm] = useState<EmissionForm>(
    createEmptyEmissionForm,
  );
  const [emissionItems, setEmissionItems] = useState<EmissionItemForm[]>([
    createEmptyEmissionItem(1),
  ]);
  const [sefazStatus, setSefazStatus] = useState<FiscalStatus | null>(null);
  const [fiscalResult, setFiscalResult] = useState<FiscalResponse | null>(null);
  const [consultaChave, setConsultaChave] = useState("");
  const [cancelProtocolo, setCancelProtocolo] = useState("");
  const [cancelJustificativa, setCancelJustificativa] = useState(
    "Cancelamento por erro de digitacao do pedido.",
  );

  useEffect(() => {
    void loadCurrentUser();
  }, []);

  useEffect(() => {
    if (authStatus === "authenticated") {
      void loadEmpresas();
    }
  }, [authStatus]);

  useEffect(() => {
    if (!selectedCompany) {
      setProdutos([]);
      setClientes([]);
      setProdutosPage(createEmptyPage<ProdutoEmpresa>());
      setClientesPage(createEmptyPage<ClienteEmpresa>());
      setNotasPageData(createEmptyPage<NotaFiscal>());
      return;
    }

    setProductForm(createEmptyProductForm());
    setClientForm(createEmptyClientForm(selectedCompany));
    setProductEditingId(null);
    setClientEditingId(null);
    setProductPage(0);
    setClientPage(0);
    setNotasPage(0);
    void loadRelations(selectedCompany.id);
  }, [selectedCompanyId]);

  useEffect(() => {
    if (!selectedCompany) {
      setProdutosPage(createEmptyPage<ProdutoEmpresa>());
      return;
    }

    void loadProdutosPage(selectedCompany.id, productPage, productNameFilter);
  }, [selectedCompanyId, productPage, productNameFilter]);

  useEffect(() => {
    if (!selectedCompany) {
      setClientesPage(createEmptyPage<ClienteEmpresa>());
      return;
    }

    void loadClientesPage(selectedCompany.id, clientPage, clientNameFilter);
  }, [selectedCompanyId, clientPage, clientNameFilter]);

  useEffect(() => {
    if (!selectedCompany) {
      setNotasPageData(createEmptyPage<NotaFiscal>());
      return;
    }

    void loadNotasPage(selectedCompany.id, notasPage);
  }, [selectedCompanyId, notasPage]);

  const activeCompanies = empresas.filter((empresa) => empresa.ativo);
  const activeProdutos = produtos.filter((produto) => produto.ativo);
  const activeClientes = clientes.filter((cliente) => cliente.ativo);
  const currentModule =
    moduleCatalog.find((moduleItem) => moduleItem.key === viewMode) ??
    moduleCatalog[0];
  const fiscalWarnings = buildFiscalWarnings(
    selectedCompany,
    clientes,
    produtos,
    emissionForm,
    emissionItems,
  );

  async function loadCurrentUser() {
    setAuthStatus("checking");
    setAuthError(null);

    try {
      const user = await api.auth.me();
      setAuthUser(user);
      setAuthStatus("authenticated");
    } catch (error) {
      setAuthUser(null);
      setAuthStatus("anonymous");

      if (error instanceof ApiRequestError && error.status === 401) {
        return;
      }

      setAuthError(getErrorMessage(error));
    }
  }

  async function handleLoginSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setAuthBusy(true);
    setAuthError(null);

    try {
      const response = await api.auth.signin(loginForm);
      setAuthUser(response.user);
      setAuthStatus("authenticated");
      setLoginForm({ username: response.username, password: "" });
      setNotice({
        kind: "success",
        text: `Acesso liberado para ${response.user.name}.`,
      });
    } catch (error) {
      setAuthError(getErrorMessage(error));
    } finally {
      setAuthBusy(false);
    }
  }

  async function handleLogout() {
    setAuthBusy(true);

    try {
      await api.auth.logout();
    } catch {
      // Mesmo se o cookie ja tiver expirado no servidor, limpamos o estado local.
    } finally {
      clearAuthenticatedState();
      setAuthBusy(false);
    }
  }

  function clearAuthenticatedState() {
    setAuthUser(null);
    setAuthStatus("anonymous");
    setNotice(null);
    setEmpresas([]);
    setSelectedCompanyId(null);
    setProdutos([]);
    setClientes([]);
    setProdutosPage(createEmptyPage<ProdutoEmpresa>());
    setClientesPage(createEmptyPage<ClienteEmpresa>());
    setNotasPageData(createEmptyPage<NotaFiscal>());
    setSefazStatus(null);
    setFiscalResult(null);
  }

  async function loadEmpresas() {
    setIsLoadingCompanies(true);
    try {
      const data = await api.empresas.list();
      setEmpresas(data);

      setSelectedCompanyId((currentId) => {
        if (currentId && data.some((empresa) => empresa.id === currentId)) {
          return currentId;
        }

        return data.find((empresa) => empresa.ativo)?.id ?? data[0]?.id ?? null;
      });
    } catch (error) {
      showError(error);
    } finally {
      setIsLoadingCompanies(false);
    }
  }

  async function loadRelations(empresaId: number) {
    setIsLoadingRelations(true);
    try {
      const [produtosData, clientesData] = await Promise.all([
        api.produtos.list(empresaId),
        api.clientes.list(empresaId),
      ]);

      setProdutos(produtosData);
      setClientes(clientesData);
    } catch (error) {
      showError(error);
    } finally {
      setIsLoadingRelations(false);
    }
  }

  async function loadProdutosPage(
    empresaId: number,
    page = productPage,
    nome = productNameFilter,
  ) {
    setIsLoadingProductPage(true);
    try {
      const data = await api.produtos.paginado(empresaId, {
        page,
        size: DEFAULT_PAGE_SIZE,
        nome,
      });
      setProdutosPage(data);
      normalizeCurrentPage(data, setProductPage);
    } catch (error) {
      showError(error);
    } finally {
      setIsLoadingProductPage(false);
    }
  }

  async function loadClientesPage(
    empresaId: number,
    page = clientPage,
    nome = clientNameFilter,
  ) {
    setIsLoadingClientPage(true);
    try {
      const data = await api.clientes.paginado(empresaId, {
        page,
        size: DEFAULT_PAGE_SIZE,
        nome,
      });
      setClientesPage(data);
      normalizeCurrentPage(data, setClientPage);
    } catch (error) {
      showError(error);
    } finally {
      setIsLoadingClientPage(false);
    }
  }

  async function loadNotasPage(empresaId: number, page = notasPage) {
    setIsLoadingNotas(true);
    try {
      const data = await api.notas.list(empresaId, {
        page,
        size: DEFAULT_PAGE_SIZE,
      });
      setNotasPageData(data);
      normalizeCurrentPage(data, setNotasPage);
    } catch (error) {
      showError(error);
    } finally {
      setIsLoadingNotas(false);
    }
  }

  async function handleCompanySubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormBusy("empresa");

    try {
      const payload = buildCompanyPayload(companyForm);
      const saved = companyEditingId
        ? await api.empresas.update(companyEditingId, payload)
        : await api.empresas.create(payload);

      setNotice({
        kind: "success",
        text: `Empresa ${companyEditingId ? "atualizada" : "criada"}: ${saved.nome}`,
      });
      setCompanyEditingId(null);
      setCompanyForm(createEmptyCompanyForm());
      await loadEmpresas();
      setSelectedCompanyId(saved.id);
    } catch (error) {
      showError(error);
    } finally {
      setFormBusy(null);
    }
  }

  async function handleCompanyCnpjLookup() {
    const cnpj = companyForm.cnpj.trim();

    if (!cnpj) {
      setNotice({ kind: "error", text: "Informe o CNPJ antes de buscar." });
      return;
    }

    setIntegrationBusy("company-cnpj");

    try {
      const data = await api.integracoes.cnpj(cnpj);

      setCompanyForm((form) => ({
        ...form,
        nome: form.nome.trim() || data.nomeFantasia || data.razaoSocial,
        cnpj: data.cnpj,
        razaoSocial: data.razaoSocial,
        nomeFantasia: data.nomeFantasia ?? form.nomeFantasia,
        cep: data.cep || form.cep,
        logradouro:
          data.logradouroCompleto || data.logradouro || form.logradouro,
        numero: data.numero || form.numero,
        bairro: data.bairro || form.bairro,
        codigoMunicipio: data.codigoMunicipioIbge || form.codigoMunicipio,
        descricaoMunicipio: data.municipio || form.descricaoMunicipio,
        uf: data.uf || form.uf,
      }));

      setNotice({
        kind: "success",
        text: `Dados encontrados para ${data.razaoSocial}.`,
      });
    } catch (error) {
      showError(error);
    } finally {
      setIntegrationBusy(null);
    }
  }

  async function handleCompanyCepLookup() {
    const cep = companyForm.cep.trim();

    if (!cep) {
      setNotice({ kind: "error", text: "Informe o CEP antes de buscar." });
      return;
    }

    setIntegrationBusy("company-cep");

    try {
      const data = await api.integracoes.cep(cep);

      setCompanyForm((form) => ({
        ...form,
        cep: data.cep || form.cep,
        logradouro:
          data.logradouroCompleto || data.logradouro || form.logradouro,
        bairro: data.bairro || form.bairro,
        codigoMunicipio: data.ibge || form.codigoMunicipio,
        descricaoMunicipio: data.localidade || form.descricaoMunicipio,
        uf: data.uf || form.uf,
      }));

      setNotice({
        kind: "success",
        text: `Endereco encontrado para ${data.localidade}/${data.uf}.`,
      });
    } catch (error) {
      showError(error);
    } finally {
      setIntegrationBusy(null);
    }
  }

  async function handleProductSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!selectedCompany) {
      setNotice({ kind: "error", text: "Selecione uma empresa primeiro." });
      return;
    }

    setFormBusy("produto");

    try {
      const payload = buildProductPayload(productForm);
      const saved = productEditingId
        ? await api.produtos.update(selectedCompany.id, productEditingId, payload)
        : await api.produtos.create(selectedCompany.id, payload);

      setNotice({
        kind: "success",
        text: `Produto ${productEditingId ? "atualizado" : "criado"}: ${saved.descricao}`,
      });
      setProductEditingId(null);
      setProductForm(createEmptyProductForm());
      const nextPage = productEditingId ? productPage : 0;
      if (!productEditingId) {
        setProductPage(0);
      }
      await Promise.all([
        loadRelations(selectedCompany.id),
        loadProdutosPage(selectedCompany.id, nextPage, productNameFilter),
      ]);
    } catch (error) {
      showError(error);
    } finally {
      setFormBusy(null);
    }
  }

  async function handleClientSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!selectedCompany) {
      setNotice({ kind: "error", text: "Selecione uma empresa primeiro." });
      return;
    }

    setFormBusy("cliente");

    try {
      const payload = buildClientPayload(clientForm);
      const saved = clientEditingId
        ? await api.clientes.update(selectedCompany.id, clientEditingId, payload)
        : await api.clientes.create(selectedCompany.id, payload);

      setNotice({
        kind: "success",
        text: `Cliente ${clientEditingId ? "atualizado" : "criado"}: ${saved.nome}`,
      });
      setClientEditingId(null);
      setClientForm(createEmptyClientForm(selectedCompany));
      const nextPage = clientEditingId ? clientPage : 0;
      if (!clientEditingId) {
        setClientPage(0);
      }
      await Promise.all([
        loadRelations(selectedCompany.id),
        loadClientesPage(selectedCompany.id, nextPage, clientNameFilter),
      ]);
    } catch (error) {
      showError(error);
    } finally {
      setFormBusy(null);
    }
  }

  async function handleClientCnpjLookup() {
    const cnpj = clientForm.cpfCnpj.trim();

    if (!cnpj) {
      setNotice({ kind: "error", text: "Informe o CNPJ do cliente antes de buscar." });
      return;
    }

    setIntegrationBusy("client-cnpj");

    try {
      const data = await api.integracoes.cnpj(cnpj);

      setClientForm((form) => ({
        ...form,
        nome: data.razaoSocial || data.nomeFantasia || form.nome,
        cpfCnpj: data.cnpj,
        cep: data.cep || form.cep,
        logradouro:
          data.logradouroCompleto || data.logradouro || form.logradouro,
        numero: data.numero || form.numero,
        bairro: data.bairro || form.bairro,
        codigoMunicipio: data.codigoMunicipioIbge || form.codigoMunicipio,
        descricaoMunicipio: data.municipio || form.descricaoMunicipio,
        uf: data.uf || form.uf,
        email: data.email ?? form.email,
      }));

      setNotice({
        kind: "success",
        text: `Cliente encontrado: ${data.razaoSocial}.`,
      });
    } catch (error) {
      showError(error);
    } finally {
      setIntegrationBusy(null);
    }
  }

  async function handleClientCepLookup() {
    const cep = clientForm.cep.trim();

    if (!cep) {
      setNotice({ kind: "error", text: "Informe o CEP do cliente antes de buscar." });
      return;
    }

    setIntegrationBusy("client-cep");

    try {
      const data = await api.integracoes.cep(cep);

      setClientForm((form) => ({
        ...form,
        cep: data.cep || form.cep,
        logradouro:
          data.logradouroCompleto || data.logradouro || form.logradouro,
        bairro: data.bairro || form.bairro,
        codigoMunicipio: data.ibge || form.codigoMunicipio,
        descricaoMunicipio: data.localidade || form.descricaoMunicipio,
        uf: data.uf || form.uf,
      }));

      setNotice({
        kind: "success",
        text: `Endereco do cliente encontrado para ${data.localidade}/${data.uf}.`,
      });
    } catch (error) {
      showError(error);
    } finally {
      setIntegrationBusy(null);
    }
  }

  async function handleSefazStatus() {
    if (!selectedCompany) {
      setNotice({ kind: "error", text: "Selecione uma empresa primeiro." });
      return;
    }

    setFiscalBusy("status");

    try {
      const data = await api.fiscal.status(
        selectedCompany.modelo,
        selectedCompany.id,
      );
      setSefazStatus(data);
      setNotice({ kind: "success", text: data.motivo });
    } catch (error) {
      showError(error);
    } finally {
      setFiscalBusy(null);
    }
  }

  async function handleXmlPreview() {
    await runFiscalEmission("preview");
  }

  async function handleEmitir() {
    await runFiscalEmission("emitir");
  }

  async function runFiscalEmission(action: "preview" | "emitir") {
    if (!selectedCompany) {
      setNotice({ kind: "error", text: "Selecione uma empresa primeiro." });
      return;
    }

    setFiscalBusy(action);

    try {
      const payload = buildEmissionPayload(
        selectedCompany,
        emissionForm,
        emissionItems,
      );
      const result =
        action === "preview"
          ? await api.fiscal.xmlPreview(selectedCompany.modelo, payload)
          : await api.fiscal.emitir(selectedCompany.modelo, payload);

      setFiscalResult(result);
      syncFiscalIdentifiers(result);
      setNotice({
        kind: result.cStat === 100 || action === "preview" ? "success" : "info",
        text: result.motivo,
      });
      if (action === "emitir") {
        setNotasPage(0);
        await loadNotasPage(selectedCompany.id, 0);
      }
    } catch (error) {
      showError(error);
    } finally {
      setFiscalBusy(null);
    }
  }

  async function handleConsultar() {
    if (!selectedCompany) {
      setNotice({ kind: "error", text: "Selecione uma empresa primeiro." });
      return;
    }

    if (!consultaChave.trim()) {
      setNotice({ kind: "error", text: "Informe a chave de acesso." });
      return;
    }

    setFiscalBusy("consultar");

    try {
      const result = await api.fiscal.consultar(selectedCompany.modelo, {
        empresaId: selectedCompany.id,
        chaveAcesso: consultaChave.trim(),
      });
      setFiscalResult(result);
      syncFiscalIdentifiers(result);
      setNotice({ kind: "success", text: result.motivo });
      await loadNotasPage(selectedCompany.id, notasPage);
    } catch (error) {
      showError(error);
    } finally {
      setFiscalBusy(null);
    }
  }

  async function handleCancelar() {
    if (!selectedCompany) {
      setNotice({ kind: "error", text: "Selecione uma empresa primeiro." });
      return;
    }

    if (!consultaChave.trim() || !cancelProtocolo.trim()) {
      setNotice({
        kind: "error",
        text: "Informe chave de acesso e protocolo de autorizacao.",
      });
      return;
    }

    if (
      cancelJustificativa.trim().length < 15 ||
      cancelJustificativa.trim().length > 255
    ) {
      setNotice({
        kind: "error",
        text: "A justificativa precisa ter entre 15 e 255 caracteres.",
      });
      return;
    }

    setFiscalBusy("cancelar");

    try {
      const result = await api.fiscal.cancelar(selectedCompany.modelo, {
        empresaId: selectedCompany.id,
        chaveAcesso: consultaChave.trim(),
        protocoloAutorizacao: cancelProtocolo.trim(),
        justificativa: cancelJustificativa.trim(),
      });
      setFiscalResult(result);
      syncFiscalIdentifiers(result);
      setNotice({ kind: "success", text: result.motivo });
      await loadNotasPage(selectedCompany.id, notasPage);
    } catch (error) {
      showError(error);
    } finally {
      setFiscalBusy(null);
    }
  }

  function syncFiscalIdentifiers(result: FiscalResponse) {
    if (result.chaveAcesso) {
      setConsultaChave(result.chaveAcesso);
    }

    if (result.protocoloAutorizacao) {
      setCancelProtocolo(result.protocoloAutorizacao);
    }
  }

  function handleProductFilterChange(value: string) {
    setProductNameFilter(value);
    setProductPage(0);
  }

  function handleClientFilterChange(value: string) {
    setClientNameFilter(value);
    setClientPage(0);
  }

  function openNotaForConsulta(nota: NotaFiscal) {
    if (nota.chaveAcesso) {
      setConsultaChave(nota.chaveAcesso);
    }

    setCancelProtocolo(nota.protocoloAutorizacao ?? "");
    setViewMode("consulta");
    setNotice({
      kind: "info",
      text: `Nota ${nota.numeroNota ?? nota.id} carregada para consulta.`,
    });
  }

  function editCompany(empresa: Empresa) {
    setCompanyEditingId(empresa.id);
    setCompanyForm({
      nome: empresa.nome ?? "",
      cnpj: empresa.cnpj ?? "",
      razaoSocial: empresa.razaoSocial ?? "",
      nomeFantasia: empresa.nomeFantasia ?? "",
      inscricaoEstadual: empresa.inscricaoEstadual ?? "",
      serie: empresa.serie ?? "1",
      naturezaOperacao: empresa.naturezaOperacao ?? "VENDA DE MERCADORIA",
      cep: empresa.cep ?? "",
      logradouro: empresa.logradouro ?? "",
      numero: empresa.numero ?? "",
      bairro: empresa.bairro ?? "",
      codigoMunicipio: empresa.codigoMunicipio ?? "",
      descricaoMunicipio: empresa.descricaoMunicipio ?? "",
      uf: empresa.uf ?? "GO",
      modelo: empresa.modelo,
      ambiente: empresa.ambiente,
      certificadoPfxPath: empresa.certificadoPfxPath ?? "",
      certificadoSenha: "",
      cadeiaJksPath: empresa.cadeiaJksPath ?? "",
      cadeiaSenha: "",
      csc: "",
      cscId: String(empresa.cscId ?? 0),
      ativo: empresa.ativo,
    });
    setViewMode("empresas");
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function editProduct(produto: ProdutoEmpresa) {
    setProductEditingId(produto.id);
    setProductForm({
      descricao: produto.descricao ?? "",
      codigoBarras: produto.codigoBarras ?? "",
      ncm: produto.ncm ?? "",
      cest: produto.cest ?? "",
      origem: produto.origem ?? "NACIONAL",
      cfop: produto.cfop ?? "5102",
      unidadeComercial: produto.unidadeComercial ?? "UN",
      precoUnitarioSugerido: String(produto.precoUnitarioSugerido ?? 0),
      ativo: produto.ativo,
    });
    setViewMode("produtos");
  }

  function editClient(cliente: ClienteEmpresa) {
    setClientEditingId(cliente.id);
    setClientForm({
      nome: cliente.nome ?? "",
      cpfCnpj: cliente.cpfCnpj ?? "",
      inscricaoEstadual: cliente.inscricaoEstadual ?? "",
      cep: cliente.cep ?? "",
      logradouro: cliente.logradouro ?? "",
      numero: cliente.numero ?? "",
      bairro: cliente.bairro ?? "",
      codigoMunicipio: cliente.codigoMunicipio ?? "",
      descricaoMunicipio: cliente.descricaoMunicipio ?? "",
      uf: cliente.uf ?? selectedCompany?.uf ?? "GO",
      email: cliente.email ?? "",
      ativo: cliente.ativo,
    });
    setViewMode("clientes");
  }

  function updateEmissionItem(
    index: number,
    patch: Partial<EmissionItemForm>,
  ) {
    setEmissionItems((currentItems) =>
      currentItems.map((item, itemIndex) =>
        itemIndex === index ? { ...item, ...patch } : item,
      ),
    );
  }

  function updateEmissionItemProduct(index: number, produtoId: string) {
    const produto = produtos.find((item) => String(item.id) === produtoId);

    setEmissionItems((currentItems) =>
      currentItems.map((item, itemIndex) =>
        itemIndex === index
          ? {
              ...item,
              produtoEmpresaId: produtoId,
              precoUnitario:
                produto?.precoUnitarioSugerido !== undefined
                  ? String(produto.precoUnitarioSugerido)
                  : item.precoUnitario,
            }
          : item,
      ),
    );
  }

  function addEmissionItem() {
    setEmissionItems((currentItems) => [
      ...currentItems,
      createEmptyEmissionItem(currentItems.length + 1),
    ]);
  }

  function removeEmissionItem(index: number) {
    setEmissionItems((currentItems) => {
      const nextItems = currentItems.filter((_, itemIndex) => itemIndex !== index);
      return nextItems.length
        ? nextItems.map((item, itemIndex) => ({
            ...item,
            itemId: String(itemIndex + 1),
          }))
        : [createEmptyEmissionItem(1)];
    });
  }

  function showError(error: unknown) {
    if (error instanceof ApiRequestError && error.status === 401) {
      clearAuthenticatedState();
      setAuthError("Sua sessao expirou. Entre novamente.");
      return;
    }

    setNotice({ kind: "error", text: getErrorMessage(error) });
  }

  if (authStatus === "checking") {
    return (
      <main className="auth-shell">
        <div className="auth-panel compact-auth-panel">
          <div className="brand-mark">IF</div>
          <strong>Validando sessao...</strong>
        </div>
      </main>
    );
  }

  if (authStatus === "anonymous") {
    return (
      <LoginScreen
        form={loginForm}
        error={authError}
        isLoading={authBusy}
        setForm={setLoginForm}
        onSubmit={handleLoginSubmit}
      />
    );
  }

  return (
    <div className="erp-shell">
      <aside className="erp-sidebar">
        <div className="brand-block">
          <div className="brand-mark">IF</div>
          <div>
            <strong>ISA Fiscal</strong>
            <span>ERP Fiscal</span>
          </div>
        </div>

        <nav className="erp-menu" aria-label="Menu principal">
          <span className="menu-heading">Cadastros</span>
          {moduleCatalog
            .filter((moduleItem) => moduleItem.group === "Cadastros")
            .map((moduleItem) => (
              <button
                className={viewMode === moduleItem.key ? "active" : ""}
                key={moduleItem.key}
                onClick={() => setViewMode(moduleItem.key)}
                type="button"
              >
                <span>{moduleItem.kicker}</span>
                <strong>{moduleItem.label}</strong>
              </button>
            ))}

          <span className="menu-heading">Fiscal</span>
          {moduleCatalog
            .filter((moduleItem) => moduleItem.group === "Fiscal")
            .map((moduleItem) => (
              <button
                className={viewMode === moduleItem.key ? "active" : ""}
                key={moduleItem.key}
                onClick={() => setViewMode(moduleItem.key)}
                type="button"
              >
                <span>{moduleItem.kicker}</span>
                <strong>{moduleItem.label}</strong>
              </button>
            ))}
        </nav>

        <div className="backend-card">
          <span className="panel-label">Backend</span>
          <strong>{API_BASE_LABEL}</strong>
          {authUser && (
            <div className="session-summary">
              <span>{authUser.role}</span>
              <strong>{authUser.name}</strong>
            </div>
          )}
          <button
            className="ghost-button"
            onClick={() => void loadEmpresas()}
            type="button"
          >
            {isLoadingCompanies ? "Sincronizando..." : "Sincronizar"}
          </button>
        </div>
      </aside>

      <div className="erp-workspace">
        <header className="erp-topbar">
          <div>
            <span className="eyebrow">{currentModule.kicker}</span>
            <h1>{currentModule.label}</h1>
            <p>{currentModule.description}</p>
          </div>

          <div className="topbar-actions">
            {authUser && (
              <div className="user-chip">
                <span>{authUser.role}</span>
                <strong>{authUser.name}</strong>
              </div>
            )}
            <button
              className="ghost-button"
              onClick={() => void loadEmpresas()}
              type="button"
            >
              {isLoadingCompanies ? "Carregando..." : "Atualizar"}
            </button>
            <button
              className="ghost-button"
              disabled={authBusy}
              onClick={() => void handleLogout()}
              type="button"
            >
              {authBusy ? "Saindo..." : "Sair"}
            </button>
          </div>
        </header>

        {notice && (
          <div className={`notice notice-${notice.kind}`}>
            <span>{notice.text}</span>
            <button type="button" onClick={() => setNotice(null)}>
              Fechar
            </button>
          </div>
        )}

        <section className="erp-context-bar">
          <label className="field company-selector">
            <span>Empresa ativa</span>
            <select
              value={selectedCompanyId ?? ""}
              onChange={(event) =>
                setSelectedCompanyId(
                  event.target.value ? Number(event.target.value) : null,
                )
              }
            >
              <option value="">Selecione uma empresa</option>
              {empresas.map((empresa) => (
                <option key={empresa.id} value={empresa.id}>
                  {empresa.nome} - {fiscalModelLabel[empresa.modelo]}
                  {empresa.ativo ? "" : " (inativa)"}
                </option>
              ))}
            </select>
          </label>

          <div className="stats-strip">
            <Metric label="Empresas ativas" value={activeCompanies.length} />
            <Metric label="Produtos ativos" value={activeProdutos.length} />
            <Metric label="Clientes ativos" value={activeClientes.length} />
          </div>
        </section>

        {selectedCompany && (
          <section className="company-ribbon">
            <div>
              <span className="panel-label">Empresa selecionada</span>
              <strong>{selectedCompany.nome}</strong>
            </div>
            <StatusPill active={selectedCompany.ativo} />
            <Badge>{fiscalModelLabel[selectedCompany.modelo]}</Badge>
            <Badge>{ambienteLabel[selectedCompany.ambiente]}</Badge>
            <Badge>UF {selectedCompany.uf}</Badge>
          </section>
        )}

        {viewMode === "empresas" ||
        viewMode === "produtos" ||
        viewMode === "clientes" ? (
          <CadastroView
            activeModule={viewMode}
            empresas={empresas}
            produtos={produtos}
            clientes={clientes}
            produtosPage={produtosPage}
            clientesPage={clientesPage}
            selectedCompany={selectedCompany}
            isLoadingRelations={isLoadingRelations}
            isLoadingProductPage={isLoadingProductPage}
            isLoadingClientPage={isLoadingClientPage}
            productNameFilter={productNameFilter}
            clientNameFilter={clientNameFilter}
            companyForm={companyForm}
            productForm={productForm}
            clientForm={clientForm}
            companyEditingId={companyEditingId}
            productEditingId={productEditingId}
            clientEditingId={clientEditingId}
            formBusy={formBusy}
            integrationBusy={integrationBusy}
            onCompanySubmit={handleCompanySubmit}
            onCompanyCnpjLookup={handleCompanyCnpjLookup}
            onCompanyCepLookup={handleCompanyCepLookup}
            onProductSubmit={handleProductSubmit}
            onClientSubmit={handleClientSubmit}
            onClientCnpjLookup={handleClientCnpjLookup}
            onClientCepLookup={handleClientCepLookup}
            onProductFilterChange={handleProductFilterChange}
            onClientFilterChange={handleClientFilterChange}
            onProductPageChange={setProductPage}
            onClientPageChange={setClientPage}
            onRefreshProducts={() =>
              selectedCompany &&
              void loadProdutosPage(
                selectedCompany.id,
                productPage,
                productNameFilter,
              )
            }
            onRefreshClients={() =>
              selectedCompany &&
              void loadClientesPage(
                selectedCompany.id,
                clientPage,
                clientNameFilter,
              )
            }
            setCompanyForm={setCompanyForm}
            setProductForm={setProductForm}
            setClientForm={setClientForm}
            onEditCompany={editCompany}
            onEditProduct={editProduct}
            onEditClient={editClient}
            onCancelCompanyEdit={() => {
              setCompanyEditingId(null);
              setCompanyForm(createEmptyCompanyForm());
            }}
            onCancelProductEdit={() => {
              setProductEditingId(null);
              setProductForm(createEmptyProductForm());
            }}
            onCancelClientEdit={() => {
              setClientEditingId(null);
              setClientForm(createEmptyClientForm(selectedCompany ?? undefined));
            }}
          />
        ) : (
          <FiscalView
            activeModule={viewMode}
            selectedCompany={selectedCompany}
            produtos={produtos}
            clientes={clientes}
            emissionForm={emissionForm}
            emissionItems={emissionItems}
            sefazStatus={sefazStatus}
            fiscalResult={fiscalResult}
            notasPageData={notasPageData}
            consultaChave={consultaChave}
            cancelProtocolo={cancelProtocolo}
            cancelJustificativa={cancelJustificativa}
            fiscalBusy={fiscalBusy}
            fiscalWarnings={fiscalWarnings}
            isLoadingNotas={isLoadingNotas}
            setEmissionForm={setEmissionForm}
            updateEmissionItem={updateEmissionItem}
            updateEmissionItemProduct={updateEmissionItemProduct}
            addEmissionItem={addEmissionItem}
            removeEmissionItem={removeEmissionItem}
            onStatus={handleSefazStatus}
            onPreview={handleXmlPreview}
            onEmitir={handleEmitir}
            onConsultar={handleConsultar}
            onCancelar={handleCancelar}
            onNotasPageChange={setNotasPage}
            onRefreshNotas={() =>
              selectedCompany && void loadNotasPage(selectedCompany.id, notasPage)
            }
            onUseNota={openNotaForConsulta}
            setConsultaChave={setConsultaChave}
            setCancelProtocolo={setCancelProtocolo}
            setCancelJustificativa={setCancelJustificativa}
          />
        )}
      </div>
    </div>
  );
}

interface CadastroViewProps {
  activeModule: CadastroMode;
  empresas: Empresa[];
  produtos: ProdutoEmpresa[];
  clientes: ClienteEmpresa[];
  produtosPage: PageResponse<ProdutoEmpresa>;
  clientesPage: PageResponse<ClienteEmpresa>;
  selectedCompany: Empresa | null;
  isLoadingRelations: boolean;
  isLoadingProductPage: boolean;
  isLoadingClientPage: boolean;
  productNameFilter: string;
  clientNameFilter: string;
  companyForm: CompanyForm;
  productForm: ProductForm;
  clientForm: ClientForm;
  companyEditingId: number | null;
  productEditingId: number | null;
  clientEditingId: number | null;
  formBusy: string | null;
  integrationBusy: string | null;
  onCompanySubmit: (event: FormEvent<HTMLFormElement>) => void;
  onCompanyCnpjLookup: () => void;
  onCompanyCepLookup: () => void;
  onProductSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onClientSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onClientCnpjLookup: () => void;
  onClientCepLookup: () => void;
  onProductFilterChange: (value: string) => void;
  onClientFilterChange: (value: string) => void;
  onProductPageChange: (page: number) => void;
  onClientPageChange: (page: number) => void;
  onRefreshProducts: () => void;
  onRefreshClients: () => void;
  setCompanyForm: Dispatch<SetStateAction<CompanyForm>>;
  setProductForm: Dispatch<SetStateAction<ProductForm>>;
  setClientForm: Dispatch<SetStateAction<ClientForm>>;
  onEditCompany: (empresa: Empresa) => void;
  onEditProduct: (produto: ProdutoEmpresa) => void;
  onEditClient: (cliente: ClienteEmpresa) => void;
  onCancelCompanyEdit: () => void;
  onCancelProductEdit: () => void;
  onCancelClientEdit: () => void;
}

interface LoginScreenProps {
  form: LoginForm;
  error: string | null;
  isLoading: boolean;
  setForm: Dispatch<SetStateAction<LoginForm>>;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
}

function LoginScreen({
  form,
  error,
  isLoading,
  setForm,
  onSubmit,
}: LoginScreenProps) {
  return (
    <main className="auth-shell">
      <section className="auth-panel">
        <div className="brand-block auth-brand">
          <div className="brand-mark">IF</div>
          <div>
            <strong>ISA Fiscal</strong>
            <span>Acesso seguro</span>
          </div>
        </div>

        <form className="auth-form" onSubmit={onSubmit}>
          <TextField
            label="Usuario"
            value={form.username}
            onChange={(value) =>
              setForm((currentForm) => ({ ...currentForm, username: value }))
            }
            autoComplete="username"
            required
          />
          <TextField
            label="Senha"
            type="password"
            value={form.password}
            onChange={(value) =>
              setForm((currentForm) => ({ ...currentForm, password: value }))
            }
            autoComplete="current-password"
            required
          />

          {error && <p className="auth-error">{error}</p>}

          <button className="primary-button" disabled={isLoading} type="submit">
            {isLoading ? "Entrando..." : "Entrar"}
          </button>
        </form>
      </section>
    </main>
  );
}

function CadastroView({
  activeModule,
  empresas,
  produtos,
  clientes,
  produtosPage,
  clientesPage,
  selectedCompany,
  isLoadingRelations,
  isLoadingProductPage,
  isLoadingClientPage,
  productNameFilter,
  clientNameFilter,
  companyForm,
  productForm,
  clientForm,
  companyEditingId,
  productEditingId,
  clientEditingId,
  formBusy,
  integrationBusy,
  onCompanySubmit,
  onCompanyCnpjLookup,
  onCompanyCepLookup,
  onProductSubmit,
  onClientSubmit,
  onClientCnpjLookup,
  onClientCepLookup,
  onProductFilterChange,
  onClientFilterChange,
  onProductPageChange,
  onClientPageChange,
  onRefreshProducts,
  onRefreshClients,
  setCompanyForm,
  setProductForm,
  setClientForm,
  onEditCompany,
  onEditProduct,
  onEditClient,
  onCancelCompanyEdit,
  onCancelProductEdit,
  onCancelClientEdit,
}: CadastroViewProps) {
  const disableCompanyPasswords =
    Boolean(companyEditingId) &&
    !companyForm.certificadoSenha &&
    !companyForm.cadeiaSenha;

  return (
    <main className="page-grid">
      {activeModule === "empresas" && (
        <>
          <section className="card span-12">
        <div className="section-heading">
          <div>
            <span className="eyebrow">Multiempresa</span>
            <h2>{companyEditingId ? "Editar empresa" : "Nova empresa"}</h2>
          </div>
          {companyEditingId && (
            <button className="ghost-button" onClick={onCancelCompanyEdit}>
              Cancelar edicao
            </button>
          )}
        </div>

        <form className="form-grid" onSubmit={onCompanySubmit}>
          <TextField
            label="Nome"
            value={companyForm.nome}
            onChange={(value) =>
              setCompanyForm((form) => ({ ...form, nome: value }))
            }
            required
          />
          <div className="lookup-field">
            <TextField
              label="CNPJ"
              value={companyForm.cnpj}
              onChange={(value) =>
                setCompanyForm((form) => ({ ...form, cnpj: value }))
              }
              required
            />
            <button
              className="ghost-button lookup-button"
              disabled={integrationBusy === "company-cnpj" || !companyForm.cnpj}
              onClick={onCompanyCnpjLookup}
              title="Buscar dados do CNPJ"
              type="button"
            >
              {integrationBusy === "company-cnpj" ? "Buscando..." : "Buscar"}
            </button>
          </div>
          <TextField
            label="Razao social"
            value={companyForm.razaoSocial}
            onChange={(value) =>
              setCompanyForm((form) => ({ ...form, razaoSocial: value }))
            }
            required
          />
          <TextField
            label="Nome fantasia"
            value={companyForm.nomeFantasia}
            onChange={(value) =>
              setCompanyForm((form) => ({ ...form, nomeFantasia: value }))
            }
          />
          <TextField
            label="Inscricao estadual"
            value={companyForm.inscricaoEstadual}
            onChange={(value) =>
              setCompanyForm((form) => ({ ...form, inscricaoEstadual: value }))
            }
          />
          <TextField
            label="Serie"
            value={companyForm.serie}
            onChange={(value) =>
              setCompanyForm((form) => ({ ...form, serie: value }))
            }
            required
          />
          <TextField
            className="span-2"
            label="Natureza da operacao"
            value={companyForm.naturezaOperacao}
            onChange={(value) =>
              setCompanyForm((form) => ({ ...form, naturezaOperacao: value }))
            }
            required
          />
          <SelectField
            label="Modelo"
            value={companyForm.modelo}
            onChange={(value) =>
              setCompanyForm((form) => ({
                ...form,
                modelo: value as FiscalModel,
                cscId:
                  value === "NFCE" && toNumber(form.cscId, 0) <= 0
                    ? "1"
                    : form.cscId,
              }))
            }
            options={[
              { value: "NFE", label: "NF-e" },
              { value: "NFCE", label: "NFC-e" },
            ]}
          />
          <SelectField
            label="Ambiente"
            value={companyForm.ambiente}
            onChange={(value) =>
              setCompanyForm((form) => ({
                ...form,
                ambiente: value as Ambiente,
              }))
            }
            options={[
              { value: "HOMOLOGACAO", label: "Homologacao" },
              { value: "PRODUCAO", label: "Producao" },
            ]}
          />
          <div className="lookup-field">
            <TextField
              label="CEP"
              value={companyForm.cep}
              onChange={(value) =>
                setCompanyForm((form) => ({ ...form, cep: value }))
              }
              required
            />
            <button
              className="ghost-button lookup-button"
              disabled={integrationBusy === "company-cep" || !companyForm.cep}
              onClick={onCompanyCepLookup}
              title="Buscar endereco pelo CEP"
              type="button"
            >
              {integrationBusy === "company-cep" ? "Buscando..." : "Buscar"}
            </button>
          </div>
          <TextField
            label="Logradouro"
            value={companyForm.logradouro}
            onChange={(value) =>
              setCompanyForm((form) => ({ ...form, logradouro: value }))
            }
            required
          />
          <TextField
            label="Numero"
            value={companyForm.numero}
            onChange={(value) =>
              setCompanyForm((form) => ({ ...form, numero: value }))
            }
            required
          />
          <TextField
            label="Bairro"
            value={companyForm.bairro}
            onChange={(value) =>
              setCompanyForm((form) => ({ ...form, bairro: value }))
            }
            required
          />
          <TextField
            label="Cod. municipio"
            value={companyForm.codigoMunicipio}
            onChange={(value) =>
              setCompanyForm((form) => ({ ...form, codigoMunicipio: value }))
            }
            required
          />
          <TextField
            label="Municipio"
            value={companyForm.descricaoMunicipio}
            onChange={(value) =>
              setCompanyForm((form) => ({
                ...form,
                descricaoMunicipio: value,
              }))
            }
            required
          />
          <TextField
            label="UF"
            value={companyForm.uf}
            onChange={(value) =>
              setCompanyForm((form) => ({
                ...form,
                uf: value.toUpperCase().slice(0, 2),
              }))
            }
            required
          />
          <TextField
            className="span-2"
            label="Caminho certificado PFX"
            value={companyForm.certificadoPfxPath}
            onChange={(value) =>
              setCompanyForm((form) => ({ ...form, certificadoPfxPath: value }))
            }
            required
          />
          <TextField
            label="Senha PFX"
            type="password"
            value={companyForm.certificadoSenha}
            onChange={(value) =>
              setCompanyForm((form) => ({ ...form, certificadoSenha: value }))
            }
            helper={
              disableCompanyPasswords
                ? "Em edicao, deixe em branco para manter a senha salva."
                : undefined
            }
          />
          <TextField
            className="span-2"
            label="Caminho cadeia JKS"
            value={companyForm.cadeiaJksPath}
            onChange={(value) =>
              setCompanyForm((form) => ({ ...form, cadeiaJksPath: value }))
            }
            required
          />
          <TextField
            label="Senha JKS"
            type="password"
            value={companyForm.cadeiaSenha}
            onChange={(value) =>
              setCompanyForm((form) => ({ ...form, cadeiaSenha: value }))
            }
          />
          {companyForm.modelo === "NFCE" && (
            <>
              <TextField
                label="CSC"
                type="password"
                value={companyForm.csc}
                onChange={(value) =>
                  setCompanyForm((form) => ({ ...form, csc: value }))
                }
                required
              />
              <TextField
                label="CSC ID"
                type="number"
                value={companyForm.cscId}
                onChange={(value) =>
                  setCompanyForm((form) => ({ ...form, cscId: value }))
                }
                min="1"
                step="1"
                required
              />
            </>
          )}
          <ToggleField
            label="Empresa ativa"
            checked={companyForm.ativo}
            onChange={(checked) =>
              setCompanyForm((form) => ({ ...form, ativo: checked }))
            }
          />
          <div className="form-actions">
            <button className="primary-button" disabled={formBusy === "empresa"}>
              {formBusy === "empresa"
                ? "Salvando..."
                : companyEditingId
                  ? "Atualizar empresa"
                  : "Criar empresa"}
            </button>
          </div>
        </form>
          </section>

          <section className="card span-12">
            <div className="section-heading">
              <div>
                <span className="eyebrow">Emitentes</span>
                <h2>Empresas cadastradas</h2>
              </div>
            </div>
            <CompanyTable empresas={empresas} onEditCompany={onEditCompany} />
          </section>
        </>
      )}

      {activeModule === "produtos" && (
        <section className="card span-12">
        <div className="section-heading">
          <div>
            <span className="eyebrow">Catalogo</span>
            <h2>{productEditingId ? "Editar produto" : "Novo produto"}</h2>
          </div>
          {productEditingId && (
            <button className="ghost-button" onClick={onCancelProductEdit}>
              Cancelar
            </button>
          )}
        </div>

        <form className="stacked-form" onSubmit={onProductSubmit}>
          <TextField
            label="Descricao"
            value={productForm.descricao}
            onChange={(value) =>
              setProductForm((form) => ({ ...form, descricao: value }))
            }
            required
            disabled={!selectedCompany}
          />
          <div className="inline-fields">
            <TextField
              label="Codigo barras"
              value={productForm.codigoBarras}
              onChange={(value) =>
                setProductForm((form) => ({ ...form, codigoBarras: value }))
              }
              disabled={!selectedCompany}
            />
            <TextField
              label="NCM"
              value={productForm.ncm}
              onChange={(value) =>
                setProductForm((form) => ({ ...form, ncm: value }))
              }
              required
              disabled={!selectedCompany}
            />
          </div>
          <div className="inline-fields">
            <TextField
              label="CEST"
              value={productForm.cest}
              onChange={(value) =>
                setProductForm((form) => ({ ...form, cest: value }))
              }
              disabled={!selectedCompany}
            />
            <TextField
              label="Origem"
              value={productForm.origem}
              onChange={(value) =>
                setProductForm((form) => ({ ...form, origem: value }))
              }
              required
              disabled={!selectedCompany}
            />
          </div>
          <div className="inline-fields">
            <TextField
              label="CFOP"
              value={productForm.cfop}
              onChange={(value) =>
                setProductForm((form) => ({ ...form, cfop: value }))
              }
              required
              disabled={!selectedCompany}
            />
            <TextField
              label="Unidade"
              value={productForm.unidadeComercial}
              onChange={(value) =>
                setProductForm((form) => ({ ...form, unidadeComercial: value }))
              }
              required
              disabled={!selectedCompany}
            />
          </div>
          <TextField
            label="Preco sugerido"
            type="number"
            step="0.01"
            min="0"
            value={productForm.precoUnitarioSugerido}
            onChange={(value) =>
              setProductForm((form) => ({
                ...form,
                precoUnitarioSugerido: value,
              }))
            }
            required
            disabled={!selectedCompany}
          />
          <ToggleField
            label="Produto ativo"
            checked={productForm.ativo}
            onChange={(checked) =>
              setProductForm((form) => ({ ...form, ativo: checked }))
            }
            disabled={!selectedCompany}
          />
          <button
            className="primary-button"
            disabled={!selectedCompany || formBusy === "produto"}
          >
            {formBusy === "produto"
              ? "Salvando..."
              : productEditingId
                ? "Atualizar produto"
                : "Criar produto"}
          </button>
        </form>

        <div className="list-toolbar">
          <TextField
            label="Filtrar por descricao"
            value={productNameFilter}
            onChange={onProductFilterChange}
            placeholder="Ex.: tijolo"
            disabled={!selectedCompany}
          />
          <button
            className="ghost-button"
            disabled={!selectedCompany || isLoadingProductPage}
            onClick={onRefreshProducts}
            type="button"
          >
            {isLoadingProductPage ? "Atualizando..." : "Atualizar lista"}
          </button>
        </div>

        <RecordList
          emptyLabel={
            isLoadingProductPage || isLoadingRelations
              ? "Carregando produtos..."
              : productNameFilter
                ? "Nenhum produto encontrado para o filtro."
                : "Nenhum produto."
          }
        >
          {produtosPage.items.map((produto) => (
            <article className="record-card" key={produto.id}>
              <div>
                <strong>{produto.descricao}</strong>
                <span>
                  CFOP {produto.cfop} - {produto.unidadeComercial} -{" "}
                  {formatMoney(produto.precoUnitarioSugerido)}
                </span>
              </div>
              <div className="record-actions">
                <StatusPill active={produto.ativo} />
                <button
                  className="ghost-button compact"
                  onClick={() => onEditProduct(produto)}
                  type="button"
                >
                  Editar
                </button>
              </div>
            </article>
          ))}
        </RecordList>
        <PaginationBar
          pageInfo={produtosPage}
          isLoading={isLoadingProductPage}
          onPageChange={onProductPageChange}
        />
        </section>
      )}

      {activeModule === "clientes" && (
        <section className="card span-12">
        <div className="section-heading">
          <div>
            <span className="eyebrow">Destinatarios</span>
            <h2>{clientEditingId ? "Editar cliente" : "Novo cliente"}</h2>
          </div>
          {clientEditingId && (
            <button className="ghost-button" onClick={onCancelClientEdit}>
              Cancelar
            </button>
          )}
        </div>

        <form className="stacked-form" onSubmit={onClientSubmit}>
          <TextField
            label="Nome"
            value={clientForm.nome}
            onChange={(value) =>
              setClientForm((form) => ({ ...form, nome: value }))
            }
            required
            disabled={!selectedCompany}
          />
          <div className="inline-fields">
            <div className="lookup-field">
              <TextField
                label="CPF/CNPJ"
                value={clientForm.cpfCnpj}
                onChange={(value) =>
                  setClientForm((form) => ({ ...form, cpfCnpj: value }))
                }
                helper="Busca automatica apenas para CNPJ."
                required
                disabled={!selectedCompany}
              />
              <button
                className="ghost-button lookup-button"
                disabled={
                  !selectedCompany ||
                  integrationBusy === "client-cnpj" ||
                  !clientForm.cpfCnpj
                }
                onClick={onClientCnpjLookup}
                title="Buscar dados do CNPJ do cliente"
                type="button"
              >
                {integrationBusy === "client-cnpj" ? "Buscando..." : "Buscar"}
              </button>
            </div>
            <TextField
              label="Inscricao estadual"
              value={clientForm.inscricaoEstadual}
              onChange={(value) =>
                setClientForm((form) => ({
                  ...form,
                  inscricaoEstadual: value,
                }))
              }
              helper="Use ISENTO para contribuinte isento."
              disabled={!selectedCompany}
            />
          </div>
          <div className="inline-fields">
            <div className="lookup-field">
              <TextField
                label="CEP"
                value={clientForm.cep}
                onChange={(value) =>
                  setClientForm((form) => ({ ...form, cep: value }))
                }
                required
                disabled={!selectedCompany}
              />
              <button
                className="ghost-button lookup-button"
                disabled={
                  !selectedCompany ||
                  integrationBusy === "client-cep" ||
                  !clientForm.cep
                }
                onClick={onClientCepLookup}
                title="Buscar endereco pelo CEP do cliente"
                type="button"
              >
                {integrationBusy === "client-cep" ? "Buscando..." : "Buscar"}
              </button>
            </div>
            <TextField
              label="UF"
              value={clientForm.uf}
              onChange={(value) =>
                setClientForm((form) => ({
                  ...form,
                  uf: value.toUpperCase().slice(0, 2),
                }))
              }
              required
              disabled={!selectedCompany}
            />
          </div>
          <TextField
            label="Logradouro"
            value={clientForm.logradouro}
            onChange={(value) =>
              setClientForm((form) => ({ ...form, logradouro: value }))
            }
            required
            disabled={!selectedCompany}
          />
          <div className="inline-fields">
            <TextField
              label="Numero"
              value={clientForm.numero}
              onChange={(value) =>
                setClientForm((form) => ({ ...form, numero: value }))
              }
              required
              disabled={!selectedCompany}
            />
            <TextField
              label="Bairro"
              value={clientForm.bairro}
              onChange={(value) =>
                setClientForm((form) => ({ ...form, bairro: value }))
              }
              required
              disabled={!selectedCompany}
            />
          </div>
          <div className="inline-fields">
            <TextField
              label="Cod. municipio"
              value={clientForm.codigoMunicipio}
              onChange={(value) =>
                setClientForm((form) => ({ ...form, codigoMunicipio: value }))
              }
              required
              disabled={!selectedCompany}
            />
            <TextField
              label="Municipio"
              value={clientForm.descricaoMunicipio}
              onChange={(value) =>
                setClientForm((form) => ({
                  ...form,
                  descricaoMunicipio: value,
                }))
              }
              required
              disabled={!selectedCompany}
            />
          </div>
          <TextField
            label="E-mail"
            type="email"
            value={clientForm.email}
            onChange={(value) =>
              setClientForm((form) => ({ ...form, email: value }))
            }
            disabled={!selectedCompany}
          />
          <ToggleField
            label="Cliente ativo"
            checked={clientForm.ativo}
            onChange={(checked) =>
              setClientForm((form) => ({ ...form, ativo: checked }))
            }
            disabled={!selectedCompany}
          />
          <button
            className="primary-button"
            disabled={!selectedCompany || formBusy === "cliente"}
          >
            {formBusy === "cliente"
              ? "Salvando..."
              : clientEditingId
                ? "Atualizar cliente"
                : "Criar cliente"}
          </button>
        </form>

        <div className="list-toolbar">
          <TextField
            label="Filtrar por nome"
            value={clientNameFilter}
            onChange={onClientFilterChange}
            placeholder="Ex.: maria"
            disabled={!selectedCompany}
          />
          <button
            className="ghost-button"
            disabled={!selectedCompany || isLoadingClientPage}
            onClick={onRefreshClients}
            type="button"
          >
            {isLoadingClientPage ? "Atualizando..." : "Atualizar lista"}
          </button>
        </div>

        <RecordList
          emptyLabel={
            isLoadingClientPage || isLoadingRelations
              ? "Carregando clientes..."
              : clientNameFilter
                ? "Nenhum cliente encontrado para o filtro."
                : "Nenhum cliente."
          }
        >
          {clientesPage.items.map((cliente) => (
            <article className="record-card" key={cliente.id}>
              <div>
                <strong>{cliente.nome}</strong>
                <span>
                  {cliente.cpfCnpj} - {cliente.uf} - IE{" "}
                  {cliente.inscricaoEstadual || "nao informada"}
                </span>
              </div>
              <div className="record-actions">
                <StatusPill active={cliente.ativo} />
                <button
                  className="ghost-button compact"
                  onClick={() => onEditClient(cliente)}
                  type="button"
                >
                  Editar
                </button>
              </div>
            </article>
          ))}
        </RecordList>
        <PaginationBar
          pageInfo={clientesPage}
          isLoading={isLoadingClientPage}
          onPageChange={onClientPageChange}
        />
        </section>
      )}
    </main>
  );
}

interface FiscalViewProps {
  activeModule: FiscalMode;
  selectedCompany: Empresa | null;
  produtos: ProdutoEmpresa[];
  clientes: ClienteEmpresa[];
  emissionForm: EmissionForm;
  emissionItems: EmissionItemForm[];
  sefazStatus: FiscalStatus | null;
  fiscalResult: FiscalResponse | null;
  notasPageData: PageResponse<NotaFiscal>;
  consultaChave: string;
  cancelProtocolo: string;
  cancelJustificativa: string;
  fiscalBusy: string | null;
  fiscalWarnings: string[];
  isLoadingNotas: boolean;
  setEmissionForm: Dispatch<SetStateAction<EmissionForm>>;
  updateEmissionItem: (index: number, patch: Partial<EmissionItemForm>) => void;
  updateEmissionItemProduct: (index: number, produtoId: string) => void;
  addEmissionItem: () => void;
  removeEmissionItem: (index: number) => void;
  onStatus: () => void;
  onPreview: () => void;
  onEmitir: () => void;
  onConsultar: () => void;
  onCancelar: () => void;
  onNotasPageChange: (page: number) => void;
  onRefreshNotas: () => void;
  onUseNota: (nota: NotaFiscal) => void;
  setConsultaChave: (value: string) => void;
  setCancelProtocolo: (value: string) => void;
  setCancelJustificativa: (value: string) => void;
}

function FiscalView({
  activeModule,
  selectedCompany,
  produtos,
  clientes,
  emissionForm,
  emissionItems,
  sefazStatus,
  fiscalResult,
  notasPageData,
  consultaChave,
  cancelProtocolo,
  cancelJustificativa,
  fiscalBusy,
  fiscalWarnings,
  isLoadingNotas,
  setEmissionForm,
  updateEmissionItem,
  updateEmissionItemProduct,
  addEmissionItem,
  removeEmissionItem,
  onStatus,
  onPreview,
  onEmitir,
  onConsultar,
  onCancelar,
  onNotasPageChange,
  onRefreshNotas,
  onUseNota,
  setConsultaChave,
  setCancelProtocolo,
  setCancelJustificativa,
}: FiscalViewProps) {
  const activeProdutos = produtos.filter((produto) => produto.ativo);
  const activeClientes = clientes.filter((cliente) => cliente.ativo);

  return (
    <main className="page-grid">
      {activeModule === "emissao" && (
        <>
          <section className="card span-5">
        <div className="section-heading">
          <div>
            <span className="eyebrow">SEFAZ</span>
            <h2>Status do webservice</h2>
          </div>
          <button
            className="ghost-button"
            disabled={!selectedCompany || fiscalBusy === "status"}
            onClick={onStatus}
            type="button"
          >
            {fiscalBusy === "status" ? "Consultando..." : "Consultar status"}
          </button>
        </div>

        {sefazStatus ? (
          <div className="status-card">
            <strong>{sefazStatus.motivo}</strong>
            <span>
              cStat {sefazStatus.cStat} - {sefazStatus.uf} -{" "}
              {ambienteLabel[sefazStatus.ambiente]}
            </span>
            <small>{sefazStatus.versaoAplicacao}</small>
          </div>
        ) : (
          <p className="muted">
            Use esta checagem antes de emitir, principalmente em producao.
          </p>
        )}
          </section>

          <section className="card span-7">
            <div className="section-heading">
              <div>
                <span className="eyebrow">Guard rails</span>
                <h2>Validacoes locais</h2>
              </div>
            </div>
            {fiscalWarnings.length > 0 ? (
              <ul className="warning-list">
                {fiscalWarnings.map((warning) => (
                  <li key={warning}>{warning}</li>
                ))}
              </ul>
            ) : (
              <p className="muted">
                Sem alertas locais. A validacao final ainda e feita pelo backend e
                pela SEFAZ.
              </p>
            )}
          </section>

          <section className="card span-12">
        <div className="section-heading">
          <div>
            <span className="eyebrow">Fluxo recomendado</span>
            <h2>Montar venda e emitir</h2>
          </div>
        </div>

        <div className="form-grid">
          <TextField
            label="Venda ID"
            type="number"
            value={emissionForm.vendaId}
            onChange={(value) =>
              setEmissionForm((form) => ({ ...form, vendaId: value }))
            }
            required
            disabled={!selectedCompany}
          />
          <TextField
            label="Numero da nota"
            value={emissionForm.numeroNota}
            onChange={(value) =>
              setEmissionForm((form) => ({ ...form, numeroNota: value }))
            }
            helper="Se ficar vazio, usa o Venda ID."
            disabled={!selectedCompany}
          />
          <TextField
            label="Data/hora"
            type="datetime-local"
            value={emissionForm.dataHora}
            onChange={(value) =>
              setEmissionForm((form) => ({ ...form, dataHora: value }))
            }
            required
            disabled={!selectedCompany}
          />
          <TextField
            label="Desconto"
            type="number"
            step="0.01"
            min="0"
            value={emissionForm.desconto}
            onChange={(value) =>
              setEmissionForm((form) => ({ ...form, desconto: value }))
            }
            disabled={!selectedCompany}
          />
          <SelectField
            label="Pagamento"
            value={emissionForm.formaPagamento}
            onChange={(value) =>
              setEmissionForm((form) => ({
                ...form,
                formaPagamento: value as FormaPagamento,
              }))
            }
            options={paymentOptions}
            disabled={!selectedCompany}
          />
          <TextField
            className="span-2"
            label="Observacao"
            value={emissionForm.observacao}
            onChange={(value) =>
              setEmissionForm((form) => ({ ...form, observacao: value }))
            }
            helper="Campo livre enviado no payload (infCpl)."
            disabled={!selectedCompany}
          />
          <SelectField
            className="span-2"
            label="Cliente cadastrado"
            value={emissionForm.clienteEmpresaId}
            onChange={(value) =>
              setEmissionForm((form) => ({
                ...form,
                clienteEmpresaId: value,
                useDestinatario: value ? false : form.useDestinatario,
              }))
            }
            options={[
              { value: "", label: "Sem cliente cadastrado" },
              ...activeClientes.map((cliente) => ({
                value: String(cliente.id),
                label: `${cliente.nome} - ${cliente.cpfCnpj} - ${cliente.uf}`,
              })),
            ]}
            disabled={!selectedCompany}
          />
          <TextField
            label="CPF/CNPJ consumidor"
            value={emissionForm.cpfCnpjConsumidor}
            onChange={(value) =>
              setEmissionForm((form) => ({ ...form, cpfCnpjConsumidor: value }))
            }
            helper="Mais comum para NFC-e sem cadastro."
            disabled={!selectedCompany || Boolean(emissionForm.clienteEmpresaId)}
          />
          <ToggleField
            label="Usar destinatario avulso"
            checked={emissionForm.useDestinatario}
            onChange={(checked) =>
              setEmissionForm((form) => ({
                ...form,
                useDestinatario: checked,
                clienteEmpresaId: checked ? "" : form.clienteEmpresaId,
              }))
            }
            disabled={!selectedCompany || Boolean(emissionForm.clienteEmpresaId)}
          />
        </div>

        {emissionForm.useDestinatario && (
          <div className="nested-panel">
            <div className="section-heading slim">
              <div>
                <span className="eyebrow">Payload direto</span>
                <h3>Destinatario avulso</h3>
              </div>
            </div>
            <DestinatarioFields
              form={emissionForm.destinatario}
              disabled={!selectedCompany}
              onChange={(destinatario) =>
                setEmissionForm((form) => ({ ...form, destinatario }))
              }
            />
          </div>
        )}

        <div className="items-panel">
          <div className="section-heading slim">
            <div>
              <span className="eyebrow">Itens</span>
              <h3>Produtos da venda</h3>
            </div>
            <button
              className="ghost-button"
              onClick={addEmissionItem}
              type="button"
              disabled={!selectedCompany}
            >
              Adicionar item
            </button>
          </div>

          <div className="item-list">
            {emissionItems.map((item, index) => (
              <article className="item-row" key={`${item.itemId}-${index}`}>
                <TextField
                  label="Item"
                  type="number"
                  value={item.itemId}
                  onChange={(value) => updateEmissionItem(index, { itemId: value })}
                  disabled={!selectedCompany}
                />
                <SelectField
                  className="item-product"
                  label="Produto"
                  value={item.produtoEmpresaId}
                  onChange={(value) => updateEmissionItemProduct(index, value)}
                  options={[
                    { value: "", label: "Selecione um produto" },
                    ...activeProdutos.map((produto) => ({
                      value: String(produto.id),
                      label: `${produto.descricao} - CFOP ${produto.cfop}`,
                    })),
                  ]}
                  disabled={!selectedCompany}
                />
                <TextField
                  label="Qtd."
                  type="number"
                  min="0.001"
                  step="0.001"
                  value={item.quantidade}
                  onChange={(value) =>
                    updateEmissionItem(index, { quantidade: value })
                  }
                  disabled={!selectedCompany}
                />
                <TextField
                  label="Preco"
                  type="number"
                  min="0"
                  step="0.01"
                  value={item.precoUnitario}
                  onChange={(value) =>
                    updateEmissionItem(index, { precoUnitario: value })
                  }
                  disabled={!selectedCompany}
                />
                <button
                  className="ghost-button compact remove-button"
                  onClick={() => removeEmissionItem(index)}
                  type="button"
                  disabled={!selectedCompany}
                >
                  Remover
                </button>
              </article>
            ))}
          </div>
        </div>

        <div className="action-row">
          <button
            className="secondary-button"
            disabled={!selectedCompany || fiscalBusy === "preview"}
            onClick={onPreview}
            type="button"
          >
            {fiscalBusy === "preview" ? "Gerando..." : "Gerar preview XML"}
          </button>
          <button
            className="primary-button"
            disabled={!selectedCompany || fiscalBusy === "emitir"}
            onClick={onEmitir}
            type="button"
          >
            {fiscalBusy === "emitir" ? "Emitindo..." : "Emitir nota"}
          </button>
        </div>
          </section>
        </>
      )}

      {activeModule === "consulta" && (
        <section className="card span-5">
        <div className="section-heading">
          <div>
            <span className="eyebrow">Consulta</span>
            <h2>Consultar ou cancelar</h2>
          </div>
        </div>

        <div className="stacked-form">
          <TextField
            label="Chave de acesso"
            value={consultaChave}
            onChange={setConsultaChave}
            disabled={!selectedCompany}
          />
          <button
            className="secondary-button"
            disabled={!selectedCompany || fiscalBusy === "consultar"}
            onClick={onConsultar}
            type="button"
          >
            {fiscalBusy === "consultar" ? "Consultando..." : "Consultar nota"}
          </button>
          <TextField
            label="Protocolo autorizacao"
            value={cancelProtocolo}
            onChange={setCancelProtocolo}
            disabled={!selectedCompany}
          />
          <TextAreaField
            label="Justificativa cancelamento"
            value={cancelJustificativa}
            onChange={setCancelJustificativa}
            minLength={15}
            maxLength={255}
            disabled={!selectedCompany}
          />
          <button
            className="danger-button"
            disabled={!selectedCompany || fiscalBusy === "cancelar"}
            onClick={onCancelar}
            type="button"
          >
            {fiscalBusy === "cancelar" ? "Cancelando..." : "Cancelar nota"}
          </button>
        </div>
        </section>
      )}

      {activeModule === "notas" && (
        <section className="card span-12">
          <div className="section-heading">
            <div>
              <span className="eyebrow">Historico fiscal</span>
              <h2>Notas ja emitidas</h2>
            </div>
            <button
              className="ghost-button"
              disabled={!selectedCompany || isLoadingNotas}
              onClick={onRefreshNotas}
              type="button"
            >
              {isLoadingNotas ? "Atualizando..." : "Atualizar notas"}
            </button>
          </div>

          {isLoadingNotas && notasPageData.items.length === 0 ? (
            <p className="muted">Carregando notas...</p>
          ) : (
            <NotasTable notas={notasPageData.items} onUseNota={onUseNota} />
          )}
          <PaginationBar
            pageInfo={notasPageData}
            isLoading={isLoadingNotas}
            onPageChange={onNotasPageChange}
          />
        </section>
      )}

      {activeModule !== "notas" && (
        <section
          className={`card ${
            activeModule === "consulta" ? "span-7" : "span-12"
          }`}
        >
          <div className="section-heading">
            <div>
              <span className="eyebrow">Resposta fiscal</span>
              <h2>Resultado da operacao</h2>
            </div>
          </div>

          {fiscalResult ? (
            <FiscalResultCard result={fiscalResult} />
          ) : (
            <p className="muted">
              O retorno do preview, emissao, consulta ou cancelamento aparece
              aqui.
            </p>
          )}
        </section>
      )}
    </main>
  );
}

function DestinatarioFields({
  form,
  disabled,
  onChange,
}: {
  form: DestinatarioForm;
  disabled?: boolean;
  onChange: (form: DestinatarioForm) => void;
}) {
  return (
    <div className="form-grid">
      <TextField
        label="Nome"
        value={form.nome}
        onChange={(value) => onChange({ ...form, nome: value })}
        required
        disabled={disabled}
      />
      <TextField
        label="CPF/CNPJ"
        value={form.cpfCnpj}
        onChange={(value) => onChange({ ...form, cpfCnpj: value })}
        required
        disabled={disabled}
      />
      <TextField
        label="Inscricao estadual"
        value={form.inscricaoEstadual}
        onChange={(value) => onChange({ ...form, inscricaoEstadual: value })}
        disabled={disabled}
      />
      <TextField
        label="CEP"
        value={form.cep}
        onChange={(value) => onChange({ ...form, cep: value })}
        required
        disabled={disabled}
      />
      <TextField
        label="Logradouro"
        value={form.logradouro}
        onChange={(value) => onChange({ ...form, logradouro: value })}
        required
        disabled={disabled}
      />
      <TextField
        label="Numero"
        value={form.numero}
        onChange={(value) => onChange({ ...form, numero: value })}
        required
        disabled={disabled}
      />
      <TextField
        label="Bairro"
        value={form.bairro}
        onChange={(value) => onChange({ ...form, bairro: value })}
        required
        disabled={disabled}
      />
      <TextField
        label="Cod. municipio"
        value={form.codigoMunicipio}
        onChange={(value) => onChange({ ...form, codigoMunicipio: value })}
        required
        disabled={disabled}
      />
      <TextField
        label="Municipio"
        value={form.descricaoMunicipio}
        onChange={(value) => onChange({ ...form, descricaoMunicipio: value })}
        required
        disabled={disabled}
      />
      <TextField
        label="UF"
        value={form.uf}
        onChange={(value) =>
          onChange({ ...form, uf: value.toUpperCase().slice(0, 2) })
        }
        required
        disabled={disabled}
      />
      <TextField
        label="E-mail"
        type="email"
        value={form.email}
        onChange={(value) => onChange({ ...form, email: value })}
        disabled={disabled}
      />
    </div>
  );
}

function FiscalResultCard({ result }: { result: FiscalResponse }) {
  return (
    <div className="result-card">
      <div className="result-grid">
        <ResultItem label="Empresa" value={result.empresaNome} />
        <ResultItem label="Modelo" value={fiscalModelLabel[result.modelo]} />
        <ResultItem label="cStat" value={result.cStat ?? "Preview"} />
        <ResultItem label="Motivo" value={result.motivo} />
        <ResultItem label="Chave" value={result.chaveAcesso ?? "Nao gerada"} />
        <ResultItem
          label="Protocolo"
          value={result.protocoloAutorizacao ?? "Nao informado"}
        />
      </div>

      <div className="download-row">
        {result.xmlDownloadUrl && (
          <a
            className="secondary-button link-button"
            href={absoluteApiUrl(result.xmlDownloadUrl)}
            target="_blank"
            rel="noreferrer"
          >
            Baixar XML
          </a>
        )}
        {result.pdfDownloadUrl && (
          <a
            className="secondary-button link-button"
            href={absoluteApiUrl(result.pdfDownloadUrl)}
            target="_blank"
            rel="noreferrer"
          >
            Baixar PDF
          </a>
        )}
        {result.qrCodeUrl && (
          <a
            className="ghost-button link-button"
            href={result.qrCodeUrl}
            target="_blank"
            rel="noreferrer"
          >
            Abrir QR Code
          </a>
        )}
      </div>

      {result.xml && (
        <TextAreaField
          label="XML"
          value={result.xml}
          onChange={() => undefined}
          readOnly
          rows={12}
        />
      )}
    </div>
  );
}

function NotasTable({
  notas,
  onUseNota,
}: {
  notas: NotaFiscal[];
  onUseNota: (nota: NotaFiscal) => void;
}) {
  if (notas.length === 0) {
    return <p className="muted">Nenhuma nota emitida encontrada.</p>;
  }

  return (
    <div className="table-wrap">
      <table className="notes-table">
        <thead>
          <tr>
            <th>Nota</th>
            <th>Destinatario</th>
            <th>Situacao</th>
            <th>Usuario</th>
            <th>Valor</th>
            <th>Emissao</th>
            <th>Chave</th>
            <th>Documentos</th>
          </tr>
        </thead>
        <tbody>
          {notas.map((nota) => (
            <tr key={nota.id}>
              <td>
                <strong>{nota.numeroNota ?? `#${nota.id}`}</strong>
                <span>
                  Serie {nota.serie ?? "-"} - {fiscalModelLabel[nota.modelo]}
                </span>
              </td>
              <td>
                <strong>{nota.destinatarioNome ?? "Nao informado"}</strong>
                <span>{nota.destinatarioDocumento ?? "Documento nao informado"}</span>
              </td>
              <td>
                <SituationPill situacao={nota.situacao} />
                {nota.cStat !== null && <span>cStat {nota.cStat}</span>}
              </td>
              <td>
                <strong>{nota.usuarioEmissorNome ?? "Nao registrado"}</strong>
                <span>{nota.usuarioEmissorLogin ?? "Sem login"}</span>
              </td>
              <td>{formatMoney(nota.valorTotal)}</td>
              <td>{formatDateTime(nota.dataEmissao ?? nota.createdAt)}</td>
              <td className="mono-cell">{nota.chaveAcesso ?? "Sem chave"}</td>
              <td>
                <div className="table-actions">
                  {nota.xmlDownloadUrl && (
                    <a
                      className="secondary-button compact link-button"
                      href={absoluteApiUrl(nota.xmlDownloadUrl)}
                      target="_blank"
                      rel="noreferrer"
                    >
                      XML
                    </a>
                  )}
                  {nota.pdfDownloadUrl && (
                    <a
                      className="secondary-button compact link-button"
                      href={absoluteApiUrl(nota.pdfDownloadUrl)}
                      target="_blank"
                      rel="noreferrer"
                    >
                      PDF
                    </a>
                  )}
                  <button
                    className="ghost-button compact"
                    disabled={!nota.chaveAcesso}
                    onClick={() => onUseNota(nota)}
                    type="button"
                  >
                    Consultar
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function CompanyTable({
  empresas,
  onEditCompany,
}: {
  empresas: Empresa[];
  onEditCompany: (empresa: Empresa) => void;
}) {
  if (empresas.length === 0) {
    return <p className="muted">Nenhuma empresa cadastrada ainda.</p>;
  }

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Empresa</th>
            <th>CNPJ</th>
            <th>Modelo</th>
            <th>Ambiente</th>
            <th>UF</th>
            <th>Status</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {empresas.map((empresa) => (
            <tr key={empresa.id}>
              <td>
                <strong>{empresa.nome}</strong>
                <span>{empresa.razaoSocial}</span>
              </td>
              <td>{empresa.cnpj}</td>
              <td>{fiscalModelLabel[empresa.modelo]}</td>
              <td>{ambienteLabel[empresa.ambiente]}</td>
              <td>{empresa.uf}</td>
              <td>
                <StatusPill active={empresa.ativo} />
              </td>
              <td>
                <button
                  className="ghost-button compact"
                  onClick={() => onEditCompany(empresa)}
                  type="button"
                >
                  Editar
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function PaginationBar({
  pageInfo,
  isLoading,
  onPageChange,
}: {
  pageInfo: PageResponse<unknown>;
  isLoading: boolean;
  onPageChange: (page: number) => void;
}) {
  const hasPages = pageInfo.totalPages > 0;

  return (
    <div className="pagination-bar">
      <span>
        {pageInfo.totalItems} registros
        {hasPages
          ? ` - pagina ${pageInfo.page + 1} de ${pageInfo.totalPages}`
          : " - sem paginas"}
      </span>
      <div>
        <button
          className="ghost-button compact"
          disabled={isLoading || !pageInfo.hasPrevious}
          onClick={() => onPageChange(Math.max(pageInfo.page - 1, 0))}
          type="button"
        >
          Anterior
        </button>
        <button
          className="ghost-button compact"
          disabled={isLoading || !pageInfo.hasNext}
          onClick={() => onPageChange(pageInfo.page + 1)}
          type="button"
        >
          Proxima
        </button>
      </div>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: number }) {
  return (
    <div className="metric">
      <strong>{value}</strong>
      <span>{label}</span>
    </div>
  );
}

function SituationPill({ situacao }: { situacao: string | null }) {
  const normalized = (situacao ?? "").toUpperCase();
  const tone = normalized.includes("AUTORIZ")
    ? "active"
    : normalized.includes("CANCEL")
      ? "inactive"
      : "neutral";

  return (
    <span className={`status-pill ${tone}`}>
      {formatSituationLabel(situacao ?? "Sem situacao")}
    </span>
  );
}

function StatusPill({ active }: { active: boolean }) {
  return (
    <span className={`status-pill ${active ? "active" : "inactive"}`}>
      {active ? "Ativo" : "Inativo"}
    </span>
  );
}

function Badge({ children }: { children: ReactNode }) {
  return <span className="badge">{children}</span>;
}

function RecordList({
  children,
  emptyLabel,
}: {
  children: ReactNode;
  emptyLabel: string;
}) {
  const childCount = Array.isArray(children)
    ? children.filter(Boolean).length
    : Number(Boolean(children));

  return (
    <div className="record-list">
      {childCount > 0 ? children : <p className="muted">{emptyLabel}</p>}
    </div>
  );
}

function ResultItem({
  label,
  value,
}: {
  label: string;
  value: string | number;
}) {
  return (
    <div className="result-item">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

interface TextFieldProps {
  label: string;
  value: string;
  onChange: (value: string) => void;
  autoComplete?: string;
  className?: string;
  type?: string;
  placeholder?: string;
  helper?: string;
  required?: boolean;
  disabled?: boolean;
  readOnly?: boolean;
  min?: string;
  max?: string;
  step?: string;
  minLength?: number;
  maxLength?: number;
}

function TextField({
  label,
  value,
  onChange,
  autoComplete,
  className = "",
  type = "text",
  placeholder,
  helper,
  required,
  disabled,
  readOnly,
  min,
  max,
  step,
  minLength,
  maxLength,
}: TextFieldProps) {
  return (
    <label className={`field ${className}`}>
      <span>{label}</span>
      <input
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        autoComplete={autoComplete}
        placeholder={placeholder}
        required={required}
        disabled={disabled}
        readOnly={readOnly}
        min={min}
        max={max}
        step={step}
        minLength={minLength}
        maxLength={maxLength}
      />
      {helper && <small>{helper}</small>}
    </label>
  );
}

interface SelectFieldProps {
  label: string;
  value: string;
  options: Array<{ value: string; label: string; disabled?: boolean }>;
  onChange: (value: string) => void;
  className?: string;
  disabled?: boolean;
}

function SelectField({
  label,
  value,
  options,
  onChange,
  className = "",
  disabled,
}: SelectFieldProps) {
  return (
    <label className={`field ${className}`}>
      <span>{label}</span>
      <select
        value={value}
        onChange={(event) => onChange(event.target.value)}
        disabled={disabled}
      >
        {options.map((option) => (
          <option
            key={`${option.value}-${option.label}`}
            value={option.value}
            disabled={option.disabled}
          >
            {option.label}
          </option>
        ))}
      </select>
    </label>
  );
}

function TextAreaField({
  label,
  value,
  onChange,
  rows = 4,
  minLength,
  maxLength,
  disabled,
  readOnly,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  rows?: number;
  minLength?: number;
  maxLength?: number;
  disabled?: boolean;
  readOnly?: boolean;
}) {
  return (
    <label className="field">
      <span>{label}</span>
      <textarea
        value={value}
        rows={rows}
        onChange={(event) => onChange(event.target.value)}
        minLength={minLength}
        maxLength={maxLength}
        disabled={disabled}
        readOnly={readOnly}
      />
    </label>
  );
}

function ToggleField({
  label,
  checked,
  onChange,
  disabled,
}: {
  label: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
  disabled?: boolean;
}) {
  return (
    <label className="toggle-field">
      <input
        type="checkbox"
        checked={checked}
        onChange={(event) => onChange(event.target.checked)}
        disabled={disabled}
      />
      <span>{label}</span>
    </label>
  );
}

function createEmptyPage<T>(): PageResponse<T> {
  return {
    items: [],
    page: 0,
    size: DEFAULT_PAGE_SIZE,
    totalItems: 0,
    totalPages: 0,
    hasNext: false,
    hasPrevious: false,
  };
}

function createEmptyCompanyForm(): CompanyForm {
  return {
    nome: "",
    cnpj: "",
    razaoSocial: "",
    nomeFantasia: "",
    inscricaoEstadual: "",
    serie: "1",
    naturezaOperacao: "VENDA DE MERCADORIA",
    cep: "",
    logradouro: "",
    numero: "",
    bairro: "",
    codigoMunicipio: "",
    descricaoMunicipio: "",
    uf: "GO",
    modelo: "NFE",
    ambiente: "HOMOLOGACAO",
    certificadoPfxPath: "",
    certificadoSenha: "",
    cadeiaJksPath: "",
    cadeiaSenha: "",
    csc: "",
    cscId: "0",
    ativo: true,
  };
}

function createEmptyProductForm(): ProductForm {
  return {
    descricao: "",
    codigoBarras: "SEM GTIN",
    ncm: "69041000",
    cest: "",
    origem: "NACIONAL",
    cfop: "5102",
    unidadeComercial: "UN",
    precoUnitarioSugerido: "0",
    ativo: true,
  };
}

function createEmptyClientForm(empresa?: Empresa): ClientForm {
  return {
    nome: "",
    cpfCnpj: "",
    inscricaoEstadual: "",
    cep: empresa?.cep ?? "",
    logradouro: "",
    numero: "",
    bairro: "",
    codigoMunicipio: empresa?.codigoMunicipio ?? "",
    descricaoMunicipio: empresa?.descricaoMunicipio ?? "",
    uf: empresa?.uf ?? "GO",
    email: "",
    ativo: true,
  };
}

function createEmptyDestinatarioForm(): DestinatarioForm {
  return {
    nome: "",
    cpfCnpj: "",
    inscricaoEstadual: "",
    cep: "",
    logradouro: "",
    numero: "",
    bairro: "",
    codigoMunicipio: "",
    descricaoMunicipio: "",
    uf: "GO",
    email: "",
  };
}

function createEmptyEmissionForm(): EmissionForm {
  const vendaId = String(Math.floor(Date.now() / 1000)).slice(-7);

  return {
    vendaId,
    numeroNota: vendaId,
    dataHora: toDateTimeLocalValue(new Date()),
    desconto: "0",
    formaPagamento: "DINHEIRO",
    observacao: "",
    clienteEmpresaId: "",
    cpfCnpjConsumidor: "",
    useDestinatario: false,
    destinatario: createEmptyDestinatarioForm(),
  };
}

function createEmptyEmissionItem(itemId: number): EmissionItemForm {
  return {
    itemId: String(itemId),
    produtoEmpresaId: "",
    quantidade: "1",
    precoUnitario: "0",
  };
}

function buildCompanyPayload(form: CompanyForm): EmpresaPayload {
  const csc = form.modelo === "NFCE" ? nullableText(form.csc) : null;
  const cscId = form.modelo === "NFCE" ? toNumber(form.cscId, 0) : 0;

  if (form.modelo === "NFCE" && !csc) {
    throw new Error("Para NFC-e, informe o CSC.");
  }

  if (form.modelo === "NFCE" && cscId <= 0) {
    throw new Error("Para NFC-e, informe um CSC ID maior que zero.");
  }

  const payload: EmpresaPayload = {
    nome: form.nome.trim(),
    cnpj: form.cnpj.trim(),
    razaoSocial: form.razaoSocial.trim(),
    nomeFantasia: nullableText(form.nomeFantasia),
    inscricaoEstadual: nullableText(form.inscricaoEstadual),
    serie: form.serie.trim(),
    naturezaOperacao: form.naturezaOperacao.trim(),
    cep: form.cep.trim(),
    logradouro: form.logradouro.trim(),
    numero: form.numero.trim(),
    bairro: form.bairro.trim(),
    codigoMunicipio: form.codigoMunicipio.trim(),
    descricaoMunicipio: form.descricaoMunicipio.trim(),
    uf: form.uf.trim().toUpperCase(),
    modelo: form.modelo,
    ambiente: form.ambiente,
    certificadoPfxPath: form.certificadoPfxPath.trim(),
    cadeiaJksPath: form.cadeiaJksPath.trim(),
    csc,
    cscId,
    ativo: form.ativo,
  };

  if (form.certificadoSenha.trim()) {
    payload.certificadoSenha = form.certificadoSenha.trim();
  }

  if (form.cadeiaSenha.trim()) {
    payload.cadeiaSenha = form.cadeiaSenha.trim();
  }

  return payload;
}

function buildProductPayload(form: ProductForm): ProdutoPayload {
  return {
    descricao: form.descricao.trim(),
    codigoBarras: nullableText(form.codigoBarras),
    ncm: form.ncm.trim(),
    cest: nullableText(form.cest),
    origem: form.origem.trim(),
    cfop: form.cfop.trim(),
    unidadeComercial: form.unidadeComercial.trim().toUpperCase(),
    precoUnitarioSugerido: toNumber(form.precoUnitarioSugerido, 0),
    ativo: form.ativo,
  };
}

function buildClientPayload(form: ClientForm): ClientePayload {
  return {
    nome: form.nome.trim(),
    cpfCnpj: form.cpfCnpj.trim(),
    inscricaoEstadual: nullableText(form.inscricaoEstadual),
    cep: form.cep.trim(),
    logradouro: form.logradouro.trim(),
    numero: form.numero.trim(),
    bairro: form.bairro.trim(),
    codigoMunicipio: form.codigoMunicipio.trim(),
    descricaoMunicipio: form.descricaoMunicipio.trim(),
    uf: form.uf.trim().toUpperCase(),
    email: nullableText(form.email),
    ativo: form.ativo,
  };
}

function buildEmissionPayload(
  empresa: Empresa,
  form: EmissionForm,
  itemForms: EmissionItemForm[],
): EmissaoPayload {
  const vendaId = toNumber(form.vendaId, 0);
  if (!vendaId) {
    throw new Error("Informe um Venda ID numerico.");
  }

  const itens = itemForms
    .filter((item) => item.produtoEmpresaId)
    .map((item, index) => ({
      itemId: toNumber(item.itemId, index + 1),
      produtoEmpresaId: toNumber(item.produtoEmpresaId, 0),
      quantidade: toNumber(item.quantidade, 0),
      precoUnitario: toNumber(item.precoUnitario, 0),
    }));

  if (itens.length === 0) {
    throw new Error("Adicione pelo menos um item com produto cadastrado.");
  }

  if (itens.some((item) => item.quantidade <= 0 || item.precoUnitario < 0)) {
    throw new Error("Quantidade precisa ser maior que zero e preco nao pode ser negativo.");
  }

  const payload: EmissaoPayload = {
    empresaId: empresa.id,
    vendaId,
    numeroNota: form.numeroNota.trim() || String(vendaId),
    dataHora: normalizeDateTime(form.dataHora),
    desconto: toNumber(form.desconto, 0),
    formaPagamento: form.formaPagamento,
    itens,
  };

  const observacao = form.observacao.trim();
  if (observacao) {
    payload.observacao = observacao;
  }

  const clienteEmpresaId = toNumber(form.clienteEmpresaId, 0);

  if (clienteEmpresaId) {
    payload.clienteEmpresaId = clienteEmpresaId;
  } else if (form.useDestinatario) {
    payload.destinatario = buildDestinatarioPayload(form.destinatario);
  } else if (form.cpfCnpjConsumidor.trim()) {
    payload.cpfCnpjConsumidor = form.cpfCnpjConsumidor.trim();
  }

  if (empresa.modelo === "NFE" && !payload.clienteEmpresaId && !payload.destinatario) {
    throw new Error("Para NF-e, selecione um cliente ou preencha destinatario avulso.");
  }

  return payload;
}

function buildDestinatarioPayload(form: DestinatarioForm): DestinatarioPayload {
  const requiredFields: Array<[keyof DestinatarioForm, string]> = [
    ["nome", "nome"],
    ["cpfCnpj", "CPF/CNPJ"],
    ["cep", "CEP"],
    ["logradouro", "logradouro"],
    ["numero", "numero"],
    ["bairro", "bairro"],
    ["codigoMunicipio", "codigo do municipio"],
    ["descricaoMunicipio", "municipio"],
    ["uf", "UF"],
  ];

  const missingField = requiredFields.find(([field]) => !form[field].trim());
  if (missingField) {
    throw new Error(`Destinatario avulso precisa informar ${missingField[1]}.`);
  }

  return {
    nome: form.nome.trim(),
    cpfCnpj: form.cpfCnpj.trim(),
    inscricaoEstadual: nullableText(form.inscricaoEstadual),
    cep: form.cep.trim(),
    logradouro: form.logradouro.trim(),
    numero: form.numero.trim(),
    bairro: form.bairro.trim(),
    codigoMunicipio: form.codigoMunicipio.trim(),
    descricaoMunicipio: form.descricaoMunicipio.trim(),
    uf: form.uf.trim().toUpperCase(),
    email: nullableText(form.email),
  };
}

function buildFiscalWarnings(
  empresa: Empresa | null,
  clientes: ClienteEmpresa[],
  produtos: ProdutoEmpresa[],
  form: EmissionForm,
  itemForms: EmissionItemForm[],
): string[] {
  const warnings: string[] = [];

  if (!empresa) {
    return ["Selecione uma empresa para habilitar as operacoes fiscais."];
  }

  if (!empresa.ativo) {
    warnings.push("A empresa selecionada esta inativa.");
  }

  if (empresa.modelo === "NFE" && !form.clienteEmpresaId && !form.useDestinatario) {
    warnings.push("NF-e deve usar cliente cadastrado ou destinatario avulso completo.");
  }

  const selectedCliente = clientes.find(
    (cliente) => String(cliente.id) === form.clienteEmpresaId,
  );
  const destinationUf = selectedCliente?.uf || (form.useDestinatario ? form.destinatario.uf : "");

  if (destinationUf) {
    const targetUf = destinationUf.toUpperCase();
    const selectedProdutos = itemForms
      .map((item) => produtos.find((produto) => String(produto.id) === item.produtoEmpresaId))
      .filter((produto): produto is ProdutoEmpresa => Boolean(produto));

    for (const produto of selectedProdutos) {
      if (targetUf === empresa.uf && produto.cfop.startsWith("6")) {
        warnings.push(
          `${produto.descricao}: CFOP ${produto.cfop} parece interestadual, mas o cliente esta na mesma UF.`,
        );
      }

      if (targetUf !== empresa.uf && produto.cfop.startsWith("5")) {
        warnings.push(
          `${produto.descricao}: CFOP ${produto.cfop} parece interno, mas o cliente esta em outra UF.`,
        );
      }
    }
  }

  if (produtos.every((produto) => !produto.ativo)) {
    warnings.push("Cadastre pelo menos um produto ativo antes de emitir.");
  }

  return Array.from(new Set(warnings));
}

function normalizeCurrentPage<T>(
  data: PageResponse<T>,
  setPage: Dispatch<SetStateAction<number>>,
) {
  if (data.totalPages > 0 && data.page >= data.totalPages) {
    setPage(Math.max(data.totalPages - 1, 0));
  }
}

function nullableText(value: string): string | null {
  const trimmed = value.trim();
  return trimmed ? trimmed : null;
}

function toNumber(value: string, fallback: number): number {
  const normalized = value.replace(",", ".").trim();
  const parsed = Number(normalized);
  return Number.isFinite(parsed) ? parsed : fallback;
}

function normalizeDateTime(value: string): string {
  if (!value) {
    return toDateTimeLocalValue(new Date()) + ":00";
  }

  return value.length === 16 ? `${value}:00` : value;
}

function toDateTimeLocalValue(date: Date): string {
  const timezoneOffset = date.getTimezoneOffset() * 60000;
  return new Date(date.getTime() - timezoneOffset).toISOString().slice(0, 16);
}

function formatMoney(value: number | null | undefined): string {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(value ?? 0);
}

function formatDateTime(value: string | null | undefined): string {
  if (!value) {
    return "Sem data";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(date);
}

function formatSituationLabel(value: string): string {
  return value
    .toLowerCase()
    .replace(/_/g, " ")
    .replace(/(^|\s)\S/g, (letter) => letter.toUpperCase());
}

function getErrorMessage(error: unknown): string {
  if (error instanceof ApiRequestError) {
    return `${error.status}: ${error.message}`;
  }

  if (error instanceof Error) {
    return error.message;
  }

  return "Ocorreu um erro inesperado.";
}

export default App;
