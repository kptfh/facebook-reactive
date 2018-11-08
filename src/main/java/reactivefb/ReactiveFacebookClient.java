package reactivefb;

import com.restfb.Parameter;
import com.restfb.batch.BatchRequest;
import com.restfb.batch.BatchResponse;
import com.restfb.exception.FacebookException;
import com.restfb.exception.FacebookOAuthException;
import com.restfb.scope.ScopeBuilder;
import com.restfb.types.DeviceCode;
import reactivefb.json.types.Connection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.restfb.FacebookClient.AccessToken;
import static com.restfb.FacebookClient.DebugTokenInfo;

/**
 * Specifies how a <a href="http://developers.facebook.com/docs/api">Facebook Graph API</a> client must operate.
 * <p>
 * If you'd like to...
 *
 * <ul>
 * <li>Fetch an object: use {@link #fetchObject(String, Class, Parameter...)} or
 * {@link #fetchObjects(List, Class, Parameter...)}</li>
 * <li>Fetch a connection: use {@link #fetchConnection(String, Class, Parameter...)}</li>
 * <li>Execute operations in batch: use {@link #executeBatch(List, List)}</li>
 * <li>Publish data: use {@link #publish(String, Class, Parameter...)} or
 * {@link #publish(String, Class, List, Parameter...)}</li>
 * <li>Delete an object: use {@link #deleteObject(String, Parameter...)}</li>
 * </ul>
 *
 * <p>
 * You may also perform some common access token operations. If you'd like to...
 *
 * <ul>
 * <li>Extend the life of an access token: use {@link #obtainExtendedAccessToken(String, String, String)}</li>
 * <li>Obtain an access token for use on behalf of an application instead of a user, use
 * {@link #obtainAppAccessToken(String, String)}.</li>
 * <li>Convert old-style session keys to OAuth access tokens: use
 * {@link #convertSessionKeysToAccessTokens(String, String, String...)}</li>
 * </ul>
 *
 * @author Srgii Karpenko
 */
public interface ReactiveFacebookClient {
  /**
   * Fetches a single <a href="http://developers.facebook.com/docs/reference/api/">Graph API object</a>, mapping the
   * result to an instance of {@code objectType}.
   *
   * @param <T>
   *          Java type to map to.
   * @param object
   *          ID of the object to fetch, e.g. {@code "me"}.
   * @param objectType
   *          Object type token.
   * @param parameters
   *          URL parameters to include in the API call (optional).
   * @return An instance of type {@code objectType} which contains the requested object's data.
   * @throws FacebookException
   *           If an error occurs while performing the API call.
   */
  <T> Mono<T> fetchObject(String object, Class<T> objectType, Parameter... parameters);

  /**
   * Fetches multiple <a href="http://developers.facebook.com/docs/reference/api/">Graph API objects</a> in a single
   * call, mapping the results to an instance of {@code objectType}.
   * <p>
   * You'll need to write your own container type ({@code objectType}) to hold the results. See
   * <a href="http://restfb.com">http://restfb.com</a> for an example of how to do this.
   *
   * @param <T>
   *          Java type to map to.
   * @param ids
   *          IDs of the objects to fetch, e.g. {@code "me", "arjun"}.
   * @param objectType
   *          Object type token.
   * @param parameters
   *          URL parameters to include in the API call (optional).
   * @return An instance of type {@code objectType} which contains the requested objects' data.
   * @throws FacebookException
   *           If an error occurs while performing the API call.
   */
  <T> Mono<T> fetchObjects(List<String> ids, Class<T> objectType, Parameter... parameters);

  /**
   * Performs a <a href="http://developers.facebook.com/docs/api#deleting">Graph API delete</a> operation on the given
   * {@code object}.
   *
   * @param object
   *          The ID of the object to delete.
   * @param parameters
   *          URL parameters to include in the API call.
   * @return {@code true} if Facebook indicated that the object was successfully deleted, {@code false} otherwise.
   * @throws FacebookException
   *           If an error occurred while attempting to delete the object.
   */
  Mono<Boolean> deleteObject(String object, Parameter... parameters);

  /**
   * Fetches a Graph API {@code Connection} type, mapping the result to an instance of {@code connectionType}.
   *
   * @param <T>
   *          Java type to map to.
   * @param connection
   *          The name of the connection, e.g. {@code "me/feed"}.
   * @param connectionType
   *          Connection type token.
   * @param parameters
   *          URL parameters to include in the API call (optional).
   * @return An instance of type {@code connectionType} which contains the requested Connection's data.
   * @throws FacebookException
   *           If an error occurs while performing the API call.
   */
  <T> Mono<Connection<T>> fetchConnection(String connection, Class<T> connectionType, Parameter... parameters);

