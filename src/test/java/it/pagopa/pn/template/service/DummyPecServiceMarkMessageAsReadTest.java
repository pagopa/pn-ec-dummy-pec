package it.pagopa.pn.template.service;

import it.pagopa.pn.template.dto.PecInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = DummyPecTestApplication.class)
class DummyPecServiceMarkMessageAsReadTest {
    @Autowired
    private DummyPecService dummyPecService;

    @Test
    void testMarkMessageAsRead_ShouldMarkMessageAsRead() {
        String messageID = "test-message-id";
        dummyPecService.getPecMap().put(messageID, PecInfo.builder()
                                                          .messageId(messageID)
                                                          .read(false)
                                                          .build());

        StepVerifier.create(dummyPecService.markMessageAsRead(messageID))
                    .verifyComplete();

        assertTrue(dummyPecService.getPecMap().get(messageID).isRead());
    }

    @Test
    void testMarkMessageAsRead_ShouldThrowExceptionForNullMessageID() {
        StepVerifier.create(dummyPecService.markMessageAsRead(null))
                    .expectError(IllegalArgumentException.class)
                    .verify();

        StepVerifier.create(dummyPecService.markMessageAsRead(""))
                    .expectError(IllegalArgumentException.class)
                    .verify();
    }

    @Test
    void testMarkMessageAsRead_ShouldThrowExceptionForNonExistentMessage() {
        StepVerifier.create(dummyPecService.markMessageAsRead("non-existent-id"))
                    .expectError(IllegalArgumentException.class)
                    .verify();
    }
}
