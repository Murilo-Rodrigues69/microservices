# Integracao Frontend

## Visao geral

Este backend expõe uma API REST para emissao de **NF-e** e **NFC-e** com suporte a **multiempresa**.

O frontend deve trabalhar com quatro blocos principais:

1. cadastro de empresas emissoras
2. cadastro de produtos por empresa
3. cadastro de clientes por empresa
4. operacoes fiscais de emissao, consulta, cancelamento e download de XML/PDF

Base URL local:

```text
http://localhost:8081
```

Hoje a API **nao possui autenticacao**.

## Persistencia

- os dados de empresas, produtos e clientes ficam no banco
- por padrao, o projeto usa H2 em arquivo
- reiniciar a aplicacao nao apaga os dados
- os arquivos fiscais XML/PDF ficam em disco na pasta `documentos-fiscais`

## Conceitos principais

### Empresa

Representa o emitente fiscal. Cada empresa possui:

- dados cadastrais
- configuracao fiscal (`modelo`, `ambiente`)
- certificado digital
- cadeia JKS
- CSC e CSC ID quando o modelo for `NFCE`

### Produto da empresa

Representa um produto vinculado a uma empresa emissora.

Campos mais importantes:

- `descricao`
- `ncm`
- `cest`
- `origem`
- `cfop`
- `unidadeComercial`
- `precoUnitarioSugerido`
- `ativo`

### Cliente da empresa

Representa um destinatario reaproveitavel para emissao.

Campos mais importantes:

- `nome`
- `cpfCnpj`
- `inscricaoEstadual`
- `cep`
- `logradouro`
- `numero`
- `bairro`
- `codigoMunicipio`
- `descricaoMunicipio`
- `uf`
- `email`
- `ativo`

### Emissao

Uma emissao usa:

- `empresaId`
- um ou mais itens
- cliente por cadastro (`clienteEmpresaId`) ou destinatario direto no payload
- `observacao` opcional para texto livre da nota

## Enumeracoes

### Modelo fiscal

```text
NFE
NFCE
```

### Ambiente

```text
HOMOLOGACAO
PRODUCAO
```

### Forma de pagamento

```text
DINHEIRO
PIX
CARTAO_CREDITO
CARTAO_DEBITO
```

## Regras de negocio importantes

### 1. `empresaId`

- se existir apenas uma empresa ativa, o backend consegue operar sem `empresaId`
- se existirem varias empresas ativas, `empresaId` passa a ser obrigatorio
- por padrao, o frontend deve sempre enviar `empresaId`

### 2. Prefixo das rotas fiscais

O controller aceita os dois prefixos:

- `/api/nfe`
- `/api/nfce`

Mas o frontend deve usar o prefixo coerente com o `modelo` da empresa:

- empresa `NFE` -> usar `/api/nfe`
- empresa `NFCE` -> usar `/api/nfce`

### 3. Produto no item

Cada item pode ser montado de dois jeitos:

1. com `produtoEmpresaId`
2. com objeto `produto` completo no payload

Para frontend administrativo, o recomendado e usar `produtoEmpresaId`.

Importante:

- `itemId` = identificador da linha do item
- `produtoEmpresaId` = identificador do produto cadastrado

### 4. Destinatario

O destinatario pode ser informado de tres formas:

1. `clienteEmpresaId`
2. objeto `destinatario`
3. `cpfCnpjConsumidor`

Prioridade aplicada pelo backend:

1. `clienteEmpresaId`
2. `destinatario`
3. `cpfCnpjConsumidor`

### 5. Inscricao estadual do destinatario

Se o cliente for contribuinte de ICMS, informar `inscricaoEstadual`.

Casos:

- contribuinte normal -> informar IE numerica
- contribuinte isento -> informar `ISENTO`
- nao contribuinte -> deixar `null`

### 6. CFOP x UF do cliente

O backend valida coerencia entre CFOP e destino da operacao:

- cliente da mesma UF do emitente -> CFOP `5xxx`
- cliente de outra UF -> CFOP `6xxx`
- exterior -> CFOP `7xxx`

Se o CFOP nao bater com a UF do cliente, a API devolve erro `400` antes do envio para a SEFAZ.

