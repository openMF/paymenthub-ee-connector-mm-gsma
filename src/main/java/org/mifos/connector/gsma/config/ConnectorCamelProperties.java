package org.mifos.connector.gsma.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The {@code camel.*} settings this connector owns: the callback host it hands to the GSMA API, the port the Camel REST
 * endpoints listen on, and whether TLS verification is switched off.
 *
 * <p>
 * Only these three keys are bound. Everything else under {@code camel.} belongs to camel-spring-boot and is left alone.
 * The names are unchanged: the deployment sets {@code PORT}, which reaches {@code camel.server-port} through
 * {@code ${PORT:5000}} in application.yml, and a rename would break that silently. All three are required, as they were
 * when they were bare {@code @Value} fields.
 * </p>
 *
 * @param host
 *            callback URL the GSMA API is told to notify
 * @param serverPort
 *            port the Camel REST endpoints listen on
 * @param disableSsl
 *            when true, the https Camel component trusts any certificate
 */
@Validated
@ConfigurationProperties(prefix = "camel")
public record ConnectorCamelProperties(@NotNull String host, @NotNull Integer serverPort, @NotNull Boolean disableSsl) {
}
