package it.pagopa.pn.template.service;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

@Service
public class DummyPecServiceTestUtil {

    public static byte[] createMimeMessageAsBytes(String subject, String from, String to) throws Exception {
        Session session = Session.getDefaultInstance(new Properties());

        // create a new MimeMessage
        MimeMessage mimeMessage = new MimeMessage(session);
        mimeMessage.setSubject(subject);
        mimeMessage.setFrom(new InternetAddress(from));
        mimeMessage.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(to));

        // add simple body content
        MimeBodyPart bodyPart = new MimeBodyPart();
        bodyPart.setText("This is a body simple text message");

        // build the message with body
        MimeMultipart multipart = new MimeMultipart();
        multipart.addBodyPart(bodyPart);
        mimeMessage.setContent(multipart);

        // write the message to a byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        mimeMessage.writeTo(outputStream);

        return outputStream.toByteArray();
    }
}

