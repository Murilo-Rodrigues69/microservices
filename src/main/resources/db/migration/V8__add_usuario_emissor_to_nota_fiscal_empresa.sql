alter table nota_fiscal_empresa
    add column usuario_emissor_id bigint;

alter table nota_fiscal_empresa
    add column usuario_emissor_login varchar(120);

alter table nota_fiscal_empresa
    add column usuario_emissor_nome varchar(255);

alter table nota_fiscal_empresa
    add constraint fk_nota_fiscal_empresa_usuario_emissor
        foreign key (usuario_emissor_id) references app_user(id);

create index if not exists idx_nota_fiscal_empresa_usuario_emissor
    on nota_fiscal_empresa (usuario_emissor_id);
