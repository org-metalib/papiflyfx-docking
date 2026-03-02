package org.metalib.papifly.fx.media.stream;

public final class EmbedUrlResolver {

    private EmbedUrlResolver() {}

    public static String resolve(String url) {
        if (url == null) return url;
        String lower = url.toLowerCase(java.util.Locale.ROOT);

        // YouTube watch → embed
        if (lower.contains("youtube.com/watch")) {
            String id = extractParam(url, "v");
            if (id != null) return "https://www.youtube.com/embed/" + id;
        }
        if (lower.contains("youtu.be/")) {
            int start = lower.indexOf("youtu.be/") + 9;
            int end = url.indexOf('?', start);
            String id = end < 0 ? url.substring(start) : url.substring(start, end);
            return "https://www.youtube.com/embed/" + id;
        }

        // Vimeo watch → embed
        if (lower.contains("vimeo.com/") && !lower.contains("player.vimeo.com")) {
            int start = lower.lastIndexOf('/') + 1;
            String id = url.substring(start).split("\\?")[0];
            return "https://player.vimeo.com/video/" + id;
        }

        // Twitch channel → embed with parent workaround
        if (lower.contains("twitch.tv/")) {
            int start = lower.indexOf("twitch.tv/") + 10;
            String channel = url.substring(start).split("[/?]")[0];
            return "https://player.twitch.tv/?channel=" + channel + "&parent=localhost";
        }

        // Already an embed or unknown — return as-is
        return url;
    }

    private static String extractParam(String url, String param) {
        String key = param + "=";
        int idx = url.indexOf(key);
        if (idx < 0) return null;
        int start = idx + key.length();
        int end = url.indexOf('&', start);
        return end < 0 ? url.substring(start) : url.substring(start, end);
    }
}
