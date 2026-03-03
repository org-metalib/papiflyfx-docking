package org.metalib.papifly.fx.media.viewer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.stream.EmbedUrlResolver;

import java.io.IOException;
import java.io.OutputStream;
import java.awt.Desktop;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.Executors;

public class EmbedViewer extends StackPane {

    private static final String MODERN_USER_AGENT =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";
    private static final System.Logger LOG = System.getLogger(EmbedViewer.class.getName());

    private final WebView webView = new WebView();
    private final ObjectProperty<Theme> themeProperty = new SimpleObjectProperty<>();
    private String youtubeWrapperUrl;

    public EmbedViewer(String url) {
        webView.setContextMenuEnabled(false);
        webView.getEngine().setUserAgent(MODERN_USER_AGENT);
        webView.prefWidthProperty().bind(widthProperty());
        webView.prefHeightProperty().bind(heightProperty());
        wireExternalNavigation();
        getChildren().add(webView);
        wireTheme();
        load(url);
    }

    public void load(String url) {
        String embedUrl = EmbedUrlResolver.resolve(url);
        if (isYouTubeEmbed(embedUrl)) {
            youtubeWrapperUrl = LocalYouTubeWrapperServer.wrapperUrlFor(embedUrl);
            webView.getEngine().load(youtubeWrapperUrl);
        } else {
            youtubeWrapperUrl = null;
            webView.getEngine().load(embedUrl);
        }
    }

    public ObjectProperty<Theme> themeProperty() { return themeProperty; }

    public void dispose() {
        webView.getEngine().load(null);
    }

    private void wireTheme() {
        themeProperty.addListener((obs, o, t) -> {
            if (t == null) return;
            setBackground(new javafx.scene.layout.Background(
                new javafx.scene.layout.BackgroundFill(t.background(),
                    javafx.scene.layout.CornerRadii.EMPTY, Insets.EMPTY)));
        });
    }

    private void wireExternalNavigation() {
        webView.getEngine().locationProperty().addListener((obs, oldLocation, newLocation) -> {
            if (!shouldOpenExternally(newLocation, youtubeWrapperUrl)) return;
            openInSystemBrowser(newLocation);
            if (youtubeWrapperUrl != null) {
                Platform.runLater(() -> webView.getEngine().load(youtubeWrapperUrl));
            }
        });
    }

    private static boolean isYouTubeEmbed(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.contains("youtube.com/embed/") || lower.contains("youtube-nocookie.com/embed/");
    }

    static boolean shouldOpenExternally(String newLocation, String youtubeWrapperUrl) {
        if (youtubeWrapperUrl == null || youtubeWrapperUrl.isBlank()) return false;
        if (newLocation == null || newLocation.isBlank()) return false;
        String lower = newLocation.toLowerCase(Locale.ROOT);
        if (lower.startsWith("about:") || lower.startsWith("data:") || lower.startsWith("javascript:")) return false;
        String localOrigin = LocalYouTubeWrapperServer.origin();
        return !newLocation.startsWith(localOrigin + "/");
    }

    static String wrapperUrlForTesting(String embedUrl) {
        return LocalYouTubeWrapperServer.wrapperUrlFor(embedUrl);
    }

    private static void openInSystemBrowser(String url) {
        if (!Desktop.isDesktopSupported()) return;
        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.BROWSE)) return;
        try {
            desktop.browse(new URI(url));
        } catch (IOException | URISyntaxException e) {
            LOG.log(System.Logger.Level.WARNING, "Failed to open external browser URL: " + url, e);
        }
    }

    private static String buildYouTubeIframeHtml(String embedUrl) {
        String safeUrl = escapeHtmlAttribute(embedUrl);
        return """
            <!doctype html>
            <html>
            <head>
              <meta charset="utf-8">
              <meta name="referrer" content="strict-origin-when-cross-origin">
              <style>
                html, body { margin: 0; width: 100%%; height: 100%%; overflow: hidden; background: #000; }
                iframe { border: 0; width: 100%%; height: 100%%; display: block; }
              </style>
            </head>
            <body>
              <iframe
                src="%s"
                title="YouTube video player"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                allowfullscreen
                referrerpolicy="strict-origin-when-cross-origin"></iframe>
            </body>
            </html>
            """.formatted(safeUrl);
    }

    private static String escapeHtmlAttribute(String value) {
        if (value == null) return "";
        return value
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

    private static final class LocalYouTubeWrapperServer {
        private static final LocalYouTubeWrapperServer INSTANCE = new LocalYouTubeWrapperServer();

        private final HttpServer server;
        private final String origin;

        private LocalYouTubeWrapperServer() {
            try {
                server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to start local YouTube wrapper server", e);
            }
            server.createContext("/youtube", this::handleYouTube);
            server.setExecutor(Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "papiflyfx-youtube-wrapper");
                t.setDaemon(true);
                return t;
            }));
            server.start();
            origin = "http://127.0.0.1:" + server.getAddress().getPort();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0), "papiflyfx-youtube-wrapper-stop"));
        }

        private static String wrapperUrlFor(String embedUrl) {
            return INSTANCE.origin + "/youtube?src=" + urlEncode(embedUrl);
        }

        private static String origin() {
            return INSTANCE.origin;
        }

        private void handleYouTube(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                write(exchange, 405, "Method Not Allowed", "text/plain; charset=UTF-8");
                return;
            }

            String embedUrl = queryParam(exchange.getRequestURI().getRawQuery(), "src");
            if (!isYouTubeEmbed(embedUrl)) {
                write(exchange, 400, "Invalid embed URL", "text/plain; charset=UTF-8");
                return;
            }

            String iframeUrl = withYouTubeContextParams(embedUrl);
            String html = buildYouTubeIframeHtml(iframeUrl);
            write(exchange, 200, html, "text/html; charset=UTF-8");
        }

        private String withYouTubeContextParams(String embedUrl) {
            String separator = embedUrl.contains("?") ? "&" : "?";
            String widgetReferrer = origin + "/youtube";
            return embedUrl + separator
                + "origin=" + urlEncode(origin)
                + "&widget_referrer=" + urlEncode(widgetReferrer);
        }

        private static String queryParam(String rawQuery, String key) {
            if (rawQuery == null || rawQuery.isBlank()) return null;
            for (String part : rawQuery.split("&")) {
                int idx = part.indexOf('=');
                String partKey = idx < 0 ? part : part.substring(0, idx);
                if (key.equals(partKey)) {
                    String value = idx < 0 ? "" : part.substring(idx + 1);
                    return URLDecoder.decode(value, StandardCharsets.UTF_8);
                }
            }
            return null;
        }

        private static String urlEncode(String value) {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        }

        private static void write(HttpExchange exchange, int status, String body, String contentType) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Cache-Control", "no-store, max-age=0");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
    }
}
