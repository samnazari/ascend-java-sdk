package ai.evolv;

import org.asynchttpclient.*;
import static org.asynchttpclient.Dsl.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class AsyncHttpClientImpl implements HttpClient {

    private final AsyncHttpClient httpClient;

    public AsyncHttpClientImpl(long timeout) {
        this.httpClient = asyncHttpClient(config()
        .setRequestTimeout(new Long(timeout).intValue()));
    }


    public CompletableFuture<String> get(String url) {

        final CompletableFuture<String> responseFuture = new CompletableFuture<>();

        StringBuilder chunks = new StringBuilder();

        httpClient.prepareGet(url)
                .execute(new AsyncHandler<String>() {
                    private Integer status;

                    @Override
                    public State onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
                        int code = responseStatus.getStatusCode();
                        if (code >= 200 && code < 300) {
                            return State.CONTINUE;
                        }
                        throw new IOException("The request returned a bad status code.");
                    }

                    @Override
                    public State onHeadersReceived(HttpResponseHeaders headers) throws Exception {
                        return State.CONTINUE;
                    }

                    @Override
                    public State onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
                        String chunk = new String(bodyPart.getBodyPartBytes()).trim();
                        if (chunk.length() != 0) {
                            chunks.append(chunk);
                        }
                        return State.CONTINUE;
                    }

                    @Override
                    public String onCompleted() throws Exception {
                        String response = chunks.toString();
                        responseFuture.complete(response);
                        return response;
                    }

                    @Override
                    public void onThrowable(Throwable t) {
                        responseFuture.completeExceptionally(t);
                    }
                });

        return responseFuture;
    }

}
