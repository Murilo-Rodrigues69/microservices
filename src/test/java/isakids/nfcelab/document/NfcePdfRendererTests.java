package isakids.nfcelab.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import isakids.nfcelab.support.SampleAuthorizedXmls;

class NfcePdfRendererTests {

    @Test
    void geraPdfAPartirDoXmlAutorizado() {
        NfcePdfRenderer renderer = new NfcePdfRenderer();

        byte[] pdf = renderer.render(SampleAuthorizedXmls.authorizedNfe());

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        assertThat(pdf.length).isGreaterThan(1000);
    }
}
