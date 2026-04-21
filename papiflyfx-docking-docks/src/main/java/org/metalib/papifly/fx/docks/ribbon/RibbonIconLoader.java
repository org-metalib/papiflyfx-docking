package org.metalib.papifly.fx.docks.ribbon;

import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import org.metalib.papifly.fx.api.ribbon.RibbonIconHandle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Internal icon loader for ribbon controls. Supports octicons, lightweight
 * path-based SVG documents, and raster image resources.
 */
final class RibbonIconLoader {

    private static final Logger LOG = Logger.getLogger(RibbonIconLoader.class.getName());
    private static final String OCTICON_PREFIX = "octicon:";

    private RibbonIconLoader() {
    }

    static Node loadGraphic(RibbonIconHandle iconHandle, double size, ClassLoader classLoader) {
        if (iconHandle == null || iconHandle.resourcePath().isBlank()) {
            return null;
        }
        String resourcePath = iconHandle.resourcePath();
        Node octicon = createOcticonGraphic(resourcePath, size);
        if (octicon != null) {
            return octicon;
        }
        if (looksLikeSvg(resourcePath)) {
            try {
                Node svgGraphic = loadSvgGraphic(resourcePath, size, classLoader);
                if (svgGraphic != null) {
                    return svgGraphic;
                }
            } catch (Exception exception) {
                LOG.log(Level.WARNING, "Failed to render SVG icon " + resourcePath + "; falling back to raster loading", exception);
            }
        }
        return loadRasterGraphic(resourcePath, size, classLoader);
    }

