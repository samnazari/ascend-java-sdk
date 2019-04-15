package ai.evolv;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OkHttpClientImpl implements HttpClient {

    private final OkHttpClient client;

    /**
     * Initializes the OhHttp# client.
     * @param timeout specify a request timeout for the client.
     */
    public OkHttpClientImpl(long timeout) {
        this.client = new OkHttpClient.Builder()
                .callTimeout(timeout, TimeUnit.MILLISECONDS)
                .connectionPool(new ConnectionPool(3, 1000, TimeUnit.MILLISECONDS))
                .build();
    }

    /**
     * Performs a GET request with the given url using the client from
     * okhttp3.
     * @param url a valid url representing a call to the Participant API.
     * @return a Completable future instance containing a response from
     *     the API
     */
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
                        throw new IOException(String.format("Unexpected response " +
                                "when making GET request: %s using url: %s with body: %s",
                                response, request.url(), body));
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
