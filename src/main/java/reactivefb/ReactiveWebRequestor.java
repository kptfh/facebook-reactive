package reactivefb;

import com.fasterxml.jackson.databind.ObjectReader;
import reactivefeign.client.ReactiveHttpResponse;
import reactor.core.publisher.Mono;

public interface ReactiveWebRequestor {

    Mono<ReactiveHttpResponse> executeGet(String url, Class responseBodyType, ObjectReader objectReader);

    Mono<ReactiveHttpResponse> executeDelete(String url, Class responseBodyType);

    Mono<ReactiveHttpResponse> executePost(String url, Class responsePublisherType, Class responseBodyType, String parameters);

    Mono<ReactiveHttpResponse> executePostWithAttachments(
            String url, Class responsePublisherType, Class responseBodyType,
            String parameters, BinaryAttachment... binaryAttachments);
}
