package reactivefb;

import com.restfb.Version;
import org.junit.Test;

import static com.restfb.FacebookClient.AccessToken;
import static org.assertj.core.api.Assertions.assertThat;

public class UserTest {

    private static final String appId = "371017066972600";
    private static final String appSecret = "990f893fcd3bd433131d981838bdc0cc";
    private static final String appAccessToken = "371017066972600|1T2pBN_RqbGs05ecz63RSYOXreo";

    DefaultReactiveFacebookClient rootFacebookClient
            = DefaultReactiveFacebookClient.builder(Version.VERSION_2_9).build();

    @Test
    public void shouldObtainAppAccessToken(){
//        AccessToken accessToken = rootFacebookClient.obtainDeviceAccessToken()o(appId, appSecret).block();
//        assertThat(accessToken.getAccessToken()).isEqualTo(appAccessToken);
    }

}
