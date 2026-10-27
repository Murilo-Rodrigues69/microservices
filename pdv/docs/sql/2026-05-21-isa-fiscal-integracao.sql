alter table produtos
    add column ncm varchar(8) null,
    add column cest varchar(7) null,
    add column origem varchar(120) null,
    add column cfop varchar(4) null,
    add column unidade_comercial varchar(10) null;

alter table vendas
    add column cpf_cnpj_consumidor varchar(14) null,
    add column nota_fiscal_empresa_id bigint null,
    add column nota_fiscal_empresa_nome varchar(255) null,
    add column nota_fiscal_modelo varchar(10) null,
    add column nota_fiscal_situacao varchar(30) null,
    add column nota_fiscal_c_stat integer null,
    add column nota_fiscal_motivo varchar(2000) null,
    add column nota_fiscal_chave_acesso varchar(44) null,
    add column nota_fiscal_protocolo_autorizacao varchar(50) null,
    add column nota_fiscal_qr_code_url varchar(2000) null,
    add column nota_fiscal_data_emissao datetime null;
