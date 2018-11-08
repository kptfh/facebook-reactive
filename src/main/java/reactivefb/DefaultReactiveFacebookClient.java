package reactivefb;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.async_.JsonFactory;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.restfb.*;
import com.restfb.batch.BatchRequest;
import com.restfb.batch.BatchResponse;
import com.restfb.exception.FacebookException;
import com.restfb.exception.FacebookNetworkException;
import com.restfb.exception.FacebookOAuthException;
import com.restfb.exception.devicetoken.DeviceTokenExceptionFactory;
import com.restfb.exception.generator.DefaultFacebookExceptionGenerator;
import com.restfb.exception.generator.FacebookExceptionGenerator;
import com.restfb.scope.ScopeBuilder;
import com.restfb.types.DeviceCode;
import com.restfb.util.StringUtils;
import org.eclipse.jetty.client.HttpClient;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactivefb.json.types.Connection;
import reactivefb.json.types.DeleteResponse;
import reactivefeign.client.DelegatingReactiveHttpResponse;
import reactivefeign.client.ReactiveHttpResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static com.restfb.FacebookClient.AccessToken;
import static com.restfb.FacebookClient.DebugTokenInfo;
import static com.restfb.util.ObjectUtil.verifyParameterPresence;
import static com.restfb.util.StringUtils.*;
import static com.restfb.util.UrlUtils.replaceOrAddQueryParameter;
import static java.lang.String.format;
import static java.net.HttpURLConnection.*;
import static java.nio.ByteBuffer.wrap;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static reactivefb.Parameters.APP_SECRET_PROOF_PARAM_NAME;
import static reactivefb.json.FacebookPropertyNamingStrategy.FACEBOOK_NAMING_STRATEGY;

/**
 * Default reactive implementation of a <a href="http://developers.facebook.com/docs/api">Facebook Graph API</a> client.
 *
 * @author Sergii Karpenko
 */
public class DefaultReactiveFacebookClient implements ReactiveFacebookClient {

  private Logger logger = LoggerFactory.getLogger(DefaultReactiveFacebookClient.class);

  /**
   * Graph API access token.
   */
  protected String accessToken;

  /**
   * Graph API app secret.
   */
  private String appSecret;

  /**
   * facebook exception generator to convert Facebook error json into java exceptions
   */
  private FacebookExceptionGenerator exceptionGenerator;

  /**
   * holds the Facebook endpoint urls
   */
  private FacebookEndpoints facebookEndpointUrls = new DefaultFacebookEndpoints();

  /**
   * Reserved "multiple IDs" parameter name.
   */
  protected static final String IDS_PARAM_NAME = "ids";

  /**
   * Version of API endpoint.
   */
  protected final Version apiVersion;

  private ReactiveWebRequestor webRequestor;

  private final ObjectMapper objectMapper;

  private UtilityFacebookClient utilityFacebookClient;

  /**
   * Creates a Facebook Graph API client .
   *
   * @param apiVersion
   *          Version of the api endpoint
   * @param accessToken
 *          A Facebook OAuth access token.
   * @param appSecret
*          A Facebook application secret.
   * @param webRequestor
*          The {@link ReactiveWebRequestor} implementation to use for sending requests to the API endpoint.
   * @param objectMapper
   */
  protected DefaultReactiveFacebookClient(
          Version apiVersion, String accessToken, String appSecret,
          ReactiveWebRequestor webRequestor, FacebookExceptionGenerator exceptionGenerator, ObjectMapper objectMapper) {
    super();

    this.apiVersion = apiVersion;
    this.accessToken = accessToken;
    this.appSecret = appSecret;

    this.webRequestor = webRequestor;
    this.exceptionGenerator = exceptionGenerator;
    this.objectMapper = objectMapper;

    this.utilityFacebookClient = new UtilityFacebookClient(accessToken, appSecret, apiVersion);
  }


