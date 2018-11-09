package reactivefb;

import com.restfb.Version;
import com.restfb.exception.FacebookOAuthException;
import com.restfb.scope.ScopeBuilder;
import com.restfb.types.DeviceCode;
import org.junit.Test;

import static com.restfb.FacebookClient.AccessToken;
import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationTest {

    private static final String appId = "371017066972600";
    private static final String appSecret = "990f893fcd3bd433131d981838bdc0cc";
    private static final String appAccessToken = "371017066972600|1T2pBN_RqbGs05ecz63RSYOXreo";

    private static final String clientAccessToken = "371017066972600|433c1a2158fc57e34215a4958b2b5d28";

    private static final String deviceCode = "f2253b29d57f3c978fd97f8a9e420bc3";

    private static

    DefaultReactiveFacebookClient rootFacebookClient
            = DefaultReactiveFacebookClient.builder(Version.VERSION_2_9).build();

    DefaultReactiveFacebookClient clientFacebookClient
            = DefaultReactiveFacebookClient.builder(Version.VERSION_2_9).setAccessToken(clientAccessToken).build();

    @Test
    public void shouldObtainAppAccessToken(){
        AccessToken accessToken = rootFacebookClient.obtainAppAccessToken(appId, appSecret).block();
        assertThat(accessToken.getAccessToken()).isEqualTo(appAccessToken);
    }

    @Test
    public void shouldObtainDeviceAccessToken(){

        ScopeBuilder scopeBuilder = new ScopeBuilder();
        DeviceCode deviceCode = clientFacebookClient.fetchDeviceCode(scopeBuilder).block();

        System.out.println("go to https://www.facebook.com/device and enter there user code:" + deviceCode.getUserCode());

        AccessToken accessToken = null;
        while(accessToken == null) {
            try {
                accessToken = clientFacebookClient.obtainDeviceAccessToken(deviceCode.getCode()).block();
            } catch (FacebookOAuthException e) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }

        AccessToken extendedToken = clientFacebookClient.obtainExtendedAccessToken(
                appId, appSecret, accessToken.getAccessToken()).block();

        int debug = 0;
    }
}
