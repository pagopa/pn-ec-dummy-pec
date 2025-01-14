package it.pagopa.pn.template.service;

import it.pagopa.pn.template.dto.PecInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

@SpringBootTest(classes = DummyPecTestApplication.class)
class DummyPecServiceDeleteMessageTest {
    @Autowired
    private DummyPecService dummyPecService;

    @Test
    void testDeleteMessage_ShouldDeleteExistingMessage() {
        String messageID = "test-message-id";
        dummyPecService.getPecMap().put(messageID, PecInfo.builder().messageId(messageID).build());

        StepVerifier.create(dummyPecService.deleteMessage(messageID))
                    .verifyComplete();

        Assertions.assertNull(dummyPecService.getPecMap().get(messageID));
    }

    @Test
    void testDeleteMessage_ShouldThrowExceptionForNullOrEmptyMessageID() {
        StepVerifier.create(dummyPecService.deleteMessage(null))
                    .expectError(IllegalArgumentException.class)
                    .verify();

        StepVerifier.create(dummyPecService.deleteMessage(""))
                    .expectError(IllegalArgumentException.class)
                    .verify();
    }

    @Test
    void testDeleteMessage_ShouldThrowExceptionForNonExistentMessage() {
        StepVerifier.create(dummyPecService.deleteMessage("non-existent-id"))
                    .expectError(IllegalArgumentException.class)
                    .verify();
    }
}
