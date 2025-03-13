package it.pagopa.pn.template.service;

import it.pagopa.pn.template.dto.PecInfo;
import it.pagopa.pn.template.util.PecUtils;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

import static jakarta.mail.Message.RecipientType.TO;

@Service
@CustomLog
public class DummyPecServiceUtil {
    @Value("${dummy.pec.min-delay-ms:100}")
    private long minDelayMs;
    @Value("${dummy.pec.max-delay-ms:500}")
    private long maxDelayMs;

    public long calculateRandomDelay() {
        return minDelayMs + (long) (Math.random() * (maxDelayMs - minDelayMs));
    }

    byte[] convertPecInfoToBytes(Map.Entry<String, PecInfo> entry) {
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
            StringBuilder datiCertXml = PecUtils.generateDaticert(pecInfo, "mock-pec", PecUtils.getCurrentDate(), PecUtils.getCurrentTime());

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

}
