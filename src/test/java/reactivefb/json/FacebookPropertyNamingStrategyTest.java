package reactivefb.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restfb.types.DeviceCode;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static reactivefb.json.FacebookPropertyNamingStrategy.FACEBOOK_NAMING_STRATEGY;

public class FacebookPropertyNamingStrategyTest {

    private final static ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.setPropertyNamingStrategy(FACEBOOK_NAMING_STRATEGY);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Test
    public void shouldSerializeWithFacebookFields(){

        DeviceCode deviceCode = new DeviceCode();
        deviceCode.setVerificationUri("testUri");

        JsonNode tree = objectMapper.valueToTree(deviceCode);

        assertThat(tree.get("verification_uri").asText()).isEqualTo("testUri");

    }

    @Test
    public void shouldDeserializeWithFacebookFields() throws IOException {

        DeviceCode deviceCode = objectMapper.readValue("{\"verification_uri\":\"testUri\"}",
                DeviceCode.class);

        assertThat(deviceCode.getVerificationUri()).isEqualTo("testUri");

    }

}
