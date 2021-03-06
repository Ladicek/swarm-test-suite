package org.wildfly.swarm.ts.javaee8.cdi;

import javax.inject.Inject;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.swarm.arquillian.DefaultDeployment;
import org.wildfly.swarm.ts.javaee8.cdi.spiconfigurators.LegacyService;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Arquillian.class)
@DefaultDeployment
public class SpiConfigTest {
    @Inject
    private LegacyService legacyService;

    @Test
    public void testSpiConfig() {
        assertThat(legacyService.sayHello()).isEqualTo("Hello");
    }
}
