package com.dummy.pec.library.conf;

import com.dummy.pec.library.service.DummyPecService;
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
