package reactivefb;

import com.restfb.Version;
import com.restfb.exception.devicetoken.FacebookDeviceTokenPendingException;
import com.restfb.scope.ScopeBuilder;
import com.restfb.types.DeviceCode;
import org.junit.Test;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.restfb.FacebookClient.AccessToken;
import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationTest {

    private static final String appId = "371017066972600";
    private static final String appSecret = "990f893fcd3bd433131d981838bdc0cc";
    private static final String appAccessToken = "371017066972600|1T2pBN_RqbGs05ecz63RSYOXreo";

    private static DefaultReactiveFacebookClient rootFacebookClient
            = DefaultReactiveFacebookClient.builder(getVersion()).build();

    @Test
    public void shouldObtainAppAccessToken(){
        StepVerifier.create(rootFacebookClient.obtainAppAccessToken(appId, appSecret))
                .assertNext(accessToken -> assertThat(accessToken.getAccessToken()).isEqualTo(appAccessToken))
                .verifyComplete();
    }

    public static String getAppId(){
        return appId;
    }

    public static String getAppAccessToken(){
        return appAccessToken;
    }

    public static Version getVersion(){
        return Version.VERSION_2_9;
    }
}
