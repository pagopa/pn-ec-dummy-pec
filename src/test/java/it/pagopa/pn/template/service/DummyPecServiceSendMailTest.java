package it.pagopa.pn.template.service;

import it.pagopa.pn.template.dto.PecInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = DummyPecTestApplication.class)
class DummyPecServiceSendMailTest {
    @Autowired
    private DummyPecService dummyPecService;

    @BeforeEach
    void resetPecMap() {
        dummyPecService.getPecMap().clear();
    }

    @Test
    void testSendMail_ShouldInsertMessagesIntoMapAndReturnOriginalId() throws Exception {
        // Arrange
        byte[] message = DummyPecServiceTestUtil.createMimeMessageAsBytes("Test Subject", "test@sender.com", "test@receiver.com");

        // Act
        StepVerifier.create(dummyPecService.sendMail(message))
                    .assertNext(originalMessageId -> {
                        // Verifica che l'ID originale sia restituito
                        assertNotNull(originalMessageId);

                        // Verifica che nella mappa siano presenti solo 2 messaggi
                        Map<String, PecInfo> pecMap = dummyPecService.getPecMap();
                        assertEquals(2, pecMap.size());

                        // Verifica i dettagli dei messaggi
                        for (PecInfo pecInfo : pecMap.values()) {
                            assertEquals("Test Subject", pecInfo.getSubject());
                            assertEquals("test@receiver.com", pecInfo.getReceiverAddress());
                            assertEquals("test@sender.com", pecInfo.getFrom());
                            assertFalse(pecInfo.isRead());
                        }
                    })
                    .verifyComplete();
    }


    @Test
    void testSendMail_ShouldHandleNullMessage() {
        // Arrange
        byte[] message = null;

        // Act & Assert
        StepVerifier.create(dummyPecService.sendMail(message))
                    .expectError(NullPointerException.class)
                    .verify();
    }

    @Test
    void testSendMail_ShouldHandleInvalidMimeMessage() {
        // Arrange
        byte[] invalidMessage = "invalid message".getBytes();

        // Act & Assert
        StepVerifier.create(dummyPecService.sendMail(invalidMessage))
                    .expectError(Exception.class)
                    .verify();
    }
}