  /**
   * Fetches a previous/next page of a Graph API {@code Connection} type, mapping the result to an instance of
   * {@code connectionType}.
   *
   * @param <T>
   *          Java type to map to.
   * @param connectionPageUrl
   *          The URL of the connection page to fetch, usually retrieved via {@link Connection#getPreviousPageUrl()} or
   *          {@link Connection#getNextPageUrl()}.
   * @param connectionType
   *          Connection type token.
   * @return An instance of type {@code connectionType} which contains the requested Connection's data.
   * @throws FacebookException
   *           If an error occurs while performing the API call.
   */
  <T> Mono<Connection<T>> fetchConnectionPage(String connectionPageUrl, Class<T> connectionType);


  /**
   * Performs a <a href="http://developers.facebook.com/docs/api#publishing">Graph API publish</a> operation on the
   * given {@code connection}, mapping the result to an instance of {@code objectType}.
   *
   * @param <T>
   *          Java type to map to.
   * @param connection
   *          The Connection to publish to.
   * @param objectType
   *          Object type token.
   * @param parameters
   *          URL parameters to include in the API call.
   * @return An instance of type {@code objectType} which contains the Facebook response to your publish request.
   * @throws FacebookException
   *           If an error occurs while performing the API call.
   */
  <T> Mono<T> publish(String connection, Class<T> objectType, Parameter... parameters);

  /**
   * Performs a <a href="http://developers.facebook.com/docs/api#publishing">Graph API publish</a> operation on the
   * given {@code connection} and includes some files - photos, for example - in the publish request, and mapping the
   * result to an instance of {@code objectType}.
   *
   * @param <T>
   *          Java type to map to.
   * @param connection
   *          The Connection to publish to.
   * @param objectType
   *          Object type token.
   * @param binaryAttachments
   *          The files to include in the publish request.
   * @param parameters
   *          URL parameters to include in the API call.
   * @return An instance of type {@code objectType} which contains the Facebook response to your publish request.
   * @throws FacebookException
   *           If an error occurs while performing the API call.
   */
  <T> Mono<T> publish(String connection, Class<T> objectType, List<BinaryAttachment> binaryAttachments,
                Parameter... parameters);

  /**
   * Executes operations as a batch using the <a href="https://developers.facebook.com/docs/reference/api/batch/">Batch
   * API</a>.
   *
   * @param batchRequests
   *          The operations to execute.
   * @return The execution results in the order in which the requests were specified.
   */
  Flux<BatchResponse> executeBatch(List<BatchRequest> batchRequests);

  /**
   * Executes operations as a batch with binary attachments using the
   * <a href="https://developers.facebook.com/docs/reference/api/batch/">Batch API</a>.
   *
   * @param batchRequests
   *          The operations to execute.
   * @param binaryAttachments
   *          Binary attachments referenced by the batch requests.
   * @return The execution results in the order in which the requests were specified.
   * @since 1.6.5
   */
  Flux<BatchResponse> executeBatch(List<BatchRequest> batchRequests, List<BinaryAttachment> binaryAttachments);

  /**
   * Converts an arbitrary number of {@code sessionKeys} to OAuth access tokens.
   * <p>
   * See the <a href="http://developers.facebook.com/docs/guides/upgrade">Facebook Platform Upgrade Guide</a> for
   * details on how this process works and why you should convert your application's session keys if you haven't
   * already.
   *
   * @param appId
   *          A Facebook application ID.
   * @param secretKey
   *          A Facebook application secret key.
   * @param sessionKeys
   *          The Old REST API session keys to be converted to OAuth access tokens.
   * @return A list of access tokens ordered to correspond to the {@code sessionKeys} argument list.
   * @throws FacebookException
   *           If an error occurs while attempting to convert the session keys to API keys.
   * @since 1.6
   */
  Flux<AccessToken> convertSessionKeysToAccessTokens(String appId, String secretKey, String... sessionKeys);

  /**
   * Obtains an access token which can be used to perform Graph API operations on behalf of a user.
   * <p>
   * See <a href="https://developers.facebook.com/docs/facebook-login/access-tokens">Access Tokens</a>.
   *
   * @param appId
   *          The ID of the app for which you'd like to obtain an access token.
   * @param appSecret
   *          The secret for the app for which you'd like to obtain an access token.
   * @param redirectUri
   *          The redirect URI which was used to obtain the {@code verificationCode}.
   * @param verificationCode
   *          The verification code in the Graph API callback to the redirect URI.
   * @return The access token for the user identified by {@code appId}, {@code appSecret}, {@code redirectUri} and
   *         {@code verificationCode}.
   * @throws FacebookException
   *           If an error occurs while attempting to obtain an access token.
   * @since 1.8.0
   */
  Mono<AccessToken> obtainUserAccessToken(String appId, String appSecret, String redirectUri, String verificationCode);