### 7. Exclusao

Nao existem endpoints `DELETE`.

Quando quiser "desativar" algo:

- atualizar a entidade com `ativo=false`

### 8. Busca publica de CNPJ

Existe uma integracao interna com a BrasilAPI para preencher rapidamente o cadastro da empresa a partir do CNPJ.

O frontend deve chamar o backend, nao a BrasilAPI diretamente.

## Formato padrao de erro

Erros de validacao e regra de negocio retornam:

```json
{
  "timestamp": "2026-05-07T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "descricao deve ser informado.",
  "path": "/api/empresas/1/produtos"
}
```

Status esperados:

- `400` validacao/regra de negocio
- `404` recurso nao encontrado
- `500` erro interno

## Rotas de empresas

### Listar empresas

`GET /api/empresas`

Resposta:

```json
[
  {
    "id": 1,
    "nome": "CERAMICA MASTER",
    "cnpj": "11222333000144",
    "razaoSocial": "EMPRESA DEMONSTRACAO LTDA",
    "nomeFantasia": "CERAMICA MASTER",
    "inscricaoEstadual": "201659450",
    "serie": "1",
    "naturezaOperacao": "VENDA DE MERCADORIA",
    "cep": "75460000",
    "logradouro": "AVENIDA EXEMPLO",
    "numero": "0",
    "bairro": "BAIRRO EXEMPLO",
    "codigoMunicipio": "5214507",
    "descricaoMunicipio": "NEROPOLIS",
    "uf": "GO",
    "modelo": "NFE",
    "ambiente": "HOMOLOGACAO",
    "certificadoPfxPath": "C:/certificados/empresa.pfx",
    "cadeiaJksPath": "C:/certificados/sefaz-producao.jks",
    "cscId": 0,
    "ativo": true,
    "certificadoSenhaConfigurada": true,
    "cadeiaSenhaConfigurada": true,
    "cscConfigurado": false,
    "createdAt": "2026-05-07T10:49:14.749808",
    "updatedAt": "2026-05-07T10:49:14.749808"
  }
]
```

### Buscar empresa

`GET /api/empresas/{empresaId}`

### Criar empresa

`POST /api/empresas`

Body:

```json
{
  "nome": "CERAMICA MASTER",
  "cnpj": "11222333000144",
  "razaoSocial": "EMPRESA DEMONSTRACAO LTDA",
  "nomeFantasia": "CERAMICA MASTER",
  "inscricaoEstadual": "201659450",
  "serie": "1",
  "naturezaOperacao": "VENDA DE MERCADORIA",
  "cep": "75460000",
  "logradouro": "AVENIDA EXEMPLO",
  "numero": "0",
  "bairro": "BAIRRO EXEMPLO",
  "codigoMunicipio": "5214507",
  "descricaoMunicipio": "NEROPOLIS",
  "uf": "GO",
  "modelo": "NFE",
  "ambiente": "HOMOLOGACAO",
  "certificadoPfxPath": "C:/certificados/empresa.pfx",
  "certificadoSenha": "env://NFE_CERTIFICADO_SENHA",
  "cadeiaJksPath": "C:/certificados/sefaz-producao.jks",
  "cadeiaSenha": "env://NFE_CADEIA_SENHA",
  "csc": null,
  "cscId": 0,
  "ativo": true
}
```

Observacoes:

- para `modelo = NFCE`, `csc` e `cscId` passam a ser relevantes
- para `modelo = NFE`, o backend limpa `csc` e `cscId`

### Atualizar empresa

`PUT /api/empresas/{empresaId}`

Mesma estrutura do `POST`.

Observacoes:

- em update, `certificadoSenha` e `cadeiaSenha` podem ser omitidos se ja existirem salvos
- o response nao devolve as senhas, apenas flags booleanas dizendo se estao configuradas

## Rotas de produtos

### Listar produtos da empresa

`GET /api/empresas/{empresaId}/produtos`

Filtro opcional por nome:

`GET /api/empresas/{empresaId}/produtos?nome=tijolo`

### Listar produtos da empresa com paginacao

`GET /api/empresas/{empresaId}/produtos/paginado?page=0&size=20`

