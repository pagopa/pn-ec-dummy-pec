package it.pagopa.pn.template.service;

import it.pagopa.pn.library.exceptions.PnSpapiPermanentErrorException;
import it.pagopa.pn.library.pec.pojo.PnGetMessagesResponse;
import it.pagopa.pn.library.pec.pojo.PnListOfMessages;
import it.pagopa.pn.library.pec.service.PnPecService;
import it.pagopa.pn.template.dto.PecInfo;
import it.pagopa.pn.template.type.PecType;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import lombok.CustomLog;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static it.pagopa.pn.template.service.DummyPecServiceUtil.DUMMY_PATTERN_STRING;

@Getter
@RequiredArgsConstructor
@Service
@CustomLog
public class DummyPecService implements PnPecService {

    private final ConcurrentHashMap<String, PecInfo> pecMap = new ConcurrentHashMap<>();
    private final DummyPecServiceUtil dummyPecServiceUtil;

    @Value("#{'${blacklisted.addresses}'.split(',')}")
    private List<String> blacklistedAddresses;

    @Value("${regex.pattern}")
    private String regexPattern;

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

               if(!validAddress(Objects.requireNonNull(replyTo))){throw new PnSpapiPermanentErrorException("Invalid address: " + replyTo);}

               String receiverAddress = mimeMessage.getAllRecipients()[0].toString();
               String originalMessageId = mimeMessage.getMessageID();

               originalMessageIdRef.set(originalMessageId);

               log.info("Received message with subject: {}, from: {}, replyTo: {}, receiverAddress: {}, originalMessageId: {}",
                        subject, from, replyTo, receiverAddress, originalMessageId);

               // build unique id for acceptance and delivery
               String acceptanceMessageId = UUID.randomUUID() + DUMMY_PATTERN_STRING;
               String deliveryMessageId = UUID.randomUUID() + DUMMY_PATTERN_STRING;

               PecInfo acceptanceInfo = PecInfo.builder()
                                               .messageId(messageID)
                                               .receiverAddress(receiverAddress)
                                               .subject(subject)
                                               .from(from)
                                               .replyTo(replyTo)
                                               .pecType(PecType.ACCETTAZIONE)
                                               .errorMap(Map.of()) // no default error
                                               .read(false)
                                               .build();

               PecInfo deliveryInfo = PecInfo.builder()
                                             .messageId(messageID)
                                             .receiverAddress(receiverAddress)
                                             .subject(subject)
                                             .from(from)
                                             .replyTo(replyTo)
                                             .pecType(PecType.CONSEGNA)
                                             .errorMap(Map.of()) // no default error
                                             .read(false)
                                             .build();

               // add message in thread-safe object
               pecMap.put(acceptanceMessageId, acceptanceInfo);
               pecMap.put(deliveryMessageId, deliveryInfo);

               return originalMessageId;
       })
       .doOnSuccess(messageId -> log.logEndingProcess("Send mail success, message id: " + messageId))
       .doOnError(throwable -> log.logEndingProcess("Send mail error, message id: " + originalMessageIdRef.get(), false, throwable.getMessage()))
       .delayElement(java.time.Duration.ofMillis(dummyPecServiceUtil.calculateRandomDelay()));
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
                       List<byte[]> messageBytes = unreadMessages.stream().map(dummyPecServiceUtil::convertPecInfoToBytes).toList();

                       // Crea un oggetto PnListOfMessages
                       PnListOfMessages pnListOfMessages = new PnListOfMessages(messageBytes);

                       // Restituisci la risposta PnGetMessagesResponse
                       return new PnGetMessagesResponse(pnListOfMessages, unreadMessages.size());
                   })
       .doOnSuccess(result -> log.logEndingProcess("Get unread messages success, message ids: " + unreadMessagesLog))
       .doOnError(throwable -> log.logEndingProcess("Get unread messages error, message ids: " + unreadMessagesLog, false, throwable.getMessage()))
       .delayElement(java.time.Duration.ofMillis(dummyPecServiceUtil.calculateRandomDelay()));
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
       .doOnSuccess(result -> log.logEndingProcess("Mark message as read success with message id: " + originalMessageIdRef.get()))
       .doOnError(throwable -> log.logEndingProcess("Mark message as read error with message id: " + originalMessageIdRef.get(),
                                                    false, throwable.getMessage()))
       .delayElement(java.time.Duration.ofMillis(dummyPecServiceUtil.calculateRandomDelay())).then();
    }


    @Override
    public Mono<Integer> getMessageCount() {
        log.logStartingProcess("Get message count starting...");

        return Mono.fromSupplier(pecMap::size)
           .doOnSuccess(result -> log.logEndingProcess("Get message count success, value: " + pecMap.size()))
           .doOnError(throwable -> log.logEndingProcess("Get message count error, value: " + pecMap.size(), false, throwable.getMessage()))
           .delayElement(java.time.Duration.ofMillis(dummyPecServiceUtil.calculateRandomDelay()));
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
        .doOnSuccess(result -> log.logEndingProcess("Delete message success, message id: " + messageID))
        .doOnError(throwable -> log.logEndingProcess("Delete message  error, message id: " + messageID, false, throwable.getMessage()))
        .delayElement(java.time.Duration.ofMillis(dummyPecServiceUtil.calculateRandomDelay())).then();
    }

    private boolean validAddress(@NotNull String replyTo){

        if( Pattern.compile(regexPattern).matcher(replyTo).matches() ) {
            return !blacklistedAddresses.contains(replyTo);
        }
        else { return false; }

    }
}