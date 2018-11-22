package reactivefb;

import com.restfb.Parameter;
import com.restfb.Version;
import com.restfb.types.TestUser;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static reactivefb.ApplicationTest.getAppAccessToken;
import static reactivefb.ApplicationTest.getAppId;
import static reactivefb.ApplicationTest.getVersion;
import static reactivefb.Parameters.ACCESS_TOKEN;
import static reactivefb.Parameters.INSTALLED;

public class UserTest {

    private static DefaultReactiveFacebookClient appFacebookClient
            = DefaultReactiveFacebookClient.builder(getVersion())
            .setAccessToken(getAppAccessToken())
            .build();

    @Test
    public void shouldCreateAndDeleteTestUser(){
        Mono<Boolean> deleted = createTestUser()
                .flatMap(testUser -> deleteTestUser(testUser));

        StepVerifier.create(deleted)
                .assertNext(result -> assertThat(result).isTrue())
                .verifyComplete();
    }

    public static Mono<Boolean> deleteTestUser(TestUser testUser) {
        return appFacebookClient.deleteObject(testUser.getId());
    }

    public static Mono<TestUser> createTestUser() {
        return appFacebookClient.publish(getAppId()+"/accounts/test-users", TestUser.class,
                Parameter.with(INSTALLED, "true"));
    }


}
