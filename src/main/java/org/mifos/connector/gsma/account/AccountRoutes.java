package org.mifos.connector.gsma.account;

import static org.mifos.connector.gsma.camel.config.CamelProperties.ACCESS_TOKEN;
import static org.mifos.connector.gsma.camel.config.CamelProperties.ACCOUNT_ACTION;
import static org.mifos.connector.gsma.camel.config.CamelProperties.ACCOUNT_RESPONSE;
import static org.mifos.connector.gsma.camel.config.CamelProperties.IDENTIFIER;
import static org.mifos.connector.gsma.camel.config.CamelProperties.IDENTIFIER_TYPE;
import static org.mifos.connector.gsma.camel.config.CamelProperties.IS_API_CALL;
import static org.mifos.connector.gsma.zeebe.ZeebeExpressionVariables.PARTY_LOOKUP_FAILED;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.mifos.connector.common.gsma.dto.AccountBalanceResponseDTO;
import org.mifos.connector.common.gsma.dto.AccountNameResponseDTO;
import org.mifos.connector.common.gsma.dto.ErrorDTO;
import org.mifos.connector.gsma.auth.AccessTokenStore;
import org.mifos.connector.gsma.config.GsmaProperties;
import org.mifos.connector.gsma.zeebe.ZeebeProcessStarter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("checkstyle:whitespaceafter")
public class AccountRoutes extends RouteBuilder {

    @Autowired
    private ZeebeProcessStarter zeebeProcessStarter;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccessTokenStore accessTokenStore;

    private final String baseURL;

    private final String account;

    @Autowired
    private AccountResponseProcessor accountResponseProcessor;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public AccountRoutes(GsmaProperties gsmaProperties) {
        this.baseURL = gsmaProperties.api().host();
        this.account = gsmaProperties.api().account();
    }

    @Override
    public void configure() throws Exception {

        /**
         * Error handling route
         */
        from("direct:account-error").id("account-error").unmarshal().json(JsonLibrary.Jackson, ErrorDTO.class).process(exchange -> {
            logger.error(exchange.getIn().getBody(ErrorDTO.class).toString());
        }).setProperty(PARTY_LOOKUP_FAILED, constant(true)).process(accountResponseProcessor);

        /**
         * Route when account API call was successful
         */
        from("direct:account-success").id("account-success").choice()
                .when(exchange -> exchange.getProperty(ACCOUNT_ACTION, String.class).equals("status"))
                .log(LoggingLevel.INFO, "Routing to account status handler").to("direct:account-status-handler")
                .when(exchange -> exchange.getProperty(ACCOUNT_ACTION, String.class).equals("balance"))
                .log(LoggingLevel.INFO, "Routing to account balance handler").to("direct:account-balance-handler")
                .when(exchange -> exchange.getProperty(ACCOUNT_ACTION, String.class).equals("accountname"))
                .log(LoggingLevel.INFO, "Routing to account name handler").to("direct:account-name-handler").otherwise()
                .log(LoggingLevel.INFO, "No routing specified for this type of action.").endChoice();

        /**
         * Account balance response handler
         */
        from("direct:account-balance-handler").id("account-balance-handler").unmarshal()
                .json(JsonLibrary.Jackson, AccountBalanceResponseDTO.class).process(exchange -> {
                    exchange.setProperty(ACCOUNT_RESPONSE, exchange.getIn().getBody(AccountBalanceResponseDTO.class).getCurrentBalance());
                });

        /**
         * Account status response handler
         */
        from("direct:account-status-handler").id("account-status-handler").log(LoggingLevel.INFO, "Inside account status handler")
                .setProperty(PARTY_LOOKUP_FAILED, constant(false)).process(accountResponseProcessor);

        /**
         * Account name response handler
         */
        from("direct:account-name-handler").id("account-name-handler").unmarshal().json(JsonLibrary.Jackson, AccountNameResponseDTO.class)
                .process(exchange -> {
                    exchange.setProperty(ACCOUNT_RESPONSE, exchange.getIn().getBody(AccountNameResponseDTO.class).getName().getFullName());
                });

        /**
         * Route to call GSMA account status API
         */
        from("direct:get-account-details").id("get-account-details").removeHeader("*").setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .setHeader("X-Date", simple(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)))
                .setHeader("Authorization", simple("Bearer ${exchangeProperty." + ACCESS_TOKEN + "}"))
                .toD(baseURL + account + "/${exchangeProperty." + IDENTIFIER_TYPE + "}/${exchangeProperty." + IDENTIFIER
                        + "}/${exchangeProperty." + ACCOUNT_ACTION + "}?bridgeEndpoint=true&throwExceptionOnFailure=false");

