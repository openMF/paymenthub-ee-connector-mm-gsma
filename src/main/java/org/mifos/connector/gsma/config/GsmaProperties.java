package org.mifos.connector.gsma.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Everything this connector needs in order to talk to the GSMA Mobile Money API.
 *
 * <p>
 * The property names are unchanged, because a deployment can set them as environment variables and a rename would break
 * that silently.
 * </p>
 *
 * <p>
 * Every value is required, as it was when it was a bare {@code @Value} field. {@code @Valid} carries the check into the
 * two groups, so deleting a whole section stops startup with a message naming the property, instead of leaving the
 * group null and failing later with a NullPointerException that names nothing.
 * </p>
 *
 * @param api
 *            the GSMA Mobile Money API itself, {@code gsma.api.*}
 * @param auth
 *            the OAuth credentials used to fetch an access token, {@code gsma.auth.*}
 */
@Validated
@ConfigurationProperties(prefix = "gsma")
public record GsmaProperties(@NotNull @Valid Api api, @NotNull @Valid Auth auth) {

    /**
     * The GSMA Mobile Money API itself: {@code gsma.api.*}.
     *
     * @param host
     *            base URL of the GSMA API, for example
     *            {@code https://sandbox.mobilemoneyapi.io/oauth/simulator/v1.1/mm}
     * @param account
     *            path of the accounts resource under the base URL, for example {@code /accounts}
     * @param channel
     *            base URL of the channel connector this connector calls back into
     */
    public record Api(@NotNull String host, @NotNull String account, @NotNull String channel) {
    }

    /**
     * The OAuth credentials used to fetch an access token: {@code gsma.auth.*}.
     *
     * @param host
     *            token endpoint of the GSMA API
     * @param clientKey
     *            OAuth client key
     * @param clientSecret
     *            OAuth client secret
     */
    public record Auth(@NotNull String host, @NotNull String clientKey, @NotNull String clientSecret) {
    }
}
