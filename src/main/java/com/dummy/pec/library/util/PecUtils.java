package com.dummy.pec.library.util;

import com.dummy.pec.library.dto.PecInfo;
import com.dummy.pec.library.type.PecType;
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

    public static StringBuilder generateDaticert(PecInfo pecInfo, String gestoreMittente, String data, String orario, String tipoDestinatario){

        boolean isAccettazione = pecInfo.getPecType().equals(PecType.ACCETTAZIONE);
        String postacertType = isAccettazione ? "accettazione" : "avvenuta-consegna";

        //Costruzione del daticert
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");//popolare con daticert su note
        stringBuilder.append("<postacert tipo=\"").append(postacertType).append("\" errore=\"nessuno\">");
        stringBuilder.append("<intestazione>");
        stringBuilder.append("<mittente>").append(pecInfo.getFrom()).append("</mittente>"); //mittente dell'email, sta nella mappa
        stringBuilder.append("<destinatari tipo=\"").append(tipoDestinatario).append("\">").append(pecInfo.getReceiverAddress()).append("</destinatari>");
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
        if (!isAccettazione) {
            stringBuilder.append("<ricevuta tipo=\"completa\" />");
            stringBuilder.append("<consegna>").append(pecInfo.getReceiverAddress()).append("</consegna>");
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
