package com.dummy.pec.library.service;

import com.dummy.pec.library.conf.DummyPecServiceTestConfiguration;
import com.dummy.pec.library.dto.PecInfo;
import it.pagopa.pn.library.exceptions.PnSpapiPermanentErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(DummyPecServiceTestConfiguration.class)
@SpringBootTest
class DummyPecServiceMarkMessageAsReadTest {
    @Autowired
    private DummyPecService dummyPecService;

    @BeforeEach
    void setUp() {
        dummyPecService.getPecMap().clear();
    }

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

    @ParameterizedTest
    @NullAndEmptySource
    void testMarkMessageAsRead_ShouldThrowExceptionForNullMessageID(String messageID) {
        StepVerifier.create(dummyPecService.markMessageAsRead(messageID))
                    .expectErrorMatches(e -> (e instanceof PnSpapiPermanentErrorException && e.getCause() instanceof IllegalArgumentException))
                    .verify();
    }

    @Test
    void testMarkMessageAsRead_ShouldThrowExceptionForNonExistentMessage() {
        StepVerifier.create(dummyPecService.markMessageAsRead("non-existent-id"))
                    .expectErrorMatches(e -> (e instanceof PnSpapiPermanentErrorException && e.getCause() instanceof IllegalArgumentException))
                    .verify();
    }
}
