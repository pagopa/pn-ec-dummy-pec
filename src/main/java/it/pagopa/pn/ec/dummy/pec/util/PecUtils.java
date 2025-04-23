package it.pagopa.pn.ec.dummy.pec.util;

import it.pagopa.pn.ec.dummy.pec.dto.PecInfo;
import it.pagopa.pn.ec.dummy.pec.type.PecType;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Random;

@Slf4j
public class PecUtils {
    private static final Random random = new Random();

    private PecUtils() {
        throw new IllegalStateException("PecUtils is a utility class");
    }
    public static String generateRandomString(int length) {
        // Use the nextBytes() method to generate a random sequence of bytes.
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);

        // Convert the bytes to a string using the Base64 encoding.
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static StringBuilder generateDaticert(PecInfo pecInfo, String gestoreMittente, String data, String orario){

        PecType pecType = pecInfo.getPecType();
        StringBuilder postacertType = new StringBuilder();
        StringBuilder postacertInfo = new StringBuilder();
        String recipientType;

        switch (pecType) {
            case ACCETTAZIONE -> {
                postacertType.append("<postacert tipo=\"").append("accettazione").append("\" errore=\"nessuno\">");
                recipientType = "certificato";
            }
            case CONSEGNA -> {
                postacertType.append("<postacert tipo=\"").append("avvenuta-consegna").append("\" errore=\"nessuno\">");
                recipientType = "certificato";
                postacertInfo.append("<ricevuta tipo=\"completa\" />");
                postacertInfo.append("<consegna>").append(pecInfo.getReceiverAddress()).append("</consegna>");
            }
            case NON_PEC -> {
                postacertType.append("<postacert tipo=\"").append("non-pec").append("\" errore=\"nessuno\">");
                recipientType = "esterno";
            }
            case PREAVVISO_MANCATA_CONSEGNA -> {
                postacertType.append("<postacert tipo=\"").append("preavviso-mancata-consegna").append("\" errore=\"nessuno\">");
                recipientType = "certificato";
                postacertInfo.append("<errore-esteso>").append("5.4.1").append("</errore-esteso>");
            }
            default -> {
                postacertType.append("<postacert tipo=\"").append("pecType").append("\" errore=\"non-gestito\">");
                recipientType = "non-gestito";
            }
        }

        //Costruzione del daticert
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");//popolare con daticert su note
        stringBuilder.append(postacertType);
        stringBuilder.append("<intestazione>");
        stringBuilder.append("<mittente>").append(pecInfo.getFrom()).append("</mittente>"); //mittente dell'email, sta nella mappa
        stringBuilder.append("<destinatari tipo=\"").append(recipientType).append("\">").append(pecInfo.getReceiverAddress()).append("</destinatari>");
        stringBuilder.append("<risposte>").append(pecInfo.getReplyTo()).append("</risposte>"); //nel messaggio che uso per popolare la mappa c'è un reply-to
        stringBuilder.append("<oggetto>").append(pecInfo.getSubject()).append("</oggetto>"); //oggetto dell'email, sta nella mappa
        stringBuilder.append("</intestazione>");
        stringBuilder.append("<dati>");
        stringBuilder.append("<gestore-emittente>").append(gestoreMittente).append("</gestore-emittente>"); //da inventare = "mock-pec" costante
        stringBuilder.append("<data zona=\"+0200\">"); //lasciare così
        stringBuilder.append("<giorno>").append(data).append("</giorno>"); //impostare in base all'ora
        stringBuilder.append("<ora>").append(orario).append("</ora>"); //impostare in base all'ora
        stringBuilder.append("</data>");
        stringBuilder.append("<identificativo>").append(generateRandomString(64)).append("</identificativo>"); //stringa random 64 caratteri
        stringBuilder.append("<msgid>").append("&lt;").append(pecInfo.getMessageId()).append("&gt;").append("</msgid>"); //msgid della mappa, nella forma url encoded. fare url encode della stringa
        if (!postacertInfo.isEmpty()) {
            stringBuilder.append(postacertInfo);
        }
        stringBuilder.append("</dati>");
        stringBuilder.append("</postacert>");

        return stringBuilder;
    }

    public static String getCurrentTime() {
        return new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
    }

    public static String getCurrentDate() {
        return new SimpleDateFormat("dd/MM/yyyy").format(Calendar.getInstance().getTime());
    }
}
