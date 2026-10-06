package org.mifos.connector.gsma.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Every property these records ask for has to be there, or the connector must refuse to start and say which one is
 * missing. That is what the plain {@code @Value} declarations did before they were replaced, so these tests hold the
 * replacement to the same promise, and they read the real application.yml rather than a copy of it.
 *
 * <p>
 * The missing-section check runs with no configuration file at all. Loading only this module's application.yml is not
 * enough to remove a section, because paymenthub-ee-core puts its own application.yaml on the classpath and that one
 * also sets {@code zeebe.*}.
 * </p>
 */
class ShippedConfigBindsTest {

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({ GsmaProperties.class, ConnectorCamelProperties.class, ZeebeProperties.class })
    static class AllRecords {}

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(GsmaProperties.class)
    static class OnlyGsma {}

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ConnectorCamelProperties.class)
    static class OnlyCamel {}

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ZeebeProperties.class)
    static class OnlyZeebe {}

    /** One configuration class per record, keyed by the prefix that record binds. */
    private static final Map<String, Class<?>> ONE_RECORD_EACH = Map.of("gsma", OnlyGsma.class, "camel", OnlyCamel.class, "zeebe",
            OnlyZeebe.class);

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner().withConfiguration(
                AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class, ValidationAutoConfiguration.class));
    }

    private ApplicationContextRunner withShippedYaml() {
        return runner().withInitializer(new ConfigDataApplicationContextInitializer());
    }

    @Test
    void theShippedApplicationYamlFillsEveryField() {
        withShippedYaml().withUserConfiguration(AllRecords.class).run(context -> {
            assertThat(context).hasNotFailed();
            GsmaProperties gsma = context.getBean(GsmaProperties.class);
            assertThat(gsma.api().host()).isEqualTo("https://sandbox.mobilemoneyapi.io/oauth/simulator/v1.1/mm");
            assertThat(gsma.api().account()).isEqualTo("/accounts");
            assertThat(gsma.api().channel()).isEqualTo("https://paymenthub-ee-connector-channel:8443");
            assertThat(gsma.auth().host()).isEqualTo("https://sandbox.mobilemoneyapi.io/v1/oauth/accesstoken");
            assertThat(gsma.auth().clientKey()).isNotEmpty();
            assertThat(gsma.auth().clientSecret()).isNotEmpty();
            ConnectorCamelProperties camel = context.getBean(ConnectorCamelProperties.class);
            assertThat(camel.serverPort()).isEqualTo(5000);
            assertThat(camel.disableSsl()).isTrue();
            assertThat(camel.host()).isNotEmpty();
            ZeebeProperties zeebe = context.getBean(ZeebeProperties.class);
            assertThat(zeebe.broker().contactpoint()).isEqualTo("127.0.0.1:26500");
            assertThat(zeebe.client().maxExecutionThreads()).isEqualTo(100);
            assertThat(zeebe.client().ttl()).isEqualTo(30000);
        });
    }

    @Test
    void everyRecordRefusesToStartWhenItsSectionIsMissing() {
        ONE_RECORD_EACH.forEach((prefix, configuration) -> runner().withUserConfiguration(configuration).run(context -> {
            assertThat(context).as("context with nothing configured under '%s'", prefix).hasFailed();
            assertThat(context.getStartupFailure()).as("failure for '%s'", prefix).hasStackTraceContaining("BindValidationException")
                    .hasStackTraceContaining("Binding validation errors on " + prefix);
        }));
    }

    @Test
    void aValueSetToNothingOnANumberFieldStopsStartup() {
        withShippedYaml().withUserConfiguration(OnlyCamel.class).withPropertyValues("camel.server-port=").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining("Binding validation errors on camel");
        });
    }

    @Test
    void aValueSetToNothingOnABooleanFieldStopsStartup() {
        withShippedYaml().withUserConfiguration(OnlyCamel.class).withPropertyValues("camel.disable-ssl=").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining("Binding validation errors on camel");
        });
    }

    @Test
    void aValueSetToNothingOnAStringFieldIsAcceptedJustAsItWasBefore() {
        withShippedYaml().withUserConfiguration(OnlyCamel.class).withPropertyValues("camel.host=").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(ConnectorCamelProperties.class).host()).isEmpty();
        });
    }
}
