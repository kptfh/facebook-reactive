package reactivefb;

import com.fasterxml.jackson.core.async_.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.eclipse.jetty.client.HttpClient;
import org.reactivestreams.Publisher;
import reactivefeign.client.ReactiveHttpRequest;
import reactivefeign.client.ReactiveHttpResponse;
import reactivefeign.jetty.client.JettyReactiveHttpClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.apache.commons.httpclient.params.HttpMethodParams.MULTIPART_BOUNDARY;

/**
 * @author Sergii Karpenko
 */


public class DefaultReactiveWebRequestor implements ReactiveWebRequestor{

    /**
     * Line separator for multipart {@code POST}s.
     */
    private static final String MULTIPART_CARRIAGE_RETURN_AND_NEWLINE = "\r\n";

    /**
     * Hyphens for multipart {@code POST}s.
     */
    private static final String MULTIPART_TWO_HYPHENS = "--";

    private final HttpClient httpClient;
    private final JsonFactory jsonFactory;
    private final ObjectMapper objectMapper;

    public DefaultReactiveWebRequestor(HttpClient httpClient, JsonFactory jsonFactory, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.jsonFactory = jsonFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<ReactiveHttpResponse> executeGet(String url, Class responseBodyType, ObjectReader objectReader) {
        try {
            return new JettyReactiveHttpClient(httpClient, null, Mono.class, responseBodyType,
                    jsonFactory, null, objectReader)
                    .executeRequest(new ReactiveHttpRequest("get", new URI(url), emptyMap(), null));
        } catch (URISyntaxException e) {
            return Mono.error(e);
        }
    }

    @Override
    public Mono<ReactiveHttpResponse> executeDelete(String url, Class responseBodyType) {
        try {
            return new JettyReactiveHttpClient(httpClient, null, Mono.class, responseBodyType,
                    jsonFactory, null, objectMapper.readerFor(responseBodyType))
                    .executeRequest(new ReactiveHttpRequest("delete", new URI(url), emptyMap(), null));
        } catch (URISyntaxException e) {
            return Mono.error(e);
        }
    }

    @Override
    public Mono<ReactiveHttpResponse> executePost(String url, Class responsePublisherType, Class responseBodyType, String parameters) {

        try {
            ReactiveHttpRequest postRequest = new ReactiveHttpRequest("post", new URI(url), emptyMap(),
                    Mono.just(parameters));

            return new JettyReactiveHttpClient(httpClient, String.class, responsePublisherType, responseBodyType,
                    jsonFactory, null, objectMapper.readerFor(responseBodyType))
                    .executeRequest(postRequest);
        } catch (URISyntaxException e) {
            return Mono.error(e);
        }
    }

    @Override
    public Mono<ReactiveHttpResponse> executePostWithAttachments(
            String url, Class responsePublisherType, Class responseBodyType,
            String parameters, BinaryAttachment... binaryAttachments) {

        if(binaryAttachments == null || binaryAttachments.length == 0){
            throw new IllegalArgumentException("Use executePost method");
        }

        url = url + parameters;

        try {
            Map<String, List<String>> headers = new HashMap<String, List<String>>() {{
                put("Connection", singletonList("Keep-Alive"));
                put("Content-Type", singletonList("multipart/form-data;boundary=" + MULTIPART_BOUNDARY));
            }};

            Publisher<Object> body = Flux.fromIterable(asList(binaryAttachments))
                    .flatMap(binaryAttachment ->
                            Mono.just(UTF_8.encode(attachmentPrefix(binaryAttachment)))
                                    .concatWith(binaryAttachment.getData())
                                    .concatWith(Mono.just(UTF_8.encode(attachmentSuffix()))));

            ReactiveHttpRequest postRequest = new ReactiveHttpRequest("post", new URI(url), headers, body);

            return new JettyReactiveHttpClient(httpClient, ByteBuffer.class, responsePublisherType, responseBodyType,
                    jsonFactory, null, objectMapper.readerFor(responseBodyType))
                    .executeRequest(postRequest);
        } catch (URISyntaxException e) {
            return Mono.error(e);
        }
    }

    private String attachmentSuffix() {
        return MULTIPART_CARRIAGE_RETURN_AND_NEWLINE + MULTIPART_TWO_HYPHENS + MULTIPART_BOUNDARY
                + MULTIPART_TWO_HYPHENS + MULTIPART_CARRIAGE_RETURN_AND_NEWLINE;
    }

    private String attachmentPrefix(BinaryAttachment binaryAttachment) {
        return MULTIPART_TWO_HYPHENS + MULTIPART_BOUNDARY +
                MULTIPART_CARRIAGE_RETURN_AND_NEWLINE + "Content-Disposition: form-data; name=\"" +
                createFormFieldName(binaryAttachment) + "\"; filename=\"" +
                binaryAttachment.getFilename() + "\"" +
                MULTIPART_CARRIAGE_RETURN_AND_NEWLINE + "Content-Type: " +
                binaryAttachment.getContentType() +
                MULTIPART_CARRIAGE_RETURN_AND_NEWLINE + MULTIPART_CARRIAGE_RETURN_AND_NEWLINE;
    }

    /**
     * Creates the form field name for the binary attachment filename by stripping off the file extension - for example,
     * the filename "test.png" would return "test".
     *
     * @param binaryAttachment
     *          The binary attachment for which to create the form field name.
     * @return The form field name for the given binary attachment.
     */
    protected String createFormFieldName(BinaryAttachment binaryAttachment) {
        if (binaryAttachment.getFieldName() != null) {
            return binaryAttachment.getFieldName();
        }

        String name = binaryAttachment.getFilename();
        int fileExtensionIndex = name.lastIndexOf('.');
        return fileExtensionIndex > 0 ? name.substring(0, fileExtensionIndex) : name;
    }
}
