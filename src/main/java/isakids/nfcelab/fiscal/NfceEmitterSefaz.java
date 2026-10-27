package isakids.nfcelab.fiscal;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fincatto.documentofiscal.DFModelo;
import com.fincatto.documentofiscal.nfe.classes.distribuicao.NFDistribuicaoDocumentoZip;
import com.fincatto.documentofiscal.nfe.classes.distribuicao.NFDistribuicaoIntRetorno;
import com.fincatto.documentofiscal.nfe.webservices.distribuicao.WSDistribuicaoNFe;
import com.fincatto.documentofiscal.nfe400.classes.NFProtocolo;
import com.fincatto.documentofiscal.nfe400.classes.NFProtocoloInfo;
import com.fincatto.documentofiscal.nfe400.classes.evento.NFInfoEventoRetorno;
import com.fincatto.documentofiscal.nfe400.classes.lote.consulta.NFLoteConsultaRetorno;
import com.fincatto.documentofiscal.nfe400.classes.lote.envio.NFCancelamentoRetornoDados;
import com.fincatto.documentofiscal.nfe400.classes.lote.envio.NFLoteEnvio;
import com.fincatto.documentofiscal.nfe400.classes.lote.envio.NFLoteEnvioRetorno;
import com.fincatto.documentofiscal.nfe400.classes.lote.envio.NFLoteEnvioRetornoDados;
import com.fincatto.documentofiscal.nfe400.classes.lote.envio.NFLoteIndicadorProcessamento;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNota;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaInfoSuplementar;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaProcessada;
import com.fincatto.documentofiscal.nfe400.classes.nota.consulta.NFNotaConsultaRetorno;
import com.fincatto.documentofiscal.nfe400.utils.NFGeraQRCode;
import com.fincatto.documentofiscal.nfe400.webservices.WSFacade;

import isakids.nfcelab.entity.EmpresaEntity;
import isakids.nfcelab.model.RecipientData;
import isakids.nfcelab.model.SaleData;

@Component
public class NfceEmitterSefaz implements NfceEmitter {

    private final FiscalContextFactory fiscalContextFactory;

    public NfceEmitterSefaz(FiscalContextFactory fiscalContextFactory) {
        this.fiscalContextFactory = fiscalContextFactory;
    }

    @Override
    public NfceEmissionResult emitir(EmpresaEntity empresa, SaleData venda, RecipientData destinatario) {
        try {
            final FiscalContext fiscalContext = fiscalContextFactory.create(empresa);
            final NfceXmlFactory xmlFactory = new NfceXmlFactory(fiscalContext.nfeConfig(), fiscalContext.issuerProperties());
            final NFNota nota = xmlFactory.gerarNotaFiscal(venda, destinatario);
            final WSFacade wsFacade = getWsFacade(fiscalContext);

            final NFLoteEnvio lote = new NFLoteEnvio();
            lote.setVersao(fiscalContext.nfeConfig().getVersao());
            lote.setIdLote(String.valueOf(venda.vendaId() != null ? venda.vendaId() : System.currentTimeMillis()));
            lote.setIndicadorProcessamento(NFLoteIndicadorProcessamento.PROCESSAMENTO_SINCRONO);
            lote.setNotas(List.of(nota));

            final NFLoteEnvioRetornoDados dados = wsFacade.enviaLote(lote);
            final NFLoteEnvioRetorno retorno = dados.getRetorno();
            final String cStatEnvio = retorno.getStatus();
            final String xMotivoEnvio = retorno.getMotivo();
            NFNota notaAssinada = extrairNotaAssinada(dados, nota);

            if ("104".equals(cStatEnvio) && retorno.getProtocoloInfo() != null) {
                return montarResultadoAutorizacao(fiscalContext, notaAssinada, retorno.getProtocoloInfo());
            }

            if ("103".equals(cStatEnvio) && retorno.getInfoRecebimento() != null) {
                final String recibo = retorno.getInfoRecebimento().getRecibo();
                NFLoteConsultaRetorno consulta = consultarAteProcessar(fiscalContext, wsFacade, recibo);
                if (consulta != null
                        && "104".equals(consulta.getStatus())
                        && consulta.getProtocolos() != null
                        && !consulta.getProtocolos().isEmpty()
                        && consulta.getProtocolos().get(0).getProtocoloInfo() != null) {
                    NFProtocoloInfo protInfo = consulta.getProtocolos().get(0).getProtocoloInfo();
                    return montarResultadoAutorizacao(fiscalContext, notaAssinada, protInfo);
                }

                return NfceEmissionResult.builder()
                        .cStat(parseIntSafe(consulta != null ? consulta.getStatus() : cStatEnvio))
                        .motivo(consulta != null ? consulta.getMotivo() : xMotivoEnvio)
                        .build();
            }

            return NfceEmissionResult.builder()
                    .cStat(parseIntSafe(cStatEnvio))
                    .motivo(xMotivoEnvio)
                    .build();
        } catch (Exception e) {
            return NfceEmissionResult.builder()
                    .cStat(500)
                        .motivo(truncate("Erro emitindo " + documentLabel(empresa) + ": " + e.getMessage(), 2000))
                    .build();
        }
    }