Filtro opcional por nome:

`GET /api/empresas/{empresaId}/produtos/paginado?page=0&size=20&nome=tijolo`

Response exemplo:

```json
{
  "items": [
    {
      "id": 1,
      "empresaId": 1,
      "descricao": "TIJOLO 9X19X19",
      "codigoBarras": null,
      "ncm": "69041000",
      "cest": "1002700",
      "origem": "NACIONAL",
      "cfop": "5102",
      "unidadeComercial": "UN",
      "precoUnitarioSugerido": 1.25,
      "ativo": true,
      "createdAt": "2026-05-07T12:00:00",
      "updatedAt": "2026-05-07T12:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1,
  "totalPages": 1,
  "hasNext": false,
  "hasPrevious": false
}
```

### Buscar produto da empresa

`GET /api/empresas/{empresaId}/produtos/{produtoId}`

### Criar produto da empresa

`POST /api/empresas/{empresaId}/produtos`

Body:

```json
{
  "descricao": "TIJOLO 9X19X19",
  "codigoBarras": "SEM GTIN",
  "ncm": "69041000",
  "cest": "10.027.00",
  "origem": "NACIONAL",
  "cfop": "5102",
  "unidadeComercial": "UN",
  "precoUnitarioSugerido": 1.25,
  "ativo": true
}
```

### Atualizar produto da empresa

`PUT /api/empresas/{empresaId}/produtos/{produtoId}`

Mesma estrutura do `POST`.

Response exemplo:

```json
{
  "id": 1,
  "empresaId": 1,
  "descricao": "TIJOLO 9X19X19",
  "codigoBarras": null,
  "ncm": "69041000",
  "cest": "1002700",
  "origem": "NACIONAL",
  "cfop": "5102",
  "unidadeComercial": "UN",
  "precoUnitarioSugerido": 1.25,
  "ativo": true,
  "createdAt": "2026-05-07T12:00:00",
  "updatedAt": "2026-05-07T12:00:00"
}
```

## Rotas de clientes

### Listar clientes da empresa

`GET /api/empresas/{empresaId}/clientes`

Mantida para compatibilidade, retornando lista simples.

Filtro opcional por nome:

`GET /api/empresas/{empresaId}/clientes?nome=maria`

### Listar clientes da empresa com paginação

`GET /api/empresas/{empresaId}/clientes/paginado?page=0&size=20`

Filtro opcional por nome:

`GET /api/empresas/{empresaId}/clientes/paginado?page=0&size=20&nome=maria`

Response exemplo:

```json
{
  "items": [
    {
      "id": 1,
      "empresaId": 1,
      "nome": "CLIENTE CONTRIBUINTE",
      "cpfCnpj": "11222333000144",
      "inscricaoEstadual": "109876543",
      "cep": "75460000",
      "logradouro": "RUA TESTE",
      "numero": "123",
      "bairro": "CENTRO",
      "codigoMunicipio": "5214507",
      "descricaoMunicipio": "NEROPOLIS",
      "uf": "GO",
      "email": "fiscal@cliente.com",
      "ativo": true,
      "createdAt": "2026-05-07T12:00:00",
      "updatedAt": "2026-05-07T12:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1,
  "totalPages": 1,
  "hasNext": false,
  "hasPrevious": false
}
```

### Buscar cliente da empresa

`GET /api/empresas/{empresaId}/clientes/{clienteId}`

### Criar cliente da empresa

`POST /api/empresas/{empresaId}/clientes`

Body:

```json
{
  "nome": "CLIENTE CONTRIBUINTE",
  "cpfCnpj": "11222333000144",
  "inscricaoEstadual": "109876543",
  "cep": "75460000",
  "logradouro": "RUA TESTE",
  "numero": "123",
  "bairro": "CENTRO",
  "codigoMunicipio": "5214507",
  "descricaoMunicipio": "NEROPOLIS",
  "uf": "GO",
  "email": "fiscal@cliente.com",
  "ativo": true
}
```

Para cliente isento:

```json
{
  "inscricaoEstadual": "ISENTO"
}
```

### Atualizar cliente da empresa

`PUT /api/empresas/{empresaId}/clientes/{clienteId}`

