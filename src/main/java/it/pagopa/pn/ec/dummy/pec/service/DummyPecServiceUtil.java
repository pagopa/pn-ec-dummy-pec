package it.pagopa.pn.ec.dummy.pec.service;

import it.pagopa.pn.ec.dummy.pec.dto.PecInfo;
import it.pagopa.pn.ec.dummy.pec.type.PecType;
import it.pagopa.pn.ec.dummy.pec.util.PecUtils;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.CustomLog;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

import static jakarta.mail.Message.RecipientType.TO;

@CustomLog
public class DummyPecServiceUtil {

    private DummyPecServiceUtil() {
        throw new IllegalStateException("DummyPecServiceUtil is a utility class");
    }
    public static final String DUMMY_PATTERN_STRING = "@pec.dummy.it";

    public static long calculateRandomDelay(long minDelayMs, long maxDelayMs) {
        return minDelayMs + (long) (Math.random() * (maxDelayMs - minDelayMs));
    }

    public static byte[] convertPecInfoToBytes(Map.Entry<String, PecInfo> entry) {
        try {
            String messageID = entry.getKey();
            PecInfo pecInfo = entry.getValue();

            // Creazione della sessione MIME
            Session session = Session.getDefaultInstance(new Properties());
            MimeMessage mimeMessage = new MimeMessage(session);

            // Imposta i dettagli del messaggio
            mimeMessage.setSubject(pecInfo.getSubject() != null ? pecInfo.getSubject() : "No Subject");
            mimeMessage.setFrom(pecInfo.getFrom() != null ? pecInfo.getFrom() : "unknown@domain.com");

            if (pecInfo.getReceiverAddress() != null) {
                mimeMessage.addRecipient(TO, new jakarta.mail.internet.InternetAddress(pecInfo.getReceiverAddress()));
            }

            // Crea il contenuto del messaggio
            var textPart = new MimeBodyPart();
            textPart.setText("Questo è un messaggio di esempio.", "UTF-8");

            // Genera il contenuto del daticert.xml
            String tipoDestinatario = pecInfo.getPecType().equals(PecType.NON_PEC) ? "esterno" : "certificato";
            StringBuilder datiCertXml = PecUtils.generateDaticert(pecInfo, "mock-pec", PecUtils.getCurrentDate(), PecUtils.getCurrentTime(), tipoDestinatario);

            // Crea la parte del messaggio per daticert.xml
            var datiCertPart = new MimeBodyPart();

            DataSource dataSource = new ByteArrayDataSource(datiCertXml.toString().getBytes(StandardCharsets.UTF_8), "application/xml");
            datiCertPart.setDataHandler(new DataHandler(dataSource));
            datiCertPart.setFileName("daticert.xml");

            // Combina il corpo e l'allegato
            var mimeMultipart = new MimeMultipart();
            mimeMultipart.addBodyPart(textPart);
            mimeMultipart.addBodyPart(datiCertPart);

            // Imposta il contenuto del messaggio MIME
            mimeMessage.setContent(mimeMultipart);

            // Converte il messaggio MIME in byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            mimeMessage.saveChanges();
            mimeMessage.setHeader("Message-ID", messageID);
            mimeMessage.writeTo(outputStream);

            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error converting PecInfo to byte[]", e);
        }
    }

    public static PecInfo buildPecInfo(String messageID, String receiverAddress, String subject, String from, String replyTo, PecType pecType) {
        return PecInfo.builder()
                .messageId(messageID)
                .receiverAddress(receiverAddress)
                .subject(subject)
                .from(from)
                .replyTo(replyTo)
                .pecType(pecType)
                .errorMap(Map.of()) // no default error
                .read(false)
                .build();
    }

}
