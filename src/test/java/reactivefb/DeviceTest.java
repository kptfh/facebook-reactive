package reactivefb;

import com.restfb.Version;
import com.restfb.exception.devicetoken.FacebookDeviceTokenPendingException;
import com.restfb.scope.ScopeBuilder;
import com.restfb.types.DeviceCode;
import org.junit.Test;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.restfb.FacebookClient.AccessToken;
import static org.assertj.core.api.Assertions.assertThat;

public class DeviceTest {

    private static final String appId = "371017066972600";
    private static final String appSecret = "990f893fcd3bd433131d981838bdc0cc";

    private static final String clientAccessToken = "371017066972600|433c1a2158fc57e34215a4958b2b5d28";

    DefaultReactiveFacebookClient clientFacebookClient
            = DefaultReactiveFacebookClient.builder(Version.VERSION_2_9).setAccessToken(clientAccessToken).build();

    @Test
    public void shouldObtainDeviceAccessToken(){

        Mono<AccessToken> extendedDeviceAccessToken = clientFacebookClient.fetchDeviceCode( new ScopeBuilder())
                //get access token
                .flatMap(deviceCode -> {
                    System.out.println("go to https://www.facebook.com/device and enter there user code:" + deviceCode.getUserCode());
                    return clientFacebookClient.obtainDeviceAccessToken(deviceCode.getCode())
                            .retryWhen(throwableFlux -> throwableFlux.flatMap(throwable -> {
                                if(throwable instanceof FacebookDeviceTokenPendingException){
                                    return Mono.delay(Duration.ofSeconds(5)).map(l -> throwable);
                                } else {
                                    throw Exceptions.propagate(throwable);
                                }
                            }));
                })
                //get extended token
                .flatMap(accessToken -> clientFacebookClient.obtainExtendedAccessToken(
                        appId, appSecret, accessToken.getAccessToken()));

        StepVerifier.create(extendedDeviceAccessToken)
                .assertNext(accessToken -> assertThat(accessToken.getAccessToken()).isNotEmpty())
                .verifyComplete();
    }
}