Mesma estrutura do `POST`.

Response exemplo:

```json
{
  "id": 1,
  "empresaId": 1,
  "nome": "CLIENTE CONTRIBUINTE",
  "cpfCnpj": "11222333000144",
  "inscricaoEstadual": "109876543",
  "cep": "75460000",
  "logradouro": "RUA TESTE",
  "numero": "123",
  "bairro": "CENTRO",
  "codigoMunicipio": "5214507",
  "descricaoMunicipio": "NEROPOLIS",
  "uf": "GO",
  "email": "fiscal@cliente.com",
  "ativo": true,
  "createdAt": "2026-05-07T12:00:00",
  "updatedAt": "2026-05-07T12:00:00"
}
```

## Rotas de notas emitidas

### Listar notas emitidas da empresa

`GET /api/empresas/{empresaId}/notas?page=0&size=20`

Response exemplo:

```json
{
  "items": [
    {
      "id": 10,
      "empresaId": 1,
      "clienteEmpresaId": 1,
      "modelo": "NFE",
      "situacao": "AUTORIZADA",
      "numeroNota": "2001",
      "serie": "1",
      "chaveAcesso": "52260411222333000144550010000020011000020010",
      "protocoloAutorizacao": "152260000000001",
      "cStat": 100,
      "motivo": "Autorizado o uso da NF-e",
      "observacao": "Entregar no caixa lateral.",
      "qrCodeUrl": null,
      "destinatarioNome": "CLIENTE CONTRIBUINTE",
      "destinatarioDocumento": "11222333000144",
      "valorTotal": 125.00,
      "dataEmissao": "2026-05-07T12:00:00",
      "xmlDownloadUrl": "/api/nfe/documentos/52260411222333000144550010000020011000020010/xml",
      "pdfDownloadUrl": "/api/nfe/documentos/52260411222333000144550010000020011000020010/pdf",
      "createdAt": "2026-05-07T12:00:01",
      "updatedAt": "2026-05-07T12:00:01"
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1,
  "totalPages": 1,
  "hasNext": false,
  "hasPrevious": false
}
```

## Rotas fiscais

As rotas abaixo existem tanto em `/api/nfe/...` quanto em `/api/nfce/...`.

O frontend deve usar o prefixo que combina com a empresa escolhida.

### Status do webservice

`GET /api/nfe/status?empresaId=1`

Response:

```json
{
  "empresaId": 1,
  "empresaNome": "CERAMICA MASTER",
  "modelo": "NFE",
  "ambiente": "HOMOLOGACAO",
  "uf": "GO",
  "cStat": 107,
  "motivo": "Servico em Operacao",
  "versaoAplicacao": "GOx.x.x"
}
```

### Preview do XML

`POST /api/nfe/xml-preview`

Use para validar montagem do XML antes de enviar a nota para a SEFAZ.

#### Emissao usando cadastros

```json
{
  "empresaId": 1,
  "vendaId": 3001,
  "clienteEmpresaId": 1,
  "numeroNota": "3001",
  "dataHora": "2026-05-07T11:45:00",
  "desconto": 0.00,
  "formaPagamento": "DINHEIRO",
  "observacao": "Separar mercadoria no deposito 2.",
  "itens": [
    {
      "itemId": 1,
      "produtoEmpresaId": 1,
      "quantidade": 100,
      "precoUnitario": 1.25
    }
  ]
}
```

#### Emissao com produto e destinatario diretos no payload

```json
{
  "empresaId": 1,
  "vendaId": 3002,
  "numeroNota": "3002",
  "dataHora": "2026-05-07T11:50:00",
  "desconto": 0.00,
  "formaPagamento": "PIX",
  "observacao": "Cliente solicitou embalagem para presente.",
  "destinatario": {
    "nome": "CLIENTE AVULSO",
    "cpfCnpj": "11222333000144",
    "inscricaoEstadual": "109876543",
    "cep": "70000000",
    "logradouro": "RUA TESTE",
    "numero": "10",
    "bairro": "CENTRO",
    "codigoMunicipio": "5300108",
    "descricaoMunicipio": "BRASILIA",
    "uf": "DF",
    "email": "cliente@teste.com"
  },
  "itens": [
    {
      "itemId": 1,
      "quantidade": 5,
      "precoUnitario": 10.50,
      "produto": {
        "produtoId": 999,
        "descricao": "PRODUTO AVULSO",
        "codigoBarras": null,
        "ncm": "69041000",
        "cest": null,
        "origem": "NACIONAL",
        "cfop": "6102",
        "unidadeComercial": "UN"
      }
    }
  ]
}
```

