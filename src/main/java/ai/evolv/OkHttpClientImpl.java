package ai.evolv;

import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class OkHttpClientImpl implements HttpClient {

    private final OkHttpClient client;

    public OkHttpClientImpl(long timeout) {
        this.client = new OkHttpClient.Builder()
                .callTimeout(timeout, TimeUnit.MILLISECONDS)
                .connectionPool(new ConnectionPool(3, 1000, TimeUnit.MILLISECONDS))
                .build();
    }

    public CompletableFuture<String> get(String url) {
        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        final Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                responseFuture.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) {
                String body = "";
                try (ResponseBody responseBody = response.body()) {
                    if (responseBody != null) {
                        body = responseBody.string();
                    }

                    if (!response.isSuccessful()) {
                        throw new IOException(String.format("Unexpected response when making GET request: %s" +
                                " using url: %s with body: %s", response, request.url(), body));
                    }

                    responseFuture.complete(body);
                } catch (Exception e) {
                    responseFuture.completeExceptionally(e);
                }
            }
        });

        return responseFuture;
    }
}
