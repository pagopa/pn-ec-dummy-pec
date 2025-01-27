package it.pagopa.pn.template.util;

import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Random;

@Slf4j
public class PecUtils {
    public static String generateRandomString(int length) {
        Random random = new Random();

        // Use the nextBytes() method to generate a random sequence of bytes.
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);

        // Convert the bytes to a string using the Base64 encoding.
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static StringBuffer generateDaticertConsegna(String from, String receiver, String replyTo, String subject, String gestoreMittente, String data, String orario, String messageId){

        //Costruzione del daticert
        StringBuffer stringBufferContent = new StringBuffer();
        stringBufferContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");//popolare con daticert su note
        stringBufferContent.append("<postacert tipo=\"avvenuta-consegna\" errore=\"nessuno\">");
        stringBufferContent.append("<intestazione>");
        stringBufferContent.append("<mittente>").append(from).append("</mittente>"); //mittente dell'email, sta nella mappa
        stringBufferContent.append("<destinatari tipo=\"certificato\">").append(receiver).append("</destinatari>"); //destinatario dell'email, sta nella mappa
        stringBufferContent.append("<risposte>").append(replyTo).append("</risposte>"); //nel messaggio che uso per popolare la mappa c'è un reply-to
        stringBufferContent.append("<oggetto>").append(subject).append("</oggetto>"); //oggetto dell'email, sta nella mappa
        stringBufferContent.append("</intestazione>");
        stringBufferContent.append("<dati>");
        stringBufferContent.append("<gestore-emittente>").append(gestoreMittente).append("</gestore-emittente>"); //da inventare = "mock-pec" costante
        stringBufferContent.append("<data zona=\"+0200\">"); //lasciare così
        stringBufferContent.append("<giorno>").append(data).append("</giorno>"); //impostare in base all'ora
        stringBufferContent.append("<ora>").append(orario).append("</ora>"); //impostare in base all'ora
        stringBufferContent.append("</data>");
        stringBufferContent.append("<identificativo>").append(generateRandomString(64)).append("</identificativo>"); //stringa random 64 caratteri
        stringBufferContent.append("<msgid>").append("&lt;").append(messageId).append("&gt;").append("</msgid>"); //msgid della mappa, nella forma url encoded. fare url encode della stringa
        stringBufferContent.append("<ricevuta tipo=\"completa\" />");
        stringBufferContent.append("<consegna>").append(receiver).append("</consegna>");
        stringBufferContent.append("</dati>");
        stringBufferContent.append("</postacert>");

        return stringBufferContent;
    }

    public static String getCurrentTime() {
        return new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
    }

    public static String getCurrentDate() {
        return new SimpleDateFormat("dd/MM/yyyy").format(Calendar.getInstance().getTime());
    }
}
