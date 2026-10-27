package isakids.nfcelab.document;

import java.util.Optional;

public interface FiscalDocumentStorage {

    boolean isEnabled();

    void store(String chaveAcesso, String xml, byte[] pdfBytes);

    Optional<NfceStoredDocument> load(String chaveAcesso);
}
