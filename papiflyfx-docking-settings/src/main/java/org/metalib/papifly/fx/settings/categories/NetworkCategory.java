package org.metalib.papifly.fx.settings.categories;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.metalib.papifly.fx.settings.api.SettingDefinition;
import org.metalib.papifly.fx.settings.api.SettingScope;
import org.metalib.papifly.fx.settings.api.SettingType;
import org.metalib.papifly.fx.settings.api.SettingsCategory;
import org.metalib.papifly.fx.settings.api.SettingsContext;

import java.util.List;

public class NetworkCategory implements SettingsCategory {

    private static final SettingDefinition<Boolean> PROXY_ENABLED = SettingDefinition
        .of("network.proxy.enabled", "Proxy Enabled", SettingType.BOOLEAN, false)
        .withDescription("Enable outbound proxy routing.");
    private static final SettingDefinition<String> PROXY_HOST = SettingDefinition
        .of("network.proxy.host", "Proxy Host", SettingType.STRING, "")
        .withDescription("Proxy host name.");
    private static final SettingDefinition<Integer> PROXY_PORT = SettingDefinition
        .of("network.proxy.port", "Proxy Port", SettingType.INTEGER, 8080)
        .withDescription("Proxy port.");
    private static final SettingDefinition<Boolean> TLS_VERIFY = SettingDefinition
        .of("network.tls.verify", "Verify TLS", SettingType.BOOLEAN, true)
        .withDescription("Verify TLS certificates for outbound requests.");
    private static final SettingDefinition<Integer> CONNECT_TIMEOUT = SettingDefinition
        .of("network.timeout.connect", "Connect Timeout (ms)", SettingType.INTEGER, 5000)
        .withDescription("Socket connect timeout.");
    private static final SettingDefinition<Integer> READ_TIMEOUT = SettingDefinition
        .of("network.timeout.read", "Read Timeout (ms)", SettingType.INTEGER, 15000)
        .withDescription("Socket read timeout.");

    private CheckBox proxyEnabled;
    private TextField proxyHost;
    private TextField proxyPort;
    private CheckBox tlsVerify;
    private TextField connectTimeout;
    private TextField readTimeout;
    private VBox pane;
    private boolean dirty;

    @Override
    public String id() {
        return "network";
    }

    @Override
    public String displayName() {
        return "Network";
    }

    @Override
    public int order() {
        return 75;
    }

    @Override
    public List<SettingDefinition<?>> definitions() {
        return List.of(PROXY_ENABLED, PROXY_HOST, PROXY_PORT, TLS_VERIFY, CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    @Override
    public Node buildSettingsPane(SettingsContext context) {
        if (pane == null) {
            proxyEnabled = new CheckBox("Enable Proxy");
            proxyHost = new TextField();
            proxyPort = new TextField();
            tlsVerify = new CheckBox("Verify TLS certificates");
            connectTimeout = new TextField();
            readTimeout = new TextField();

            proxyEnabled.selectedProperty().addListener((obs, oldValue, newValue) -> dirty = true);
            proxyHost.textProperty().addListener((obs, oldValue, newValue) -> dirty = true);
            proxyPort.textProperty().addListener((obs, oldValue, newValue) -> dirty = true);
            tlsVerify.selectedProperty().addListener((obs, oldValue, newValue) -> dirty = true);
            connectTimeout.textProperty().addListener((obs, oldValue, newValue) -> dirty = true);
            readTimeout.textProperty().addListener((obs, oldValue, newValue) -> dirty = true);

            pane = new VBox(
                12,
                proxyEnabled,
                field("Proxy Host", proxyHost),
                field("Proxy Port", proxyPort),
                tlsVerify,
                field("Connect Timeout (ms)", connectTimeout),
                field("Read Timeout (ms)", readTimeout)
            );
            pane.setPadding(new Insets(8));
        }
        reset(context);
        return pane;
    }

    @Override
    public void apply(SettingsContext context) {
        context.storage().putBoolean(SettingScope.APPLICATION, PROXY_ENABLED.key(), proxyEnabled.isSelected());
        context.storage().putString(SettingScope.APPLICATION, PROXY_HOST.key(), proxyHost.getText());
        context.storage().putInt(SettingScope.APPLICATION, PROXY_PORT.key(), parse(proxyPort.getText(), PROXY_PORT.defaultValue()));
        context.storage().putBoolean(SettingScope.APPLICATION, TLS_VERIFY.key(), tlsVerify.isSelected());
        context.storage().putInt(SettingScope.APPLICATION, CONNECT_TIMEOUT.key(), parse(connectTimeout.getText(), CONNECT_TIMEOUT.defaultValue()));
        context.storage().putInt(SettingScope.APPLICATION, READ_TIMEOUT.key(), parse(readTimeout.getText(), READ_TIMEOUT.defaultValue()));
        context.storage().save();
        dirty = false;
    }

    @Override
    public void reset(SettingsContext context) {
        proxyEnabled.setSelected(context.storage().getBoolean(SettingScope.APPLICATION, PROXY_ENABLED.key(), PROXY_ENABLED.defaultValue()));
        proxyHost.setText(context.storage().getString(SettingScope.APPLICATION, PROXY_HOST.key(), PROXY_HOST.defaultValue()));
        proxyPort.setText(String.valueOf(context.storage().getInt(SettingScope.APPLICATION, PROXY_PORT.key(), PROXY_PORT.defaultValue())));
        tlsVerify.setSelected(context.storage().getBoolean(SettingScope.APPLICATION, TLS_VERIFY.key(), TLS_VERIFY.defaultValue()));
        connectTimeout.setText(String.valueOf(context.storage().getInt(SettingScope.APPLICATION, CONNECT_TIMEOUT.key(), CONNECT_TIMEOUT.defaultValue())));
        readTimeout.setText(String.valueOf(context.storage().getInt(SettingScope.APPLICATION, READ_TIMEOUT.key(), READ_TIMEOUT.defaultValue())));
        dirty = false;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    private VBox field(String labelText, TextField field) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-weight: bold;");
        return new VBox(4, label, field);
    }

    private int parse(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }
}
