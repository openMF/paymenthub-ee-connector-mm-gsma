package org.mifos.connector.gsma.zeebe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JobVariablesTest {

    @Test
    void aMissingVariableIsNamed() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("transactionId", null);

        assertThatThrownBy(() -> JobVariables.requiredText(variables, "gsmaChannelRequest")).isInstanceOf(IllegalStateException.class)
                .hasMessage("Zeebe variable 'gsmaChannelRequest' is missing");
        assertThatThrownBy(() -> JobVariables.requiredAsText(variables, "transactionId"))
                .hasMessage("Zeebe variable 'transactionId' is missing");
    }

    @Test
    void aVariableOfTheWrongTypeIsNamedWithItsType() {
        assertThatThrownBy(() -> JobVariables.requiredText(Map.of("channelRequest", 42), "channelRequest"))
                .isInstanceOf(IllegalStateException.class).hasMessage("Zeebe variable 'channelRequest' is a Integer, expected text");
    }

    @Test
    void presentVariablesAreReturned() {
        assertThat(JobVariables.requiredText(Map.of("channelRequest", "{}"), "channelRequest")).isEqualTo("{}");
        assertThat(JobVariables.requiredAsText(Map.of("transactionId", 7L), "transactionId")).isEqualTo("7");
    }
}