Response do preview:

```json
{
  "empresaId": 1,
  "empresaNome": "CERAMICA MASTER",
  "modelo": "NFE",
  "cStat": null,
  "motivo": "XML gerado sem envio para a SEFAZ.",
  "observacao": "Separar mercadoria no deposito 2.",
  "chaveAcesso": null,
  "protocoloAutorizacao": null,
  "qrCodeUrl": null,
  "xml": "<NFe>...</NFe>",
  "xmlDownloadUrl": null,
  "pdfDownloadUrl": null,
  "xmlFilePath": null,
  "pdfFilePath": null
}
```

### Emitir nota

`POST /api/nfe/emitir`

Usa o mesmo body do preview.

Response de autorizacao:

```json
{
  "empresaId": 1,
  "empresaNome": "CERAMICA MASTER",
  "modelo": "NFE",
  "cStat": 100,
  "motivo": "Autorizado o uso da NF-e",
  "observacao": "Separar mercadoria no deposito 2.",
  "chaveAcesso": "52260511222333000144550010000040011000040010",
  "protocoloAutorizacao": "152260000000000",
  "qrCodeUrl": null,
  "xml": "<?xml version=\"1.0\" encoding=\"UTF-8\"?>...",
  "xmlDownloadUrl": "/api/nfe/documentos/52260511222333000144550010000040011000040010/xml",
  "pdfDownloadUrl": "/api/nfe/documentos/52260511222333000144550010000040011000040010/pdf",
  "xmlFilePath": "documentos-fiscais/52260511222333000144550010000040011000040010.xml",
  "pdfFilePath": "documentos-fiscais/52260511222333000144550010000040011000040010.pdf"
}
```

Observacoes:

- `cStat = 100` normalmente indica autorizacao
- quando houver rejeicao da SEFAZ, o backend devolve `motivo` com a mensagem fiscal
- em operacoes `NFCE`, `qrCodeUrl` pode vir preenchido

### Consultar nota por chave

`POST /api/nfe/consultar`

Body:

```json
{
  "empresaId": 1,
  "chaveAcesso": "52260511222333000144550010000040011000040010"
}
```

Response:

```json
{
  "empresaId": 1,
  "empresaNome": "CERAMICA MASTER",
  "modelo": "NFE",
  "cStat": 100,
  "motivo": "Autorizado o uso da NF-e",
  "observacao": "Separar mercadoria no deposito 2.",
  "chaveAcesso": "52260511222333000144550010000040011000040010",
  "protocoloAutorizacao": "152260000000000",
  "qrCodeUrl": null,
  "xml": "<?xml version=\"1.0\" encoding=\"UTF-8\"?>...",
  "xmlDownloadUrl": "/api/nfe/documentos/52260511222333000144550010000040011000040010/xml",
  "pdfDownloadUrl": "/api/nfe/documentos/52260511222333000144550010000040011000040010/pdf",
  "xmlFilePath": "documentos-fiscais/52260511222333000144550010000040011000040010.xml",
  "pdfFilePath": "documentos-fiscais/52260511222333000144550010000040011000040010.pdf"
}
```

### Cancelar nota

`POST /api/nfe/cancelar`

Body:

```json
{
  "empresaId": 1,
  "chaveAcesso": "52260511222333000144550010000040011000040010",
  "protocoloAutorizacao": "152260000000000",
  "justificativa": "Cancelamento por erro de digitacao do pedido."
}
```

Observacoes:

- `justificativa` precisa ter entre `15` e `255` caracteres

### Download do XML

`GET /api/nfe/documentos/{chaveAcesso}/xml`

Retorno:

- `Content-Type: application/xml`
- body binario do arquivo XML

### Download do PDF

