import type {
  Ambiente,
  ApiErrorPayload,
  AuthUser,
  BrasilApiCnpjResponse,
  CancelamentoPayload,
  ClienteEmpresa,
  ClientePayload,
  ConsultaPayload,
  EmissaoPayload,
  Empresa,
  EmpresaPayload,
  FiscalModel,
  FiscalResponse,
  FiscalStatus,
  LoginPayload,
  LoginResponse,
  NotaFiscal,
  PageResponse,
  ProdutoEmpresa,
  ProdutoPayload,
  ViaCepResponse,
} from "./types";

const envApiBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim().replace(/\/$/, "");

export const API_BASE_URL = envApiBaseUrl ?? "";

export const API_BASE_LABEL = API_BASE_URL || "mesma origem";

export class ApiRequestError extends Error {
  public readonly status: number;
  public readonly payload?: ApiErrorPayload;

  constructor(status: number, message: string, payload?: ApiErrorPayload) {
    super(message);
    this.name = "ApiRequestError";
    this.status = status;
    this.payload = payload;
  }
}

type RequestOptions = Omit<RequestInit, "body"> & {
  body?: unknown;
};

interface PageParams {
  page?: number;
  size?: number;
  nome?: string;
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers = new Headers(options.headers);

  if (options.body !== undefined) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    credentials: options.credentials ?? "include",
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  });

  const rawText = await response.text();
  const json = parseJson(rawText);

  if (!response.ok) {
    const payload = json as ApiErrorPayload | undefined;
    const message =
      payload?.message ??
      payload?.error ??
      rawText ??
      `A API respondeu com status ${response.status}.`;

    throw new ApiRequestError(response.status, message, payload);
  }

  return json as T;
}

function parseJson(value: string): unknown {
  if (!value) {
    return undefined;
  }

  try {
    return JSON.parse(value);
  } catch {
    return value;
  }
}

function withQuery(
  path: string,
  params: Record<string, string | number | null | undefined>,
): string {
  const searchParams = new URLSearchParams();

  for (const [key, value] of Object.entries(params)) {
    if (value === null || value === undefined) {
      continue;
    }

    if (typeof value === "string" && !value.trim()) {
      continue;
    }

    searchParams.set(key, String(value));
  }

  const query = searchParams.toString();
  return query ? `${path}?${query}` : path;
}

export function fiscalPrefix(modelo: FiscalModel): "/api/nfe" | "/api/nfce" {
  return modelo === "NFCE" ? "/api/nfce" : "/api/nfe";
}

export function absoluteApiUrl(pathOrUrl: string): string {
  if (/^https?:\/\//i.test(pathOrUrl)) {
    return pathOrUrl;
  }

  return `${API_BASE_URL}${pathOrUrl.startsWith("/") ? "" : "/"}${pathOrUrl}`;
}

export const api = {
  auth: {
    me: () => request<AuthUser>("/api/auth/me"),
    signin: (payload: LoginPayload) =>
      request<LoginResponse>("/api/auth/signin", {
        method: "POST",
        body: payload,
      }),
    logout: () =>
      request<{ status: boolean }>("/api/auth/logout", {
        method: "POST",
      }),
  },
  integracoes: {
    cnpj: (cnpj: string) =>
      request<BrasilApiCnpjResponse>(
        `/api/integracoes/brasilapi/cnpj?cnpj=${encodeURIComponent(cnpj)}`,
      ),
    cep: (cep: string) =>
      request<ViaCepResponse>(
        `/api/integracoes/viacep/cep?cep=${encodeURIComponent(cep)}`,
      ),
  },
  empresas: {
    list: () => request<Empresa[]>("/api/empresas"),
    create: (payload: EmpresaPayload) =>
      request<Empresa>("/api/empresas", {
        method: "POST",
        body: payload,
      }),
    update: (empresaId: number, payload: EmpresaPayload) =>
      request<Empresa>(`/api/empresas/${empresaId}`, {
        method: "PUT",
        body: payload,
      }),
  },
  produtos: {
    list: (empresaId: number, nome?: string) =>
      request<ProdutoEmpresa[]>(
        withQuery(`/api/empresas/${empresaId}/produtos`, { nome }),
      ),
    paginado: (empresaId: number, params: PageParams = {}) =>
      request<PageResponse<ProdutoEmpresa>>(
        withQuery(`/api/empresas/${empresaId}/produtos/paginado`, {
          page: params.page ?? 0,
          size: params.size ?? 20,
          nome: params.nome,
        }),
      ),
    create: (empresaId: number, payload: ProdutoPayload) =>
      request<ProdutoEmpresa>(`/api/empresas/${empresaId}/produtos`, {
        method: "POST",
        body: payload,
      }),
    update: (empresaId: number, produtoId: number, payload: ProdutoPayload) =>
      request<ProdutoEmpresa>(
        `/api/empresas/${empresaId}/produtos/${produtoId}`,
        {
          method: "PUT",
          body: payload,
        },
      ),
  },
  clientes: {
    list: (empresaId: number, nome?: string) =>
      request<ClienteEmpresa[]>(
        withQuery(`/api/empresas/${empresaId}/clientes`, { nome }),
      ),
    paginado: (empresaId: number, params: PageParams = {}) =>
      request<PageResponse<ClienteEmpresa>>(
        withQuery(`/api/empresas/${empresaId}/clientes/paginado`, {
          page: params.page ?? 0,
          size: params.size ?? 20,
          nome: params.nome,
        }),
      ),
    create: (empresaId: number, payload: ClientePayload) =>
      request<ClienteEmpresa>(`/api/empresas/${empresaId}/clientes`, {
        method: "POST",
        body: payload,
      }),
    update: (empresaId: number, clienteId: number, payload: ClientePayload) =>
      request<ClienteEmpresa>(
        `/api/empresas/${empresaId}/clientes/${clienteId}`,
        {
          method: "PUT",
          body: payload,
        },
      ),
  },
  notas: {
    list: (empresaId: number, params: PageParams = {}) =>
      request<PageResponse<NotaFiscal>>(
        withQuery(`/api/empresas/${empresaId}/notas`, {
          page: params.page ?? 0,
          size: params.size ?? 20,
        }),
      ),
  },
  fiscal: {
    status: (modelo: FiscalModel, empresaId: number) =>
      request<FiscalStatus>(
        `${fiscalPrefix(modelo)}/status?empresaId=${empresaId}`,
      ),
    xmlPreview: (modelo: FiscalModel, payload: EmissaoPayload) =>
      request<FiscalResponse>(`${fiscalPrefix(modelo)}/xml-preview`, {
        method: "POST",
        body: payload,
      }),
    emitir: (modelo: FiscalModel, payload: EmissaoPayload) =>
      request<FiscalResponse>(`${fiscalPrefix(modelo)}/emitir`, {
        method: "POST",
        body: payload,
      }),
    consultar: (modelo: FiscalModel, payload: ConsultaPayload) =>
      request<FiscalResponse>(`${fiscalPrefix(modelo)}/consultar`, {
        method: "POST",
        body: payload,
      }),
    cancelar: (modelo: FiscalModel, payload: CancelamentoPayload) =>
      request<FiscalResponse>(`${fiscalPrefix(modelo)}/cancelar`, {
        method: "POST",
        body: payload,
      }),
  },
};

export const fiscalModelLabel: Record<FiscalModel, string> = {
  NFE: "NF-e",
  NFCE: "NFC-e",
};

export const ambienteLabel: Record<Ambiente, string> = {
  HOMOLOGACAO: "Homologacao",
  PRODUCAO: "Producao",
};
