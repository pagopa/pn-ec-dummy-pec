package it.pagopa.pn.ec.dummy.pec.conf;

import it.pagopa.pn.ec.dummy.pec.service.DummyPecService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import(DummyPecSharedAutoConfiguration.class)
public class DummyPecServiceTestConfiguration {
    @Bean
    public DummyPecService dummyPecService() {
        return new DummyPecService();
    }
}