  @Override
  public <T> Mono<T> fetchObject(String object, Class<T> objectType, Parameter... parameters) {
    verifyParameterPresence("object", object);
    verifyParameterPresence("objectType", objectType);
    return makeGetRequest(object, objectType,null, parameters);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Mono<T> fetchObjects(List<String> ids, Class<T> containerType, Parameter... parameters) {
    verifyParameterPresence("ids", ids);
    verifyParameterPresence("connectionType", containerType);

    if (ids.isEmpty()) {
      throw new IllegalArgumentException("The list of IDs cannot be empty.");
    }

    for (String id : ids) {
      if (StringUtils.isBlank(id)) {
        throw new IllegalArgumentException("The list of IDs cannot contain blank strings.");
      }
    }

    for (Parameter parameter : parameters) {
      if (IDS_PARAM_NAME.equals(parameter.name)) {
        throw new IllegalArgumentException("You cannot specify the '" + IDS_PARAM_NAME + "' URL parameter yourself - "
            + "RestFB will populate this for you with " + "the list of IDs you passed to this method.");
      }
    }

    try {
      return makeGetRequest("", containerType,
              withAdditionalParameter(parameters, IDS_PARAM_NAME, objectMapper.writeValueAsString(ids)));
    } catch (JsonProcessingException e) {
      return Mono.error(e);
    }
  }

  @Override
  public Mono<Boolean> deleteObject(String object, Parameter... parameters) {
    verifyParameterPresence("object", object);

    return makeDeleteRequest(object, String.class, parameters)
            .map(responseString -> {
              try {
                DeleteResponse deleteResponse = objectMapper.readValue(responseString, DeleteResponse.class);
                if(deleteResponse.getSuccess() != null){
                  return Boolean.valueOf(deleteResponse.getSuccess());
                }
                if(deleteResponse.getResult() != null){
                  return deleteResponse.getResult().contains("Successfully deleted");
                }
              } catch (IOException e) {
                logger.debug("no valid JSON returned while deleting a object, using returned String instead", e);
              }
              return "true".equals(responseString);
            });
  }

  /**
   * @see com.restfb.FacebookClient#fetchConnection(String, Class, com.restfb.Parameter[])
   */
  @Override
  public <T> Mono<Connection<T>> fetchConnection(String connection, Class<T> connectionType, Parameter... parameters) {
    verifyParameterPresence("connection", connection);
    verifyParameterPresence("connectionType", connectionType);
    JavaType parametrizedType = objectMapper.getTypeFactory().constructParametricType(Connection.class, connectionType);
    TraceableResult<Connection> traceableResult = makeGetRequestTraceable(connection, Connection.class,
            objectMapper.readerFor(parametrizedType), parameters);

    return traceableResult.result.map(conn -> {
      conn.setUrl(traceableResult.url);
      return conn;
    });
  }


  /**
   * @see com.restfb.FacebookClient#fetchConnectionPage(String, Class)
   */
  @Override
  public <T> Mono<Connection<T>> fetchConnectionPage(String connectionPageUrl, Class<T> connectionType) {
    if (!isBlank(accessToken) && !isBlank(appSecret)) {
      connectionPageUrl = replaceOrAddQueryParameter(connectionPageUrl,
              APP_SECRET_PROOF_PARAM_NAME, utilityFacebookClient.obtainAppSecretProof(accessToken, appSecret));
    }

    JavaType parametrizedType = objectMapper.getTypeFactory().constructParametricType(Connection.class, connectionType);
    TraceableResult<Connection> traceableResult = makeGetRequestTraceable(connectionPageUrl, Connection.class,
            objectMapper.readerFor(parametrizedType));

    return traceableResult.result.map(conn -> {
      conn.setUrl(traceableResult.url);
      return conn;
    });
  }

  /**
   * @see com.restfb.FacebookClient#publish(String, Class, com.restfb.Parameter[])
   */
  @Override
  public <T> Mono<T> publish(String connection, Class<T> objectType, Parameter... parameters) {
    return publish(connection, objectType, null, parameters);
  }


  /**
   * @see com.restfb.FacebookClient#publish(String, Class, com.restfb.BinaryAttachment,
   *      com.restfb.Parameter[])
   */
  @Override
  public <T> Mono<T> publish(String connection, Class<T> objectType, List<BinaryAttachment> binaryAttachments,
      Parameter... parameters) {
    verifyParameterPresence("connection", connection);

    return makePostRequest(connection, objectType, binaryAttachments, parameters);
  }

  /**
   * @see com.restfb.FacebookClient#executeBatch(List)
   */
  @Override
  public Flux<BatchResponse> executeBatch(List<BatchRequest> batchRequests) {
    return executeBatch(batchRequests, emptyList());
  }

  /**
   * @see com.restfb.FacebookClient#executeBatch(List, List)
   */
  @Override
  public Flux<BatchResponse> executeBatch(List<BatchRequest> batchRequests, List<BinaryAttachment> binaryAttachments) {
    verifyParameterPresence("binaryAttachments", binaryAttachments);

    if (batchRequests == null || batchRequests.isEmpty()) {
      throw new IllegalArgumentException("You must specify at least one batch request.");
    }

    try {
      return makePostRequestFlux("", BatchResponse.class, binaryAttachments,
              Parameter.with("batch", objectMapper.writeValueAsString(batchRequests)));
    } catch (JsonProcessingException e){
      return Flux.error(e);
    }
  }




  /**
   * Coordinates the process of executing the API request GET and processing the response we receive from the
   * endpoint.
   *
   * @param endpoint
   *          Facebook Graph API endpoint.
   * @param parameters
   *          Arbitrary number of parameters to send along to Facebook as part of the API call.
   * @return object returned by Facebook for the API call.
   * @throws FacebookException
   *           If an error occurs while making the Facebook API POST or processing the response.
   */
  protected <T> Mono<T> makeGetRequest(String endpoint, Class<T> returnType, Parameter... parameters) {
    return makeGetRequest(endpoint, returnType, objectMapper.readerFor(returnType), parameters);
  }

  /**
   * Coordinates the process of executing the API request GET and processing the response we receive from the
   * endpoint.
   *
   * @param endpoint
   *          Facebook Graph API endpoint.
   * @param parameters
   *          Arbitrary number of parameters to send along to Facebook as part of the API call.
   * @return object returned by Facebook for the API call.
   * @throws FacebookException
   *           If an error occurs while making the Facebook API POST or processing the response.
   */
  protected <T> Mono<T> makeGetRequest(String endpoint,
                                       Class<T> returnType,
                                       ObjectReader objectReader,
                                       Parameter... parameters){
    return makeGetRequestTraceable(endpoint, returnType, objectReader, parameters).result;
  }


  /**
   * Coordinates the process of executing the API request GET and processing the response we receive from the
   * endpoint.
   *
   * @param endpoint
   *          Facebook Graph API endpoint.
   * @param parameters
   *          Arbitrary number of parameters to send along to Facebook as part of the API call.
   * @return object returned by Facebook for the API call.
   * @throws FacebookException
   *           If an error occurs while making the Facebook API POST or processing the response.
   */
  protected <T> TraceableResult<T> makeGetRequestTraceable(String endpoint,
                                    Class<T> returnType,
                                    ObjectReader objectReader,
                                    Parameter... parameters) {
    utilityFacebookClient.verifyParameterLegality(parameters);

    if (!endpoint.startsWith("/")) {
      endpoint = "/" + endpoint;
    }

    final String fullEndpoint = utilityFacebookClient.createEndpointForApiCall(endpoint, false);
    final String parameterString = utilityFacebookClient.toParameterString(parameters);
    String url = fullEndpoint + "?" + parameterString;

    Mono<ReactiveHttpResponse> response = webRequestor.executeGet(url, returnType, objectReader);

    Mono<T> result = processErrors(response).flatMap(reactiveHttpResponse -> (Mono<T>) reactiveHttpResponse.body());
    return new TraceableResult<>(result, url);
  }

  /**
   * Coordinates the process of executing the API request DELETE and processing the response we receive from the
   * endpoint.
   *
   * @param endpoint
   *          Facebook Graph API endpoint.
   * @param parameters
   *          Arbitrary number of parameters to send along to Facebook as part of the API call.
   * @return object returned by Facebook for the API call.
   * @throws FacebookException
   *           If an error occurs while making the Facebook API POST or processing the response.
   */
  protected <T> Mono<T> makeDeleteRequest(String endpoint,
                                    Class<T> returnType,
                                    Parameter... parameters) {
    utilityFacebookClient.verifyParameterLegality(parameters);

    if (!endpoint.startsWith("/")) {
      endpoint = "/" + endpoint;
    }

    final String fullEndpoint = utilityFacebookClient.createEndpointForApiCall(endpoint, false);
    final String parameterString = utilityFacebookClient.toParameterString(parameters);

    Mono<ReactiveHttpResponse> response = webRequestor.executeDelete(fullEndpoint + "?" + parameterString, returnType);
    return processErrors(response).flatMap(reactiveHttpResponse -> (Mono<T>)reactiveHttpResponse.body());
  }

  protected <T> Mono<T> makePostRequest(String endpoint,
                                    Class<T> returnType,
                                    final List<BinaryAttachment> binaryAttachments,
                                    Parameter... parameters) {
    return makePostRequest(endpoint, Mono.class, returnType, binaryAttachments, parameters)
            .flatMap(reactiveHttpResponse -> (Mono<T>)reactiveHttpResponse.body());
  }

  protected <T> Flux<T> makePostRequestFlux(String endpoint,
                                        Class<T> returnType,
                                        final List<BinaryAttachment> binaryAttachments,
                                        Parameter... parameters) {
    return makePostRequest(endpoint, Flux.class, returnType, binaryAttachments, parameters)
            .flatMapMany(reactiveHttpResponse -> (Flux<T>)reactiveHttpResponse.body());
  }


  /**
   * Coordinates the process of executing the API request POST and processing the response we receive from the
   * endpoint.
   *
   * @param endpoint
   *          Facebook Graph API endpoint.
   * @param binaryAttachments
   *          A list of binary files to include in a {@code POST} request. Pass {@code null} if no attachment should be
   *          sent.
   * @param parameters
   *          Arbitrary number of parameters to send along to Facebook as part of the API call.
   * @return object returned by Facebook for the API call.
   * @throws FacebookException
   *           If an error occurs while making the Facebook API POST or processing the response.
   */

  protected <T> Mono<ReactiveHttpResponse> makePostRequest(String endpoint,
                                        Class returnPublisherType,
                                        Class<T> returnType,
                                        final List<BinaryAttachment> binaryAttachments,
                                        Parameter... parameters) {
    utilityFacebookClient.verifyParameterLegality(parameters);

    if (!endpoint.startsWith("/")) {
      endpoint = "/" + endpoint;
    }

    final String fullEndpoint = utilityFacebookClient.createEndpointForApiCall(
            endpoint, binaryAttachments != null && !binaryAttachments.isEmpty());
    final String parameterString = utilityFacebookClient.toParameterString(parameters);

    Mono<ReactiveHttpResponse> response = binaryAttachments != null
            ? webRequestor.executePostWithAttachments(fullEndpoint, returnPublisherType, returnType, parameterString,
            binaryAttachments.toArray(new BinaryAttachment[0]))
            : webRequestor.executePost(fullEndpoint, returnPublisherType, returnType, parameterString);

    return processErrors(response);
  }

  public static final Set<Integer> CHECKED_ERROR_STATUSES = new HashSet<>(asList(
          HTTP_BAD_REQUEST,
          HTTP_UNAUTHORIZED,
          HTTP_NOT_FOUND,
          HTTP_INTERNAL_ERROR,
          HTTP_FORBIDDEN,
          HTTP_NOT_MODIFIED
  ));

  protected Mono<ReactiveHttpResponse> processErrors(Mono<ReactiveHttpResponse> response){
    return response.map(resp -> {
      int status = resp.status();
      if(status != HTTP_OK){
        if(!CHECKED_ERROR_STATUSES.contains(status)){
          return errorResponse(resp, bytes -> new FacebookNetworkException(status));
        } else {
          return errorResponse(resp, bytes -> {
            String errorJson = StandardCharsets.UTF_8.decode(wrap(bytes)).toString();
            // If the response contained an facebook error code, throw an exception.
            exceptionGenerator.throwFacebookResponseStatusExceptionIfNecessary(errorJson, status);
            return new FacebookNetworkException(status);
          });
        }
      } else {
        return resp;
      }
    });
  }

  private DelegatingReactiveHttpResponse errorResponse(ReactiveHttpResponse response,
                                                       Function<byte[], Throwable> errorDecoder) {
    return new DelegatingReactiveHttpResponse(response) {
      @Override
      public Publisher<?> body() {
        return response.bodyData().map(errorDecoder).flatMap(Mono::error);
      }
    };
  }

  @Override
  public Flux<AccessToken> convertSessionKeysToAccessTokens(String appId, String secretKey, String... sessionKeys) {
    verifyParameterPresence("appId", appId);
    verifyParameterPresence("secretKey", secretKey);

    if (sessionKeys == null || sessionKeys.length == 0) {
      return Flux.empty();
    }

    return makePostRequestFlux("/oauth/exchange_sessions", AccessToken.class, null,
            Parameter.with("client_id", appId),
            Parameter.with("client_secret", secretKey),
            Parameter.with("sessions", join(sessionKeys)));
  }

  @Override
  public Mono<AccessToken> obtainAppAccessToken(String appId, String appSecret) {
    verifyParameterPresence("appId", appId);
    verifyParameterPresence("appSecret", appSecret);

    return getAccessTokenFromResponse(
            makeGetRequest("oauth/access_token", String.class,
            Parameter.with("grant_type", "client_credentials"),
            Parameter.with("client_id", appId),
            Parameter.with("client_secret", appSecret)));
  }

  @Override
  public Mono<AccessToken> obtainUserAccessToken(String appId, String appSecret, String redirectUri,
                                                 String verificationCode) {
    verifyParameterPresence("appId", appId);
    verifyParameterPresence("appSecret", appSecret);
    verifyParameterPresence("verificationCode", verificationCode);

    return getAccessTokenFromResponse(
            makeGetRequest("oauth/access_token", String.class,
            Parameter.with("client_id", appId),
            Parameter.with("client_secret", appSecret),
            Parameter.with("code", verificationCode),
            Parameter.with("redirect_uri", redirectUri)));
  }

  @Override
  public Mono<AccessToken> obtainExtendedAccessToken(String appId, String appSecret) {
    if (accessToken == null) {
      throw new IllegalStateException(
        format("You cannot call this method because you did not construct this instance of %s with an access token.",
          getClass().getSimpleName()));
    }

    return obtainExtendedAccessToken(appId, appSecret, accessToken);
  }

  @Override
  public Mono<AccessToken> obtainExtendedAccessToken(String appId, String appSecret, String accessToken) {
    verifyParameterPresence("appId", appId);
    verifyParameterPresence("appSecret", appSecret);
    verifyParameterPresence("accessToken", accessToken);

    return getAccessTokenFromResponse(
            makeGetRequest("/oauth/access_token", String.class,
            Parameter.with("client_id", appId),
            Parameter.with("client_secret", appSecret),
            Parameter.with("grant_type", "fb_exchange_token"),
            Parameter.with("fb_exchange_token", accessToken)));
  }



  protected Mono<AccessToken> getAccessTokenFromResponse(Mono<String> tokenString){
    return tokenString.map(response -> {
      try {
        return objectMapper.readValue(response, AccessToken.class);
      } catch (IOException e) {
        logger.debug("could not map response to access token class try to fetch directly from String", e);
        return AccessToken.fromQueryString(response);
      }
    });
  }

  @Override
  public Mono<DeviceCode> fetchDeviceCode(ScopeBuilder scope) {
    verifyParameterPresence("scope", scope);

    if (accessToken == null) {
      throw new IllegalStateException("access token is required to fetch a device access token");
    }

    return makePostRequest("device/login", DeviceCode.class, null,
            Parameter.with("type", "device_code"),
            Parameter.with("scope", scope.toString()));
  }

  @Override
  public Mono<AccessToken> obtainDeviceAccessToken(String code) {
    verifyParameterPresence("code", code);

    if (accessToken == null) {
      throw new IllegalStateException("access token is required to fetch a device access token");
    }

    return getAccessTokenFromResponse(
            makePostRequest("device/login_status", String.class, null,
                    Parameter.with("type", "device_token"),
                    Parameter.with("code", code)))
            .onErrorMap(FacebookOAuthException.class, foae -> {
              try {
                DeviceTokenExceptionFactory.createFrom(foae);
                return foae;
              } catch (Exception e) {
                return e;
              }
            });
  }

  @Override
  public Mono<DebugTokenInfo> debugToken(String inputToken) {
    verifyParameterPresence("inputToken", inputToken);
    return makeGetRequest("/debug_token", DebugTokenInfo.class,
            Parameter.with("input_token", inputToken));
  }

  public static Builder builder(Version version){
    return new Builder(version);
  }

  private static class Builder{
    private final Version version;
    private HttpClient httpClient;
    private String accessToken;
    private String appSecret;
    private ObjectMapper objectMapper;
    private JsonFactory jsonFactory = new JsonFactory();

    private ReactiveWebRequestor webRequestor;

    private FacebookExceptionGenerator facebookExceptionGenerator = new DefaultFacebookExceptionGenerator();


    private Builder(Version version) {
      this.version = version;
    }

    public Builder setAccessToken(String accessToken) {
      this.accessToken = trimToNull(accessToken);
      return this;
    }

    public Builder setAppSecret(String appSecret) {
      this.appSecret = trimToNull(appSecret);
      return this;
    }

    public Builder setWebRequestor(ReactiveWebRequestor webRequestor) {
      this.webRequestor = webRequestor;
      return this;
    }

    public Builder setFacebookExceptionGenerator(FacebookExceptionGenerator facebookExceptionGenerator) {
      this.facebookExceptionGenerator = facebookExceptionGenerator;
      return this;
    }

    public Builder setHttpClient(HttpClient httpClient) {
      this.httpClient = httpClient;
      return this;
    }

    public Builder setObjectMapper(ObjectMapper objectMapper) {
      this.objectMapper = objectMapper;
      return this;
    }

    public Builder setJsonFactory(JsonFactory jsonFactory) {
      this.jsonFactory = jsonFactory;
      return this;
    }

    public DefaultReactiveFacebookClient build(){

      if(httpClient == null){
        httpClient = new HttpClient();
        try {
          httpClient.start();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }

      if(objectMapper == null){
        objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(FACEBOOK_NAMING_STRATEGY);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
      }

      if(webRequestor == null){
        webRequestor = new DefaultReactiveWebRequestor(httpClient, jsonFactory, objectMapper);
      }

      return new DefaultReactiveFacebookClient(version, accessToken, appSecret,
              webRequestor, facebookExceptionGenerator, objectMapper);
    }
  }

  public Parameter[] withAdditionalParameter(Parameter[] parameters, String name, String value){
    return utilityFacebookClient.withAdditionalParameter(Parameter.with(IDS_PARAM_NAME,
            value), parameters);
  }

  private static class UtilityFacebookClient extends DefaultFacebookClient {

    public UtilityFacebookClient(String accessToken, String appSecret, Version apiVersion) {
      super(accessToken, appSecret, new DefaultWebRequestor(), new DefaultJsonMapper(), apiVersion);
    }

    @Override
    public void verifyParameterLegality(Parameter... parameters) {
      super.verifyParameterLegality(parameters);
    }

    @Override
    public String createEndpointForApiCall(String apiCall, boolean hasAttachment) {
      return super.createEndpointForApiCall(apiCall, hasAttachment);
    }

    @Override
    public String toParameterString(Parameter... parameters) {
      return super.toParameterString(parameters);
    }

    @Override
    public String toParameterString(boolean withJsonParameter, Parameter... parameters) {
      return super.toParameterString(withJsonParameter, parameters);
    }

    public Parameter[] withAdditionalParameter(Parameter parameter, Parameter... parameters) {
      return super.parametersWithAdditionalParameter(parameter, parameters);
    }
  }

  private static class TraceableResult<T> {
    final Mono<T> result;
    final String url;

    private TraceableResult(Mono<T> result, String url) {
      this.result = result;
      this.url = url;
    }
  }

}
