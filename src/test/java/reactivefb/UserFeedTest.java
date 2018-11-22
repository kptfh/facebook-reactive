package reactivefb;

import com.restfb.types.TestUser;

import static reactivefb.ApplicationTest.getVersion;
import static reactivefb.UserTest.createTestUser;

public class UserFeedTest {

    private static TestUser testUser = createTestUser().block();

    private static DefaultReactiveFacebookClient appFacebookClient
            = DefaultReactiveFacebookClient.builder(getVersion())
            .setAccessToken(testUser.getAccessToken())
            .build();

    public static void publishPost(){
        appFacebookClient.publish(testUser.getId(), )
    }

}
