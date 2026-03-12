package civerooni.com.putman.core.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import civerooni.com.putman.core.logging.PutmanLogger;
import civerooni.com.putman.core.model.RequestModel;
import civerooni.com.putman.core.model.ResponseModel;

public class PutmanHttpClient {

    private static final PutmanLogger log = PutmanLogger.getLogger(PutmanHttpClient.class);

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private static final PutmanHttpClient INSTANCE = new PutmanHttpClient();

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private PutmanHttpClient() {}

    public static PutmanHttpClient getInstance() {
        return INSTANCE;
    }

    public CompletableFuture<ResponseModel> send(RequestModel request) {
        if (request.getUrl() == null || request.getUrl().isBlank()) {
            log.warn("Send called with no URL");
            return CompletableFuture.completedFuture(ResponseModel.error("No URL specified."));
        }

        URI uri;
        try {
            uri = URI.create(request.getUrl());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid URL '" + request.getUrl() + "': " + e.getMessage());
            return CompletableFuture.completedFuture(
                    ResponseModel.error("Invalid URL: " + e.getMessage()));
        }

        if (uri.getScheme() == null) {
            String msg = "Invalid URL: missing scheme (e.g. http://" + request.getUrl() + ")";
            log.warn(msg);
            return CompletableFuture.completedFuture(ResponseModel.error(msg));
        }

        HttpRequest.Builder builder;
        try {
            builder = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(REQUEST_TIMEOUT);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid URL '" + request.getUrl() + "': " + e.getMessage());
            return CompletableFuture.completedFuture(
                    ResponseModel.error("Invalid URL: " + e.getMessage()));
        }

        request.getHeaders().stream()
                .filter(h -> h.isEnabled())
                .forEach(h -> builder.header(h.getKey(), h.getValue()));

        String bodyStr = request.getBody();
        HttpRequest.BodyPublisher bodyPublisher = (bodyStr != null && !bodyStr.isEmpty())
                ? HttpRequest.BodyPublishers.ofString(bodyStr)
                : HttpRequest.BodyPublishers.noBody();

        switch (request.getMethod()) {
            case GET     -> builder.GET();
            case DELETE  -> builder.DELETE();
            case POST    -> builder.POST(bodyPublisher);
            case PUT     -> builder.PUT(bodyPublisher);
            case PATCH   -> builder.method("PATCH",   bodyPublisher);
            case HEAD    -> builder.method("HEAD",    HttpRequest.BodyPublishers.noBody());
            case OPTIONS -> builder.method("OPTIONS", HttpRequest.BodyPublishers.noBody());
        }

        HttpRequest httpRequest = builder.build();
        Instant start = Instant.now();

        log.debug("--> " + request.getMethod() + " " + uri
                + " (connect timeout: " + CONNECT_TIMEOUT.toSeconds()
                + "s, request timeout: " + REQUEST_TIMEOUT.toSeconds() + "s)");

        return client.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    long ms = Duration.between(start, Instant.now()).toMillis();
                    log.debug("<-- " + response.statusCode() + " " + uri + " (" + ms + "ms)");
                    log.trace("Response body: " + response.body());
                    return new ResponseModel(response.statusCode(), response.body());
                })
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    long ms = Duration.between(start, Instant.now()).toMillis();
                    log.error("<-- " + request.getMethod() + " " + uri
                            + " after " + ms + "ms: " + cause.getMessage(), cause);
                    return ResponseModel.error(cause.getMessage());
                });
    }
}