    private static Node loadSvgGraphic(String resourcePath, double size, ClassLoader classLoader) throws Exception {
        URL resource = resolveResource(resourcePath, classLoader);
        if (resource == null) {
            return null;
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setNamespaceAware(true);

        try (InputStream inputStream = resource.openStream();
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            Document document = factory.newDocumentBuilder()
                .parse(new InputSource(new StringReader(readAll(reader))));
            Element root = document.getDocumentElement();
            if (root == null || !"svg".equalsIgnoreCase(root.getLocalName() == null ? root.getTagName() : root.getLocalName())) {
                throw new IllegalArgumentException("Resource is not an SVG document: " + resourcePath);
            }

            Group group = new Group();
            NodeList paths = root.getElementsByTagNameNS("*", "path");
            if (paths.getLength() == 0) {
                paths = root.getElementsByTagName("path");
            }
            for (int index = 0; index < paths.getLength(); index++) {
                org.w3c.dom.Node domNode = paths.item(index);
                if (!(domNode instanceof Element pathElement)) {
                    continue;
                }
                String pathData = pathElement.getAttribute("d");
                if (pathData == null || pathData.isBlank()) {
                    continue;
                }
                SVGPath path = new SVGPath();
                path.setContent(pathData);
                path.getStyleClass().add("pf-ribbon-svg-path");
                path.setStyle("-fx-fill: -pf-ui-text-primary;");
                group.getChildren().add(path);
            }
            if (group.getChildren().isEmpty()) {
                throw new IllegalArgumentException("SVG icon contains no path elements: " + resourcePath);
            }

            Bounds bounds = group.getLayoutBounds();
            double maxDimension = Math.max(bounds.getWidth(), bounds.getHeight());
            if (maxDimension <= 0.0) {
                throw new IllegalArgumentException("SVG icon has empty bounds: " + resourcePath);
            }
            double scale = size / maxDimension;
            group.setScaleX(scale);
            group.setScaleY(scale);
            group.setTranslateX((-bounds.getMinX() * scale) + ((size - (bounds.getWidth() * scale)) / 2.0));
            group.setTranslateY((-bounds.getMinY() * scale) + ((size - (bounds.getHeight() * scale)) / 2.0));

            StackPane wrapper = new StackPane(group);
            wrapper.setAlignment(Pos.CENTER);
            wrapper.getStyleClass().add("pf-ribbon-icon");
            wrapper.setMinSize(size, size);
            wrapper.setPrefSize(size, size);
            wrapper.setMaxSize(size, size);
            return wrapper;
        }
    }

    private static Node loadRasterGraphic(String resourcePath, double size, ClassLoader classLoader) {
        try {
            URL resource = resolveResource(resourcePath, classLoader);
            if (resource == null) {
                return null;
            }
            try (InputStream inputStream = resource.openStream()) {
                Image image = new Image(inputStream, size, size, true, true);
                if (image.isError()) {
                    return null;
                }
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(size);
                imageView.setFitHeight(size);
                imageView.setPreserveRatio(true);
                imageView.getStyleClass().add("pf-ribbon-icon");
                return imageView;
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static URL resolveResource(String resourcePath, ClassLoader classLoader) {
        if (resourcePath.startsWith("http://") || resourcePath.startsWith("https://") || resourcePath.startsWith("file:")) {
            try {
                return new URL(resourcePath);
            } catch (Exception ignored) {
                return null;
            }
        }
        URL resource = RibbonIconLoader.class.getResource(resourcePath);
        if (resource != null) {
            return resource;
        }
        String normalizedPath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        ClassLoader resolvedLoader = classLoader == null ? RibbonIconLoader.class.getClassLoader() : classLoader;
        return resolvedLoader.getResource(normalizedPath);
    }

    private static boolean looksLikeSvg(String resourcePath) {
        String normalized = resourcePath.toLowerCase(Locale.ROOT);
        return normalized.endsWith(".svg") || normalized.contains(".svg?");
    }

    private static String readAll(InputStreamReader reader) throws Exception {
        StringBuilder content = new StringBuilder();
        char[] buffer = new char[4096];
        int read;
        while ((read = reader.read(buffer)) >= 0) {
            content.append(buffer, 0, read);
        }
        return content.toString();
    }

    private static Node createOcticonGraphic(String resourcePath, double size) {
        if (resourcePath == null || !resourcePath.startsWith(OCTICON_PREFIX)) {
            return null;
        }
        String name = resourcePath.substring(OCTICON_PREFIX.length()).trim().toLowerCase(Locale.ROOT);
        String pathData = octiconPath(name);
        if (pathData == null) {
            return null;
        }
        SVGPath svgPath = new SVGPath();
        svgPath.setContent(pathData);
        svgPath.getStyleClass().add("pf-ribbon-octicon");
        svgPath.setStyle("-fx-fill: -pf-ui-text-primary;");
        double scale = size / 16.0;
        svgPath.setScaleX(scale);
        svgPath.setScaleY(scale);
        StackPane wrapper = new StackPane(svgPath);
        wrapper.getStyleClass().add("pf-ribbon-icon-octicon-wrap");
        wrapper.setMinSize(size, size);
        wrapper.setPrefSize(size, size);
        wrapper.setMaxSize(size, size);
        return wrapper;
    }

    private static String octiconPath(String name) {
        return switch (name) {
            case "sync" -> "M8,2 A6,6 0 0 1 14,8 L12,6 M14,8 L10,8 M8,14 A6,6 0 0 1 2,8 L4,10 M2,8 L6,8";
            case "upload" -> "M8,2 L12,6 L10,6 L10,10 L6,10 L6,6 L4,6 Z M3,12 L13,12 L13,14 L3,14 Z";
            case "download", "repo-pull" -> "M8,14 L12,10 L10,10 L10,6 L6,6 L6,10 L4,10 Z M3,2 L13,2 L13,4 L3,4 Z";
            case "git-branch" -> "M5,3 A2,2 0 1 0 5,7 A2,2 0 1 0 5,3 Z M11,3 A2,2 0 1 0 11,7 A2,2 0 1 0 11,3 Z M11,9 A2,2 0 1 0 11,13 A2,2 0 1 0 11,9 Z M5,7 L5,11 L9,11 M5,9 C5,7 7,6 9,6";
            case "git-merge" -> "M5,3 A2,2 0 1 0 5,7 A2,2 0 1 0 5,3 Z M11,3 A2,2 0 1 0 11,7 A2,2 0 1 0 11,3 Z M8,13 A2,2 0 1 0 8,17 A2,2 0 1 0 8,13 Z M5,7 C5,9 7,9 8,11 M11,7 C11,9 9,9 8,11";
            case "git-pull-request" -> "M5,3 A2,2 0 1 0 5,7 A2,2 0 1 0 5,3 Z M5,7 L5,12 M11,3 A2,2 0 1 0 11,7 A2,2 0 1 0 11,3 Z M5,10 C5,7 8,7 10,7 M10,7 L8,5 M10,7 L8,9";
            case "issue-opened" -> "M8,2 A6,6 0 1 0 8,14 A6,6 0 1 0 8,2 Z M8,5 L8,9 M8,11 L8,11.2";
            case "git-commit" -> "M8,2 A6,6 0 1 0 8,14 A6,6 0 1 0 8,2 Z";
            case "diff" -> "M4,3 A1.5,1.5 0 1 0 4,6 A1.5,1.5 0 1 0 4,3 Z M12,10 A1.5,1.5 0 1 0 12,13 A1.5,1.5 0 1 0 12,10 Z M4,6 L4,12 M4,12 L10.5,12 M12,3 L12,10";
            case "trash" -> "M5,5 L11,5 L10.5,14 L5.5,14 Z M4,5 L12,5 M6.5,5 L6.5,3 L9.5,3 L9.5,5 M7,7 L7,12 M9,7 L9,12";
            case "play" -> "M4,3 L13,8 L4,13 Z";
            case "stop" -> "M4,4 L12,4 L12,12 L4,12 Z";
            case "file-add" -> "M4,2 L10,2 L13,5 L13,14 L4,14 Z M10,2 L10,5 L13,5 M8.5,8 L8.5,12 M6.5,10 L10.5,10";
            case "file" -> "M4,2 L10,2 L13,5 L13,14 L4,14 Z M10,2 L10,5 L13,5";
            case "pencil" -> "M3,12 L4,15 L7,14 L13,8 L10,5 Z M9,6 L12,9";
            case "package" -> "M3,5 L8,2 L13,5 L13,11 L8,14 L3,11 Z M3,5 L8,8 L13,5 M8,8 L8,14";
            case "package-dependencies", "archive" -> "M3,4 L13,4 L13,13 L3,13 Z M3,7 L13,7 M6,9 L10,9";
            case "terminal" -> "M2,3 L14,3 L14,13 L2,13 Z M4,6 L6.5,8 L4,10 M8,10 L11,10";
            case "code", "code-square" -> "M3,3 L13,3 L13,13 L3,13 Z M7,6 L5,8 L7,10 M9,6 L11,8 L9,10";
            case "video" -> "M3,4 L11,4 L11,12 L3,12 Z M11,7 L14,5.5 L14,10.5 L11,9 Z M6,6 L9,8 L6,10 Z";
            case "image" -> "M3,3 L13,3 L13,13 L3,13 Z M5,6 A1.2,1.2 0 1 0 5,8.4 A1.2,1.2 0 1 0 5,6 Z M4,12 L7.5,8.5 L9.5,10.5 L11,9 L12.5,12 Z";
            default -> null;
        };
    }
}