    @Override
    public NfceEmissionResult consultarPorChave(EmpresaEntity empresa, String chaveAcesso) {
        try {
            FiscalContext fiscalContext = fiscalContextFactory.create(empresa);
            WSFacade wsFacade = getWsFacade(fiscalContext);
            NFNotaConsultaRetorno retorno = wsFacade.consultaNota(chaveAcesso);
            String xmlAutorizado = recuperarXmlAutorizadoPorDistribuicao(fiscalContext, chaveAcesso);
            return NfceEmissionResult.builder()
                    .cStat(parseIntSafe(retorno.getStatus()))
                    .motivo(retorno.getMotivo())
                    .chaveAcesso(retorno.getChave())
                    .protocoloAutorizacao(extrairProtocoloConsulta(retorno))
                    .xmlAutorizado(xmlAutorizado)
                    .build();
        } catch (Exception e) {
            return NfceEmissionResult.builder()
                    .cStat(500)
                        .motivo(truncate("Erro consultando " + documentLabel(empresa) + ": " + e.getMessage(), 2000))
                    .build();
        }
    }

    @Override
    public NfceEmissionResult cancelar(EmpresaEntity empresa, String chaveAcesso, String protocolo, String justificativa) {
        try {
            FiscalContext fiscalContext = fiscalContextFactory.create(empresa);
            NFCancelamentoRetornoDados dados = getWsFacade(fiscalContext).cancelaNota(chaveAcesso, protocolo, justificativa);
            NFInfoEventoRetorno info = dados.getProtocolo()
                    .getInfoEventoRetorno()
                    .getInfoEventoRetorno();

            return NfceEmissionResult.builder()
                    .cStat(info.getCodigoStatus())
                    .motivo(info.getMotivo())
                    .chaveAcesso(info.getChave())
                    .protocoloAutorizacao(info.getNumeroProtocolo())
                    .build();
        } catch (Exception e) {
            return NfceEmissionResult.builder()
                    .cStat(500)
                        .motivo(truncate("Erro cancelando " + documentLabel(empresa) + ": " + e.getMessage(), 2000))
                    .build();
        }
    }

    private NFNota extrairNotaAssinada(NFLoteEnvioRetornoDados dados, NFNota notaOriginal) {
        try {
            if (dados != null
                    && dados.getLoteAssinado() != null
                    && dados.getLoteAssinado().getNotas() != null
                    && !dados.getLoteAssinado().getNotas().isEmpty()
                    && dados.getLoteAssinado().getNotas().get(0) != null) {
                return dados.getLoteAssinado().getNotas().get(0);
            }
        } catch (Exception ignored) {
        }
        return notaOriginal;
    }

    private NFLoteConsultaRetorno consultarAteProcessar(FiscalContext fiscalContext, WSFacade wsFacade, String recibo) throws Exception {
        final int maxTentativas = 10;
        final long esperaMs = 800L;
        NFLoteConsultaRetorno last = null;

        for (int i = 0; i < maxTentativas; i++) {
            last = wsFacade.consultaLote(recibo, fiscalContext.nfeConfig().getModelo());
            if (last != null && !"105".equals(last.getStatus())) {
                return last;
            }
            Thread.sleep(esperaMs);
        }

        return last;
    }