        /**
         * Base route for accounts
         */
        from("direct:account-route").id("account-route")
                .log(LoggingLevel.INFO, "Getting ${exchangeProperty." + ACCOUNT_ACTION + "} for Identifier")
                .log(LoggingLevel.INFO, "Completed ${exchangeProperty." + ACCOUNT_ACTION + "} ${body}").choice()
                .when(exchange -> exchange.getProperty(IS_API_CALL, String.class).equals("true"))
                .log(LoggingLevel.INFO, "Setting off API response").otherwise().choice()
                .when(header("CamelHttpResponseCode").isEqualTo("200")).to("direct:account-success").otherwise()
                .to("direct:account-success") // Changing to test, OG: "direct:account-error"
                .endChoice().endChoice();

        /**
         * Method to get account status
         */
        from("rest:GET:/account/status/{identifierType}/{identifier}").log(LoggingLevel.INFO, "Getting Account Status")
                .process(exchange -> {
                    exchange.setProperty(IDENTIFIER_TYPE, exchange.getIn().getHeader("identifierType"));
                    exchange.setProperty(IDENTIFIER, exchange.getIn().getHeader("identifier"));
                    exchange.setProperty(IS_API_CALL, "true");
                    exchange.setProperty(ACCOUNT_ACTION, "status");
                }).to("direct:account-route");

        /**
         * Method to get account name
         */
        from("rest:GET:/account/name/{identifierType}/{identifier}").log(LoggingLevel.INFO, "Getting Account Name").process(exchange -> {
            exchange.setProperty(IDENTIFIER_TYPE, exchange.getIn().getHeader("identifierType"));
            exchange.setProperty(IDENTIFIER, exchange.getIn().getHeader("identifier"));
            exchange.setProperty(IS_API_CALL, "true");
            exchange.setProperty(ACCOUNT_ACTION, "accountname");
        }).to("direct:account-route");

        /**
         * Method to get account balance
         */
        from("rest:GET:/account/balance/{identifierType}/{identifier}").log(LoggingLevel.INFO, "Getting Account Balance")
                .process(exchange -> {
                    exchange.setProperty(IDENTIFIER_TYPE, exchange.getIn().getHeader("identifierType"));
                    exchange.setProperty(IDENTIFIER, exchange.getIn().getHeader("identifier"));
                    exchange.setProperty(IS_API_CALL, "true");
                    exchange.setProperty(ACCOUNT_ACTION, "balance");
                }).to("direct:account-route");

        /**
         * Method to get account statement
         */
        from("rest:GET:/account/statements/{identifierType}/{identifier}").log(LoggingLevel.INFO, "Getting Account Statements")
                .process(exchange -> {
                    exchange.setProperty(IDENTIFIER_TYPE, exchange.getIn().getHeader("identifierType"));
                    exchange.setProperty(IDENTIFIER, exchange.getIn().getHeader("identifier"));
                    exchange.setProperty(IS_API_CALL, "true");
                    exchange.setProperty(ACCOUNT_ACTION, "statemententries");
                }).to("direct:account-route");

        /**
         * Method to get account transactions
         */
        from("rest:GET:/account/transactions/{identifierType}/{identifier}").log(LoggingLevel.INFO, "Getting Account Transactions")
                .process(exchange -> {
                    exchange.setProperty(IDENTIFIER_TYPE, exchange.getIn().getHeader("identifierType"));
                    exchange.setProperty(IDENTIFIER, exchange.getIn().getHeader("identifier"));
                    exchange.setProperty(IS_API_CALL, "true");
                    exchange.setProperty(ACCOUNT_ACTION, "transactions");
                }).to("direct:account-route");
    }
}
