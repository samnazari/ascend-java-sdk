package ai.evolv.utils;

import ai.evolv.HttpClient;

import java.util.concurrent.CompletableFuture;

public class MockHttpClient implements HttpClient {

    private final String responseBody;

    public MockHttpClient(String responseBody) {
        this.responseBody = responseBody;
    }

    @Override
    public CompletableFuture<String> get(String url) {
        CompletableFuture<String> futureResponse = new CompletableFuture<>();
        futureResponse.complete(responseBody);

        return futureResponse;
    }

}
