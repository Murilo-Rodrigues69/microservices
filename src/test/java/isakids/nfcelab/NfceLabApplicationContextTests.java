package isakids.nfcelab;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import isakids.nfcelab.repository.EmpresaRepository;

@SpringBootTest(properties = {
        "app.nfce.ws-enabled=false",
        "app.security.bootstrap-admin.enabled=false",
        "app.security.jwt.secret=aXNhLW5mY2UtbGFiLXRlc3Qtand0LXNlY3JldC1jaGFuZ2UtbWUtMjAyNi0wOS0wNA==",
        "spring.datasource.url=jdbc:h2:mem:isa-nfce-lab-context;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "nfe.modelo=NFE",
        "nfe.ambiente=HOMOLOGACAO",
        "nfe.uf=GO",
        "nfe.certificado.pfx-path=C:/certificados/teste.pfx",
        "nfe.certificado.senha=123456",
        "nfe.cadeia.jks-path=C:/certificados/sefaz-homologacao.jks",
        "nfe.cadeia.senha=changeit",
        "nfe.csc=",
        "nfe.csc-id=0",
        "nfe.emitente.cnpj=12345678000199",
        "nfe.emitente.razao-social=EMPRESA TESTE LTDA",
        "nfe.emitente.nome-fantasia=EMPRESA TESTE",
        "nfe.emitente.inscricao-estadual=123456789",
        "nfe.emitente.serie=1",
        "nfe.emitente.natureza-operacao=VENDA DE MERCADORIA",
        "nfe.emitente.cep=75460000",
        "nfe.emitente.logradouro=RUA DE TESTE",
        "nfe.emitente.numero=100",
        "nfe.emitente.bairro=CENTRO",
        "nfe.emitente.codigo-municipio=5214507",
        "nfe.emitente.descricao-municipio=NEROPOLIS"
})
class NfceLabApplicationContextTests {

    @Autowired
    private EmpresaRepository empresaRepository;

    @Test
    void contextLoadsAndBootstrapsFirstEmpresa() {
        assertThat(empresaRepository.count()).isEqualTo(1);
        assertThat(empresaRepository.findAll())
                .singleElement()
                .satisfies(empresa -> {
                    assertThat(empresa.getNome()).isEqualTo("EMPRESA TESTE");
                    assertThat(empresa.getCnpj()).isEqualTo("12345678000199");
                    assertThat(empresa.isAtivo()).isTrue();
                });
    }
}
