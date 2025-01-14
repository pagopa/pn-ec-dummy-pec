package it.pagopa.pn.template.service;

import it.pagopa.pn.template.dto.PecInfo;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

@Service
public class DummyPecServiceUtil {
    @Value("${dummy.pec.min-delay-ms:100}")
    private long minDelayMs;

    @Value("${dummy.pec.max-delay-ms:500}")
    private long maxDelayMs;

    public long calculateRandomDelay() {
        return minDelayMs + (long) (Math.random() * (maxDelayMs - minDelayMs));
    }

    byte[] convertPecInfoToBytes(PecInfo pecInfo) {
        try {
            Session session = Session.getDefaultInstance(new Properties());

            MimeMessage mimeMessage = new MimeMessage(session);
            mimeMessage.setSubject(pecInfo.getSubject() != null ? pecInfo.getSubject() : "No Subject");
            mimeMessage.setFrom(pecInfo.getFrom() != null ? pecInfo.getFrom() : "unknown@domain.com");

            if (pecInfo.getReceiverAddress() != null) {
                mimeMessage.addRecipient(MimeMessage.RecipientType.TO,
                                         new jakarta.mail.internet.InternetAddress(pecInfo.getReceiverAddress()));
            }

            mimeMessage.setText("Questo è un messaggio di esempio.");
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            mimeMessage.writeTo(outputStream);

            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error converting PecInfo to byte[]", e);
        }
    }

}
