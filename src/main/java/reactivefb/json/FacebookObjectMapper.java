package reactivefb.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import static reactivefb.json.FacebookPropertyNamingStrategy.FACEBOOK_NAMING_STRATEGY;

public class FacebookObjectMapper {

    public final static ObjectMapper INSTANCE = new ObjectMapper();
    static {
        INSTANCE.setPropertyNamingStrategy(FACEBOOK_NAMING_STRATEGY);
        INSTANCE.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

}
