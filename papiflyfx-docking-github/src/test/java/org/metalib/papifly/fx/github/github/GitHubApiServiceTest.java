package org.metalib.papifly.fx.github.github;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.github.model.PullRequestDraft;
import org.metalib.papifly.fx.github.model.PullRequestResult;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GitHubApiServiceTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void fetchDefaultBranchParsesResponse() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/repos/org/repo", exchange -> respondJson(exchange, 200, "{\"default_branch\":\"main\"}"));
        server.start();

        GitHubApiService service = new GitHubApiService(
            HttpClient.newHttpClient(),
            Optional::empty,
            URI.create("http://localhost:" + server.getAddress().getPort()),
            1
        );

        String branch = service.fetchDefaultBranch("org", "repo");

        assertEquals("main", branch);
    }

    @Test
    void createPullRequestParsesResponse() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/repos/org/repo/pulls", exchange -> respondJson(exchange, 201,
            "{\"number\":42,\"html_url\":\"https://github.com/org/repo/pull/42\"}"));
        server.start();

        GitHubApiService service = new GitHubApiService(
            HttpClient.newHttpClient(),
            Optional::empty,
            URI.create("http://localhost:" + server.getAddress().getPort()),
            1
        );

        PullRequestResult result = service.createPullRequest("org", "repo",
            new PullRequestDraft("title", "body", "feature", "main", false));

        assertEquals(42, result.number());
        assertEquals("https://github.com/org/repo/pull/42", result.url().toString());
    }

    @Test
    void mapsAuthFailure() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/repos/org/repo", exchange -> respondJson(exchange, 401, "{}"));
        server.start();

        GitHubApiService service = new GitHubApiService(
            HttpClient.newHttpClient(),
            Optional::empty,
            URI.create("http://localhost:" + server.getAddress().getPort()),
            1
        );

        GitHubApiException exception = assertThrows(GitHubApiException.class,
            () -> service.fetchDefaultBranch("org", "repo"));

        assertEquals(GitHubApiException.Category.AUTHENTICATION, exception.category());
        assertEquals(401, exception.statusCode());
    }

    @Test
    void mapsValidationFailure() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/repos/org/repo/pulls", exchange -> respondJson(exchange, 422, "{}"));
        server.start();

        GitHubApiService service = new GitHubApiService(
            HttpClient.newHttpClient(),
            Optional::empty,
            URI.create("http://localhost:" + server.getAddress().getPort()),
            1
        );

        GitHubApiException exception = assertThrows(GitHubApiException.class,
            () -> service.createPullRequest("org", "repo",
                new PullRequestDraft("title", "", "feature", "main", false)));

        assertEquals(GitHubApiException.Category.VALIDATION, exception.category());
        assertEquals(422, exception.statusCode());
    }

    @Test
    void retriesTransientFailures() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/repos/org/repo", exchange -> {
            if (calls.incrementAndGet() == 1) {
                respondJson(exchange, 503, "{}");
                return;
            }
            respondJson(exchange, 200, "{\"default_branch\":\"main\"}");
        });
        server.start();

        GitHubApiService service = new GitHubApiService(
            HttpClient.newHttpClient(),
            Optional::empty,
            URI.create("http://localhost:" + server.getAddress().getPort()),
            3
        );

        String branch = service.fetchDefaultBranch("org", "repo");

        assertEquals("main", branch);
        assertEquals(2, calls.get());
    }

    private static void respondJson(HttpExchange exchange, int status, String body) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, data.length);
        exchange.getResponseBody().write(data);
        exchange.close();
    }
}