    private NfceEmissionResult montarResultadoAutorizacao(FiscalContext fiscalContext, NFNota notaAssinada, NFProtocoloInfo protInfo)
            throws Exception {
        if (isNfce(fiscalContext)) {
            preencherQrCodeSePossivel(fiscalContext, notaAssinada);
        }

        NFProtocolo protocolo = new NFProtocolo();
        protocolo.setProtocoloInfo(protInfo);

        NFNotaProcessada proc = new NFNotaProcessada();
        proc.setNota(notaAssinada);
        proc.setProtocolo(protocolo);
        proc.setVersao(BigDecimal.valueOf(4.00));

        return NfceEmissionResult.builder()
                .cStat(parseIntSafe(protInfo.getStatus()))
                .motivo(protInfo.getMotivo())
                .chaveAcesso(protInfo.getChave())
                .protocoloAutorizacao(protInfo.getNumeroProtocolo())
                .qrCodeUrl(isNfce(fiscalContext) ? extrairQrCode(notaAssinada) : null)
                .xmlAutorizado(proc.toString())
                .build();
    }

    private void preencherQrCodeSePossivel(FiscalContext fiscalContext, NFNota notaAssinada) {
        try {
            NFGeraQRCode qrCodeGenerator = new NFGeraQRCode(notaAssinada, fiscalContext.nfeConfig());
            String qrCode = qrCodeGenerator.getQRCode();
            String urlConsulta = qrCodeGenerator.urlConsultaChaveAcesso();
            NFNotaInfoSuplementar suplementar = notaAssinada.getInfoSuplementar();
            if (suplementar == null) {
                suplementar = new NFNotaInfoSuplementar();
            }
            suplementar.setQrCode(qrCode);
            suplementar.setUrlConsultaChaveAcesso(urlConsulta);
            notaAssinada.setInfoSuplementar(suplementar);
        } catch (Exception ignored) {
        }
    }

    private String extrairQrCode(NFNota notaAssinada) {
        return notaAssinada.getInfoSuplementar() != null ? notaAssinada.getInfoSuplementar().getQrCode() : null;
    }

    private String extrairProtocoloConsulta(NFNotaConsultaRetorno retorno) {
        if (retorno.getProtocolo() != null && retorno.getProtocolo().getProtocoloInfo() != null) {
            return retorno.getProtocolo().getProtocoloInfo().getNumeroProtocolo();
        }
        return null;
    }

    private String recuperarXmlAutorizadoPorDistribuicao(FiscalContext fiscalContext, String chaveAcesso) {
        try {
            String cnpj = onlyDigits(fiscalContext.issuerProperties().getCnpj());
            if (!StringUtils.hasText(cnpj) || cnpj.length() != 14) {
                return null;
            }

            NFDistribuicaoIntRetorno retorno = getWsFacade(fiscalContext)
                    .consultarDistribuicaoDFe(cnpj, fiscalContext.nfeConfig().getCUF(), chaveAcesso, null, null);

            if (retorno == null || retorno.getLote() == null || retorno.getLote().getDocZip() == null) {
                return null;
            }

            for (NFDistribuicaoDocumentoZip docZip : retorno.getLote().getDocZip()) {
                String xml = WSDistribuicaoNFe.decodeGZipToXml(docZip.getValue());
                if (StringUtils.hasText(xml) && xml.contains(chaveAcesso)) {
                    return xml;
                }
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private WSFacade getWsFacade(FiscalContext fiscalContext) {
        WSFacade wsFacade = fiscalContext.wsFacade();
        if (wsFacade == null) {
            throw new IllegalStateException("Webservice da SEFAZ desabilitado. Ajuste app.nfce.ws-enabled=true.");
        }
        return wsFacade;
    }

    private Integer parseIntSafe(String value) {
        try {
            return value == null ? null : Integer.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isNfce(FiscalContext fiscalContext) {
        return DFModelo.NFCE.equals(fiscalContext.nfeConfig().getModelo());
    }

    private boolean isNfce(EmpresaEntity empresa) {
        return empresa != null && DFModelo.NFCE.name().equals(empresa.getModelo() != null ? empresa.getModelo().name() : null);
    }

    private String documentLabel(FiscalContext fiscalContext) {
        return isNfce(fiscalContext) ? "NFC-e" : "NF-e";
    }

    private String documentLabel(EmpresaEntity empresa) {
        return isNfce(empresa) ? "NFC-e" : "NF-e";
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private String onlyDigits(String value) {
        return value == null ? null : value.replaceAll("\\D", "");
    }
}
