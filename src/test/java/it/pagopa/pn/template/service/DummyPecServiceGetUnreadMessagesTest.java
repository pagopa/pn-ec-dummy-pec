package it.pagopa.pn.template.service;

import it.pagopa.pn.template.dto.PecInfo;
import it.pagopa.pn.template.type.PecType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = DummyPecTestApplication.class)
class DummyPecServiceGetUnreadMessagesTest {
    public static final int TOTAL_MSG = 10;
    public static final int EXPECTED_UNREAD_MSG = 4;
    public static final int LIMIT_UNREAD = 4;

    @Autowired
    private DummyPecService dummyPecService;

    @BeforeEach
    void setUp() {
        // Popola la mappa con 5 messaggi di esempio
        for (int i = 0; i < TOTAL_MSG; i++) {
            String messageId = UUID.randomUUID().toString();
            dummyPecService.getPecMap()
                           .put(messageId,
                                PecInfo.builder()
                                       .messageId(messageId)
                                       .receiverAddress("test" + i + "@receiver.com")
                                       .subject("Subject " + i)
                                       .from("test" + i + "@sender.com")
                                       .pecType(isRead(i) ? PecType.ACCETTAZIONE : PecType.CONSEGNA)
                                       .read(isRead(i)) // Solo i messaggi con indice dispari sono non letti
                                       .build());
        }
    }

    private boolean isRead(int i) {
        return i % 2 == 0;
    }

    @Test
    void testGetUnreadMessages_ShouldReturnUnreadMessagesAsBytes() {
        StepVerifier.create(dummyPecService.getUnreadMessages(LIMIT_UNREAD)).assertNext(response -> {
            assertEquals(EXPECTED_UNREAD_MSG, response.getNumOfMessages());

            // Verifica che ogni messaggio sia rappresentato come byte[]
            List<byte[]> messages = response.getPnListOfMessages().getMessages();
            assertEquals(EXPECTED_UNREAD_MSG, messages.size());
            messages.forEach(Assertions::assertNotNull);
        }).verifyComplete();
    }

    @Test
    void testGetUnreadMessages_ShouldHandleNoUnreadMessages() {
        dummyPecService.getPecMap().values().forEach(message -> message.setRead(true));

        StepVerifier.create(dummyPecService.getUnreadMessages(LIMIT_UNREAD))
                    .assertNext(response -> {
                        assertEquals(0, response.getNumOfMessages());
                        assertTrue(response.getPnListOfMessages().getMessages().isEmpty());
                    })
                    .verifyComplete();
    }
}
