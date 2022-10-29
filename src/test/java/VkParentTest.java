import java.util.Map;
import java.util.Optional;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.UserAuthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VkParentTest {
    private static final Logger logger = LoggerFactory.getLogger(VkParentTest.class);
    private static final String REDIRECT_URI = "https://oauth.vk.com/blank.html";
    protected VkApiClient client = this.createVkClient();

    protected VkApiClient createVkClient() {
        TransportClient transportClient = new HttpTransportClient();
        VkApiClient vk = new VkApiClient(transportClient);
        return vk;
    }

    private VkAuthData createAuthData() {
        Map<String, String> getenv = System.getenv();
        String clientId = getenv.get("clientId");
        String secret = getenv.get("secret");
        String code = getenv.get("code");
        return new VkAuthData(clientId, secret, code);
    }

    private Optional<UserActor> createUserActor() {
        Map<String, String> getenv = System.getenv();
        String token = getenv.get("accessToken");
        Integer userId = Integer.parseInt(getenv.get("userId"));
        if (token == null && userId == null) {
            return Optional.empty();
        }
        return Optional.of(new UserActor(userId, token));
    }

    protected Optional<UserActor> createUserActor(VkApiClient vk) {
        Optional<UserActor> actor = this.createUserActor();
        return actor.or(() -> {
            VkAuthData data = this.createAuthData();
            UserAuthResponse authResponse = null;
            try {
                authResponse = vk.oAuth().userAuthorizationCodeFlow(data.getClientId(), data.getSecret(), REDIRECT_URI, data.getCode()).execute();
            }
            catch (ApiException | ClientException ex) {
                logger.error("Received exception on user auth flow" + ex.getMessage());
            }
            return authResponse == null
                ? Optional.empty()
                : Optional.of(new UserActor(authResponse.getUserId(), authResponse.getAccessToken()));
        });
    }
}

