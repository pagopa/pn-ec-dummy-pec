package com.dummy.pec.library.service;

import com.dummy.pec.library.conf.DummyPecServiceTestConfiguration;
import com.dummy.pec.library.dto.PecInfo;
import it.pagopa.pn.library.exceptions.PnSpapiPermanentErrorException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

@Import(DummyPecServiceTestConfiguration.class)
@SpringBootTest
class DummyPecServiceDeleteMessageTest {
    @Autowired
    private DummyPecService dummyPecService;

    @BeforeEach
    void setUp() {
        dummyPecService.getPecMap().clear();
    }

    @Test
    void testDeleteMessage_ShouldDeleteExistingMessage() {
        String messageID = "test-message-id";
        dummyPecService.getPecMap().put(messageID, PecInfo.builder().messageId(messageID).build());

        StepVerifier.create(dummyPecService.deleteMessage(messageID))
                    .verifyComplete();

        Assertions.assertNull(dummyPecService.getPecMap().get(messageID));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testDeleteMessage_ShouldThrowExceptionForNullOrEmptyMessageID(String messageID) {
        StepVerifier.create(dummyPecService.deleteMessage(messageID))
                    .expectErrorMatches(e -> (e instanceof PnSpapiPermanentErrorException && e.getCause() instanceof IllegalArgumentException))
                    .verify();
    }

    @Test
    void testDeleteMessage_ShouldThrowExceptionForNonExistentMessage() {
        StepVerifier.create(dummyPecService.deleteMessage("non-existent-id"))
                    .expectErrorMatches(e -> (e instanceof PnSpapiPermanentErrorException && e.getCause() instanceof IllegalArgumentException))
                    .verify();
    }
}
