package isakids.isa.services;

import java.io.ByteArrayOutputStream;

import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Image;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.Barcode128;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;



@Service
public class CodigoBarrasService {

    public byte[] gerarCodigoBarrasPDF(String codigoBarras) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            Document document = new Document();
            PdfWriter writer = PdfWriter.getInstance(document, baos);

            document.open();

            // Título / texto
            Paragraph titulo = new Paragraph("Código de barras: " + codigoBarras);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);

            document.add(new Paragraph(" ")); // linha em branco

            // Gera o barcode
            Barcode128 barcode = new Barcode128();
            barcode.setCode(codigoBarras);
            barcode.setCodeType(Barcode128.CODE128);

            Image image = barcode.createImageWithBarcode(writer.getDirectContent(), null, null);
            image.setAlignment(Image.ALIGN_CENTER);

            document.add(image);

            document.close();

            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar PDF de código de barras", e);
        }
    }
}
