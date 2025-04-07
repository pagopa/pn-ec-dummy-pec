package it.pagopa.pn.ec.dummy.pec.service;

import it.pagopa.pn.ec.dummy.pec.dto.PecInfo;
import it.pagopa.pn.library.exceptions.PnSpapiPermanentErrorException;
import it.pagopa.pn.library.pec.pojo.PnGetMessagesResponse;
import it.pagopa.pn.library.pec.pojo.PnListOfMessages;
import it.pagopa.pn.library.pec.service.PnPecService;
import it.pagopa.pn.ec.dummy.pec.type.PecType;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import lombok.CustomLog;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static it.pagopa.pn.ec.dummy.pec.service.DummyPecServiceUtil.*;

@CustomLog
public class DummyPecService implements PnPecService {

    @Getter
    private final ConcurrentHashMap<String, PecInfo> pecMap = new ConcurrentHashMap<>();

    @Value("${dummy.pec.min-delay-ms}")
    private long minDelayMs;

    @Value("${dummy.pec.max-delay-ms}")
    private long maxDelayMs;

    @Value("#{'${blacklisted.addresses}'.split(',')}")
    private List<String> blacklistedAddresses;

    @Override
    public Mono<String> sendMail(byte[] message) {
        log.logStartingProcess("Send mail starting...");
        AtomicReference<String> originalMessageIdRef = new AtomicReference<>();

        return Mono.fromCallable(() -> {
               // parse message
               MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(message));

               // get message info
               String messageID = mimeMessage.getMessageID();
               String subject = mimeMessage.getSubject();
               String from = mimeMessage.getFrom()[0].toString();
               String replyTo = (mimeMessage.getReplyTo() != null && mimeMessage.getReplyTo().length > 0) ? mimeMessage.getRecipients(Message.RecipientType.TO)[0].toString() : null;

               String receiverAddress = mimeMessage.getAllRecipients()[0].toString();
               String originalMessageId = mimeMessage.getMessageID();

               originalMessageIdRef.set(originalMessageId);

               log.info("Received message with subject: {}, from: {}, replyTo: {}, receiverAddress: {}, originalMessageId: {}",
                        subject, from, replyTo, receiverAddress, originalMessageId);

               // Creazione della ricevuta di accettazione.
               String acceptanceMessageId = UUID.randomUUID() + DUMMY_PATTERN_STRING;
               PecInfo acceptanceInfo = buildPecInfo(messageID, receiverAddress, subject, from, replyTo, PecType.ACCETTAZIONE);
               pecMap.put(acceptanceMessageId, acceptanceInfo);

               // Se l'indirizzo non è PEC, viene generata la ricevuta di accettazione, ma non quella di consegna.
               if (isPecAddress(Objects.requireNonNull(replyTo))) {
                   PecInfo deliveryInfo = buildPecInfo(messageID, receiverAddress, subject, from, replyTo, PecType.CONSEGNA);
                   pecMap.put(UUID.randomUUID() + DUMMY_PATTERN_STRING, deliveryInfo);
               } else {
                   PecInfo nonPecInfo = buildPecInfo(messageID, receiverAddress, subject, from, replyTo, PecType.NON_PEC);
                   pecMap.put(UUID.randomUUID() + DUMMY_PATTERN_STRING, nonPecInfo);
               }

               return originalMessageId;
       })
       .onErrorMap(e -> new PnSpapiPermanentErrorException(e.getClass() + " " + e.getMessage(), e))
       .doOnSuccess(messageId -> log.logEndingProcess("Send mail success, message id: " + messageId))
       .doOnError(throwable -> log.logEndingProcess("Send mail error, message id: " + originalMessageIdRef.get(), false, throwable.getMessage()))
       .delayElement(java.time.Duration.ofMillis(calculateRandomDelay(minDelayMs, maxDelayMs)));
    }

    @Override
    public Mono<PnGetMessagesResponse> getUnreadMessages(int limit) {
        log.logStartingProcess("Get unread messages starting...");
        log.info("Limit: {}", limit);
        StringBuilder unreadMessagesLog = new StringBuilder();

        return Mono.fromCallable(() -> {
                       if (limit <= 0) {
                           throw new IllegalArgumentException("Limit must be greater than 0");
                       }

                       // Filtra i messaggi non letti
                       var unreadMessages = pecMap.entrySet().stream().filter(entry -> !entry.getValue().isRead()).limit(limit).toList();

                       unreadMessages.stream()
                                     .map(entry -> entry.getValue().getMessageId())
                                     .toList()
                                     .forEach(messageId -> unreadMessagesLog.append(messageId).append(", "));

                       // Costruisci la lista di byte[] da PecInfo
                       List<byte[]> messageBytes = unreadMessages.stream().map(DummyPecServiceUtil::convertPecInfoToBytes).toList();

                       // Crea un oggetto PnListOfMessages
                       PnListOfMessages pnListOfMessages = new PnListOfMessages(messageBytes);

                       // Restituisci la risposta PnGetMessagesResponse
                       return new PnGetMessagesResponse(pnListOfMessages, unreadMessages.size());
                   })
       .onErrorMap(e -> new PnSpapiPermanentErrorException(e.getClass() + " " + e.getMessage(), e))
       .doOnSuccess(result -> log.logEndingProcess("Get unread messages success, message ids: " + unreadMessagesLog))
       .doOnError(throwable -> log.logEndingProcess("Get unread messages error, message ids: " + unreadMessagesLog, false, throwable.getMessage()))
       .delayElement(java.time.Duration.ofMillis(calculateRandomDelay(minDelayMs, maxDelayMs)));
    }

    @Override
    public Mono<Void> markMessageAsRead(String messageID) {
        log.logStartingProcess("Mark message as read starting...");
        log.info("Message ID: {}", messageID);
        AtomicReference<String> originalMessageIdRef = new AtomicReference<>(messageID);

        return Mono.fromRunnable(() -> {
            if (messageID == null || messageID.isEmpty()) {
                throw new IllegalArgumentException("Message ID cannot be null or empty");
            }

            PecInfo message = pecMap.get(messageID);

            if (message == null) {
                throw new IllegalArgumentException("Message with ID " + messageID + " not found");
            }

            message.setRead(true);
        })
       .onErrorMap(e -> new PnSpapiPermanentErrorException(e.getClass() + " " + e.getMessage(), e))
       .doOnSuccess(result -> log.logEndingProcess("Mark message as read success with message id: " + originalMessageIdRef.get()))
       .doOnError(throwable -> log.logEndingProcess("Mark message as read error with message id: " + originalMessageIdRef.get(), false, throwable.getMessage()))
       .delayElement(java.time.Duration.ofMillis(calculateRandomDelay(minDelayMs, maxDelayMs))).then();
    }


    @Override
    public Mono<Integer> getMessageCount() {
        log.logStartingProcess("Get message count starting...");

        return Mono.fromSupplier(pecMap::size)
           .onErrorMap(e -> new PnSpapiPermanentErrorException(e.getClass() + " " + e.getMessage(), e))
           .doOnSuccess(result -> log.logEndingProcess("Get message count success, value: " + pecMap.size()))
           .doOnError(throwable -> log.logEndingProcess("Get message count error, value: " + pecMap.size(), false, throwable.getMessage()))
           .delayElement(java.time.Duration.ofMillis(calculateRandomDelay(minDelayMs, maxDelayMs)));
    }

    @Override
    public Mono<Void> deleteMessage(String messageID) {
        log.logStartingProcess("Delete message starting...");
        log.info("Message ID: {}", messageID);

        return Mono.fromRunnable(() -> {
            if (messageID == null || messageID.isEmpty()) {
                throw new IllegalArgumentException("Message ID cannot be null or empty");
            }

            PecInfo removedMessage = pecMap.remove(messageID);

            if (removedMessage == null) {
                throw new IllegalArgumentException("Message with ID " + messageID + " not found");
            }
        })
        .onErrorMap(e -> new PnSpapiPermanentErrorException(e.getClass() + " " + e.getMessage(), e))
        .doOnSuccess(result -> log.logEndingProcess("Delete message success, message id: " + messageID))
        .doOnError(throwable -> log.logEndingProcess("Delete message  error, message id: " + messageID, false, throwable.getMessage()))
        .delayElement(java.time.Duration.ofMillis(calculateRandomDelay(minDelayMs, maxDelayMs))).then();
    }

    private boolean isPecAddress(@NotNull String replyTo) {
        return !blacklistedAddresses.contains(replyTo);
    }
}