`GET /api/nfe/documentos/{chaveAcesso}/pdf`

Retorno:

- `Content-Type: application/pdf`
- body binario do arquivo PDF

## Rotas de integracao

### Buscar dados publicos de empresa por CNPJ

`GET /api/integracoes/brasilapi/cnpj?cnpj=27865757000102`

Observacoes:

- aceita CNPJ com ou sem mascara
- o backend normaliza para 14 digitos
- se o CNPJ nao existir na BrasilAPI, retorna `404`

Response:

```json
{
  "cnpj": "27865757000102",
  "razaoSocial": "GLOBO COMUNICACAO E PARTICIPACOES S/A",
  "nomeFantasia": "TV GLOBO",
  "situacaoCadastral": "2",
  "descricaoSituacaoCadastral": "ATIVA",
  "dataInicioAtividade": "1986-01-31",
  "naturezaJuridica": "Sociedade Anônima Fechada",
  "porte": "DEMAIS",
  "capitalSocial": 6983568400,
  "email": "contato@empresa.com",
  "telefone1": "2121554551",
  "telefone2": "2121554552",
  "cep": "22460901",
  "tipoLogradouro": "RUA",
  "logradouro": "LOPES QUINTAS",
  "logradouroCompleto": "RUA LOPES QUINTAS",
  "numero": "303",
  "complemento": null,
  "bairro": "JARDIM BOTANICO",
  "municipio": "RIO DE JANEIRO",
  "codigoMunicipioIbge": "3304557",
  "uf": "RJ"
}
```

### Buscar endereco por CEP

`GET /api/integracoes/viacep/cep?cep=71827615`

Observacoes:

- aceita CEP com ou sem mascara
- o backend normaliza para 8 digitos
- se o CEP nao existir no ViaCEP, retorna `404`

Response:

```json
{
  "cep": "71827-615",
  "logradouro": "Quadra QSA 10",
  "logradouroCompleto": "Quadra QSA 10",
  "complemento": "Lote 01",
  "bairro": "Taguatinga Sul (Taguatinga)",
  "localidade": "Brasília",
  "uf": "DF",
  "estado": "Distrito Federal",
  "regiao": "Centro-Oeste",
  "ibge": "5300108",
  "gia": null,
  "ddd": "61",
  "siafi": "9701"
}
```

## Fluxo recomendado para o frontend

### Fluxo administrativo

1. listar empresas
2. selecionar empresa ativa
3. listar produtos da empresa
4. listar clientes da empresa
5. criar/editar cadastros quando necessario

### Fluxo de emissao

1. selecionar empresa
2. montar itens da venda
3. selecionar cliente ou preencher destinatario avulso
4. chamar `xml-preview`
5. se o preview estiver correto, chamar `emitir`
6. exibir `cStat`, `motivo`, `chaveAcesso` e links de download

### Fluxo de consulta/cancelamento

1. informar empresa
2. informar chave de acesso
3. consultar
4. se precisar cancelar, informar protocolo e justificativa

## Recomendacoes de UI

- sempre carregar `empresaId` no contexto da tela
- nao usar `itemId` como id de produto
- tratar `ativo=false` como registro indisponivel para operacao
- exibir `motivo` da SEFAZ de forma literal ao usuario
- para emissao, preferir `xml-preview` antes do `emitir`
- quando `modelo = NFE`, exigir destinatario mais completo no frontend
- quando o cliente for contribuinte, exigir `inscricaoEstadual`
- quando a UF do cliente for diferente da UF da empresa, validar no frontend se o CFOP comeca com `6`

## Limites atuais do backend

- sem autenticacao
- sem paginação nas listagens
- sem delete fisico
- sem endpoint dedicado para listar documentos emitidos por empresa
- armazenamento local de XML/PDF no filesystem

## Arquivos de exemplo do projeto

- `examples/empresa-exemplo.json`
- `examples/produto-empresa-exemplo.json`
- `examples/cliente-empresa-exemplo.json`
- `examples/emissao-por-cadastro-exemplo.json`
- `examples/emissao-exemplo.json`
- `examples/consulta-exemplo.json`
- `examples/cancelamento-exemplo.json`
