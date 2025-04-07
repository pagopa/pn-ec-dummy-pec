package it.pagopa.pn.ec.dummy.pec.conf;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.util.ResourceUtils;

@Configuration
@PropertySource(ResourceUtils.CLASSPATH_URL_PREFIX + "application-shared-dummy.properties")
public class DummyPecSharedAutoConfiguration {
}
