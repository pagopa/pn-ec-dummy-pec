package it.pagopa.pn.ec.dummy.pec.service;

import it.pagopa.pn.ec.dummy.pec.conf.DummyPecServiceTestConfiguration;
import it.pagopa.pn.ec.dummy.pec.dto.PecInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

import java.util.UUID;

@Import(DummyPecServiceTestConfiguration.class)
@SpringBootTest
class DummyPecServiceGetMessageCountTest {
    @Autowired
    private DummyPecService dummyPecService;

    @BeforeEach
    void setUp() {
        dummyPecService.getPecMap().clear();
    }

    @Test
    void testGetMessageCount_ShouldReturnCorrectCount() {
        for (int i = 0; i < 5; i++) {
            String messageId = UUID.randomUUID().toString();
            dummyPecService.getPecMap().put(messageId, PecInfo.builder()
                                                              .messageId(messageId)
                                                              .build());
        }

        StepVerifier.create(dummyPecService.getMessageCount())
                    .expectNext(5)
                    .verifyComplete();
    }

    @Test
    void testGetMessageCount_ShouldReturnZeroForEmptyMap() {
        StepVerifier.create(dummyPecService.getMessageCount())
                    .expectNext(0)
                    .verifyComplete();
    }


    @Test
    void testGetMessageCount_ShouldReflectDynamicChanges() {
        dummyPecService.getPecMap().put("msg1", PecInfo.builder().messageId("msg1#id").build());
        dummyPecService.getPecMap().put("msg2", PecInfo.builder().messageId("msg2#id").build());

        StepVerifier.create(dummyPecService.getMessageCount())
                    .expectNext(2)
                    .verifyComplete();

        dummyPecService.getPecMap().put("msg3", PecInfo.builder().messageId("msg3#id").build());

        StepVerifier.create(dummyPecService.getMessageCount())
                    .expectNext(3)
                    .verifyComplete();
    }
}
