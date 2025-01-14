package it.pagopa.pn.template.service;

import it.pagopa.pn.library.pec.pojo.PnGetMessagesResponse;
import it.pagopa.pn.library.pec.pojo.PnListOfMessages;
import it.pagopa.pn.library.pec.service.PnPecService;
import it.pagopa.pn.template.dto.PecInfo;
import it.pagopa.pn.template.type.PecType;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@RequiredArgsConstructor
@Service
public class DummyPecService implements PnPecService {
    private final ConcurrentHashMap<String, PecInfo> pecMap = new ConcurrentHashMap<>();
    private final DummyPecServiceUtil dummyPecServiceUtil;

    @Override
    public Mono<String> sendMail(byte[] message) {
        return Mono.fromCallable(() -> {
            // parse message
            MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(message));

            // get message info
            String subject = mimeMessage.getSubject();
            String from = mimeMessage.getFrom()[0].toString();
            String replyTo = (mimeMessage.getReplyTo() != null && mimeMessage.getReplyTo().length > 0)
                    ? mimeMessage.getReplyTo()[0].toString() : null;
            String receiverAddress = mimeMessage.getAllRecipients()[0].toString();
            String originalMessageId = mimeMessage.getMessageID();

            // build unique id for acceptance and delivery
            String acceptanceMessageId = UUID.randomUUID().toString();
            String deliveryMessageId = UUID.randomUUID().toString();

            PecInfo acceptanceInfo = PecInfo.builder()
                                            .messageId(acceptanceMessageId)
                                            .receiverAddress(receiverAddress)
                                            .subject(subject)
                                            .from(from)
                                            .replyTo(replyTo)
                                            .pecType(PecType.ACCETTAZIONE)
                                            .errorMap(Map.of()) // no default error
                                            .read(false)
                                            .build();

            PecInfo deliveryInfo = PecInfo.builder()
                                          .messageId(deliveryMessageId)
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
        }).delayElement(java.time.Duration.ofMillis(dummyPecServiceUtil.calculateRandomDelay()));
    }

    @Override
    public Mono<PnGetMessagesResponse> getUnreadMessages(int limit) {
        return Mono.fromCallable(() -> {
            if (limit <= 0) {
                throw new IllegalArgumentException("Limit must be greater than 0");
            }

            // Filtra i messaggi non letti
            var unreadMessages = pecMap.values().stream()
                                       .filter(pecInfo -> !pecInfo.isRead())
                                       .limit(limit)
                                       .toList();

            // Costruisci la lista di byte[] da PecInfo
            List<byte[]> messageBytes = unreadMessages.stream()
                                                      .map(dummyPecServiceUtil::convertPecInfoToBytes)
                                                      .toList();

            // Crea un oggetto PnListOfMessages
            PnListOfMessages pnListOfMessages = new PnListOfMessages(messageBytes);

            // Restituisci la risposta PnGetMessagesResponse
            return new PnGetMessagesResponse(pnListOfMessages, unreadMessages.size());
        }).delayElement(java.time.Duration.ofMillis(dummyPecServiceUtil.calculateRandomDelay()));
    }

    @Override
    public Mono<Void> markMessageAsRead(String messageID) {
        return Mono.fromRunnable(() -> {
            if (messageID == null || messageID.isEmpty()) {
                throw new IllegalArgumentException("Message ID cannot be null or empty");
            }

            PecInfo message = pecMap.get(messageID);

            if (message == null) {
                throw new IllegalArgumentException("Message with ID " + messageID + " not found");
            }

            message.setRead(true);
        }).delayElement(java.time.Duration.ofMillis(dummyPecServiceUtil.calculateRandomDelay())).then();
    }


    @Override
    public Mono<Integer> getMessageCount() {
        return Mono.fromSupplier(pecMap::size)
                   .delayElement(java.time.Duration.ofMillis(dummyPecServiceUtil.calculateRandomDelay()));
    }

    @Override
    public Mono<Void> deleteMessage(String messageID) {
        return Mono.fromRunnable(() -> {
            if (messageID == null || messageID.isEmpty()) {
                throw new IllegalArgumentException("Message ID cannot be null or empty");
            }

            PecInfo removedMessage = pecMap.remove(messageID);

            if (removedMessage == null) {
                throw new IllegalArgumentException("Message with ID " + messageID + " not found");
            }
        }).delayElement(java.time.Duration.ofMillis(dummyPecServiceUtil.calculateRandomDelay())).then();
    }
}
