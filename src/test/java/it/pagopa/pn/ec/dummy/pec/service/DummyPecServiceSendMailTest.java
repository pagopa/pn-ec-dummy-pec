package it.pagopa.pn.ec.dummy.pec.service;

import it.pagopa.pn.ec.dummy.pec.conf.DummyPecServiceTestConfiguration;
import it.pagopa.pn.ec.dummy.pec.type.PecType;
import it.pagopa.pn.ec.dummy.pec.util.DummyPecServiceTestUtil;
import it.pagopa.pn.library.exceptions.PnSpapiPermanentErrorException;
import it.pagopa.pn.ec.dummy.pec.dto.PecInfo;
import jakarta.mail.internet.AddressException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Import(DummyPecServiceTestConfiguration.class)
@SpringBootTest
class DummyPecServiceSendMailTest {
    @Autowired
    private DummyPecService dummyPecService;

    @BeforeEach
    void setUp() {
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
                .expectErrorMatches(e -> (e instanceof PnSpapiPermanentErrorException && e.getCause() instanceof NullPointerException))
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

    @Test
    void testBlackmailAddress() throws Exception {

        byte[] message = DummyPecServiceTestUtil.createMimeMessageAsBytes("Test Subject", "test@sender.com", "test1@test.it");

        StepVerifier.create(dummyPecService.sendMail(message))
                .assertNext(originalMessageId -> {
                    // Verifica che l'ID originale sia restituito
                    assertNotNull(originalMessageId);

                    // Verifica che nella mappa siano presenti solo 2 messaggi
                    Map<String, PecInfo> pecMap = dummyPecService.getPecMap();
                    assertEquals(2, pecMap.size());

                    // Verifica che ci sia un PecInfo con pecType ACCETTAZIONE
                    assertThat(pecMap.values(), Matchers.hasItem(Matchers.hasProperty("pecType", Matchers.is(PecType.ACCETTAZIONE))));

                    // Verifica che ci sia un PecInfo con pecType NON_PEC
                    assertThat(pecMap.values(), Matchers.hasItem(Matchers.hasProperty("pecType", Matchers.is(PecType.NON_PEC))));


                })
                .verifyComplete();
    }


    @Test
    void testForewarningAddress() throws Exception {

        byte[] message = DummyPecServiceTestUtil.createMimeMessageAsBytes("Test Subject", "test@sender.com", "test3@test.it");

        StepVerifier.create(dummyPecService.sendMail(message))
                    .assertNext(originalMessageId -> {
                        // Verifica che l'ID originale sia restituito
                        assertNotNull(originalMessageId);

                        // Verifica che nella mappa siano presenti solo 2 messaggi
                        Map<String, PecInfo> pecMap = dummyPecService.getPecMap();
                        assertEquals(2, pecMap.size());

                        // Verifica che ci sia un PecInfo con pecType PREAVVISO_ERRORE_CONSEGNA
                        assertThat(pecMap.values(), Matchers.hasItem(Matchers.hasProperty("pecType", Matchers.is(PecType.PREAVVISO_ERRORE_CONSEGNA))));


                    })
                    .verifyComplete();
    }

    @Test
    void testMalformedAddresses() throws Exception {

        byte[] message = DummyPecServiceTestUtil.createMimeMessageAsBytes("Test Subject", "test@sender.com", ".test@test.it");

        StepVerifier.create(dummyPecService.sendMail(message))
                .expectErrorMatches(e -> (e instanceof PnSpapiPermanentErrorException && e.getCause() instanceof AddressException))
                .verify();

    }
}