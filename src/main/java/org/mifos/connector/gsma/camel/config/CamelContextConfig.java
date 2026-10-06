package org.mifos.connector.gsma.camel.config;

import java.util.HashMap;
import org.apache.camel.CamelContext;
import org.apache.camel.component.http.HttpComponent;
import org.apache.camel.spi.RestConfiguration;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.mifos.connector.gsma.config.ConnectorCamelProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CamelContextConfig {

    private final int serverPort;

    private final boolean disableSSL;

    @Autowired
    private HttpClientConfigurerTrustAllCACerts httpClientConfigurerTrustAllCACerts;

    public CamelContextConfig(ConnectorCamelProperties camelProperties) {
        this.serverPort = camelProperties.serverPort();
        this.disableSSL = camelProperties.disableSsl();
    }

    @Bean
    CamelContextConfiguration contextConfiguration() {
        return new CamelContextConfiguration() {

            @Override
            public void beforeApplicationStart(CamelContext camelContext) {
                camelContext.setTracing(false);
                camelContext.setMessageHistory(false);
                camelContext.setStreamCaching(true);
                camelContext.disableJMX();

                if (disableSSL) {
                    HttpComponent httpComponent = camelContext.getComponent("https", HttpComponent.class);
                    httpComponent.setHttpClientConfigurer(httpClientConfigurerTrustAllCACerts);
                }

                RestConfiguration rest = new RestConfiguration();
                camelContext.setRestConfiguration(rest);
                rest.setComponent("undertow");
                rest.setProducerComponent("undertow");
                rest.setPort(serverPort);
                rest.setBindingMode(RestConfiguration.RestBindingMode.json);
                rest.setDataFormatProperties(new HashMap<>());
                rest.getDataFormatProperties().put("prettyPrint", "true");
                rest.setScheme("http");
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                // empty
            }
        };
    }
}
