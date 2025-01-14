package it.pagopa.pn.template.service;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class DummyPecServiceTestConfiguration {

    @Bean
    public DummyPecService dummyPecService() {
        return new DummyPecService(new DummyPecServiceUtil());
    }

    @Bean
    public DummyPecServiceUtil dummyPecServiceUtil() {
        return new DummyPecServiceUtil();
    }
}
