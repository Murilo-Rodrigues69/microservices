alter table nota_fiscal_empresa
    add column origem_sistema varchar(40);

alter table nota_fiscal_empresa
    add column venda_origem_id bigint;

alter table nota_fiscal_empresa
    add constraint uk_nota_fiscal_empresa_origem_venda
        unique (empresa_id, origem_sistema, venda_origem_id);

create index if not exists idx_nota_fiscal_empresa_origem_venda
    on nota_fiscal_empresa (empresa_id, origem_sistema, venda_origem_id);
