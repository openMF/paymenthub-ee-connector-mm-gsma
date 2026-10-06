package org.mifos.connector.gsma.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * How this connector reaches the Zeebe broker: {@code zeebe.broker.*} and {@code zeebe.client.*}.
 *
 * <p>
 * {@code zeebe.client.evenly-allocated-max-jobs} is deliberately not here. Its value is a Spring expression
 * ({@code "#{...}"}) that only {@code @Value} evaluates, and a deployment could override it with a plain number, the
 * way other connectors' application.yml set it, so moving it would either fail to start or quietly ignore such an
 * override. It stays on the worker classes that use it.
 * </p>
 *
 * <p>
 * Every value here is required, as it was when it was a bare {@code @Value} field.
 * </p>
 *
 * @param broker
 *            the broker to connect to, {@code zeebe.broker.*}
 * @param client
 *            client settings, {@code zeebe.client.*}
 */
@Validated
@ConfigurationProperties(prefix = "zeebe")
public record ZeebeProperties(@NotNull @Valid Broker broker, @NotNull @Valid Client client) {

    /**
     * The broker to connect to: {@code zeebe.broker.*}.
     *
     * @param contactpoint
     *            gateway address, as {@code host:port}
     */
    public record Broker(@NotNull String contactpoint) {
    }

    /**
     * Client settings: {@code zeebe.client.*}.
     *
     * @param maxExecutionThreads
     *            size of the job worker execution thread pool
     * @param ttl
     *            how long, in milliseconds, a published message stays available for correlation
     */
    public record Client(@NotNull Integer maxExecutionThreads, @NotNull Integer ttl) {
    }
}