  /**
   * Obtains an access token which can be used to perform Graph API operations on behalf of an application instead of a
   * user.
   * <p>
   * See <a href="https://developers.facebook.com/docs/authentication/applications/" >Facebook's authenticating as an
   * app documentation</a>.
   *
   * @param appId
   *          The ID of the app for which you'd like to obtain an access token.
   * @param appSecret
   *          The secret for the app for which you'd like to obtain an access token.
   * @return The access token for the application identified by {@code appId} and {@code appSecret}.
   * @throws FacebookException
   *           If an error occurs while attempting to obtain an access token.
   * @since 1.6.10
   */
  Mono<AccessToken> obtainAppAccessToken(String appId, String appSecret);

  /**
   * Obtains an extended access token for the given existing, non-expired, short-lived access_token.
   * <p>
   * See <a href="https://developers.facebook.com/roadmap/offline-access-removal/#extend_token">Facebook's extend access
   * token documentation</a>.
   *
   * @param appId
   *          The ID of the app for which you'd like to obtain an extended access token.
   * @param appSecret
   *          The secret for the app for which you'd like to obtain an extended access token.
   * @param accessToken
   *          The non-expired, short-lived access token to extend.
   * @return An extended access token for the given {@code accessToken}.
   * @throws FacebookException
   *           If an error occurs while attempting to obtain an extended access token.
   * @since 1.6.10
   */
  Mono<AccessToken> obtainExtendedAccessToken(String appId, String appSecret, String accessToken);


  /**
   * Convenience method which invokes {@link #obtainExtendedAccessToken(String, String, String)} with the current access
   * token.
   *
   * @param appId
   *          The ID of the app for which you'd like to obtain an extended access token.
   * @param appSecret
   *          The secret for the app for which you'd like to obtain an extended access token.
   * @return An extended access token for the given {@code accessToken}.
   * @throws FacebookException
   *           If an error occurs while attempting to obtain an extended access token.
   * @throws IllegalStateException
   *           If this instance was not constructed with an access token.
   * @since 1.6.10
   */
  Mono<AccessToken> obtainExtendedAccessToken(String appId, String appSecret);

   /**
   * Method to initialize the device access token generation.
   *
   * You receive a {@link DeviceCode} instance and have to show the user the {@link DeviceCode#getVerificationUri()} and
   * the {@link DeviceCode#getUserCode()}. The user have to enter the user code at the verification url.
   *
   * Save the {@link DeviceCode#getCode()} to use it later, when polling Facebook with the
   * {@link #obtainDeviceAccessToken(String)} method.
   *
   * @param scope
   *          List of Permissions to request from the person using your app.
   * @return Instance of {@code DeviceCode} including the information to obtain the Device access token
   */
  Mono<DeviceCode> fetchDeviceCode(ScopeBuilder scope);

  /**
   * Method to poll Facebook and fetch the Device Access Token.
   *
   * You have to use this method to check if the user confirms the authorization.
   *
   * {@link FacebookOAuthException} can be thrown if the authorization is declined or still pending.
   *
   * @param code
   *          The device
   * @return An extended access token for the given {@link AccessToken}.
   * @since 1.12.0
   */
  Mono<AccessToken> obtainDeviceAccessToken(String code);

  /**
   * <p>
   * When working with access tokens, you may need to check what information is associated with them, such as its user
   * or expiry. To get this information you can use the debug tool in the developer site, or you can use this function.
   * </p>
   *
   * <p>
   * You must instantiate your ReactiveFacebookClient using your App Access Token, or a valid User Access Token from a developer
   * of the app.
   * </p>
   *
   * <p>
   * Note that if your app is set to Native/Desktop in the Advanced settings of your App Dashboard, the underlying
   * GraphAPI endpoint will not work with your app token unless you change the "App Secret in Client" setting to NO. If
   * you do not see this setting, make sure your "App Type" is set to Native/Desktop and then press the save button at
   * the bottom of the page. This will not affect apps set to Web.
   * </p>
   *
   * <p>
   * The response of the API call is a JSON array containing data and a map of fields. For example:
   * </p>
   *
   * <pre>
   * {@code
   * {
   *     "data": {
   *         "app_id": 138483919580948,
   *         "application": "Social Cafe",
   *         "expires_at": 1352419328,
   *         "is_valid": true,
   *         "issued_at": 1347235328,
   *         "metadata": {
   *             "sso": "iphone-safari"
   *         },
   *         "scopes": [
   *             "email",
   *             "publish_actions"
   *         ],
   *         "user_id": 1207059
   *     }
   * }
   * }
   * </pre>
   *
   * <p>
   * Note that the {@code issued_at} field is not returned for short-lived access tokens.
   * </p>
   *
   * <p>
   * See <a href="https://developers.facebook.com/docs/howtos/login/debugging-access-tokens/"> Debugging an Access
   * Token</a>
   * </p>
   *
   * @param inputToken
   *          The Access Token to debug.
   *
   * @return A JsonObject containing the debug information for the accessToken.
   * @since 1.6.13
   */
  Mono<DebugTokenInfo> debugToken(String inputToken);

}
