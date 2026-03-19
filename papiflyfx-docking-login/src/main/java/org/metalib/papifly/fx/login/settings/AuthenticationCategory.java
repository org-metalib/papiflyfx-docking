package org.metalib.papifly.fx.login.settings;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.metalib.papifly.fx.login.api.AuthSession;
import org.metalib.papifly.fx.login.api.AuthSessionBroker;
import org.metalib.papifly.fx.login.api.UserPrincipal;
import org.metalib.papifly.fx.login.core.DefaultAuthSessionBroker;
import org.metalib.papifly.fx.login.runtime.LoginRuntime;
import org.metalib.papifly.fx.settings.api.SecretKeyNames;
import org.metalib.papifly.fx.settings.api.SettingDefinition;
import org.metalib.papifly.fx.settings.api.SettingScope;
import org.metalib.papifly.fx.settings.api.SettingType;
import org.metalib.papifly.fx.settings.api.SettingsAction;
import org.metalib.papifly.fx.settings.api.SettingsCategory;
import org.metalib.papifly.fx.settings.api.SettingsContext;
import org.metalib.papifly.fx.settings.api.ValidationResult;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class AuthenticationCategory implements SettingsCategory {

    private static final String GENERIC_PROVIDER = "generic";
    private static final String GOOGLE_PROVIDER = "google";
    private static final String GITHUB_PROVIDER = "github";
    private static final String TOKEN_PREFIX = "login:oauth:refresh:";

    private static final SettingDefinition<Boolean> GENERIC_ENABLED = SettingDefinition
        .of("login.provider.generic.enabled", "Generic OIDC Enabled", SettingType.BOOLEAN, true)
        .withDescription("Enable the generic OIDC provider preset.");
    private static final SettingDefinition<String> GENERIC_DISCOVERY_URL = SettingDefinition
        .of("login.provider.generic.discoveryUrl", "Generic OIDC Discovery URL", SettingType.STRING, "")
        .withDescription("OIDC discovery document for the generic provider.");
    private static final SettingDefinition<String> GENERIC_CLIENT_ID = SettingDefinition
        .of("login.provider.generic.clientId", "Generic OIDC Client ID", SettingType.STRING, "")
        .withDescription("Client identifier used for generic OIDC flows.");
    private static final SettingDefinition<String> GENERIC_SCOPES = SettingDefinition
        .of("login.provider.generic.scopes", "Generic OIDC Scopes", SettingType.STRING, "openid profile email")
        .withDescription("Space or comma separated scopes for the generic provider.");
    private static final SettingDefinition<Boolean> GOOGLE_ENABLED = SettingDefinition
        .of("login.provider.google.enabled", "Google Enabled", SettingType.BOOLEAN, true)
        .withDescription("Enable Google sign-in.");
    private static final SettingDefinition<String> GOOGLE_CLIENT_ID = SettingDefinition
        .of("login.provider.google.clientId", "Google Client ID", SettingType.STRING, "")
        .withDescription("Client identifier used for Google sign-in.");
    private static final SettingDefinition<String> GOOGLE_SCOPES = SettingDefinition
        .of("login.provider.google.scopes", "Google Scopes", SettingType.STRING, "openid email profile")
        .withDescription("Granted scopes for Google sessions.");
    private static final SettingDefinition<String> GOOGLE_WORKSPACE_DOMAIN = SettingDefinition
        .of("login.provider.google.workspaceDomain", "Google Workspace Domain", SettingType.STRING, "")
        .withDescription("Optional hosted-domain restriction for Google accounts.");
    private static final SettingDefinition<Boolean> GITHUB_ENABLED = SettingDefinition
        .of("login.provider.github.enabled", "GitHub Enabled", SettingType.BOOLEAN, true)
        .withDescription("Enable GitHub sign-in.");
    private static final SettingDefinition<String> GITHUB_CLIENT_ID = SettingDefinition
        .of("login.provider.github.clientId", "GitHub Client ID", SettingType.STRING, "")
        .withDescription("Client identifier used for GitHub OAuth.");
    private static final SettingDefinition<String> GITHUB_SCOPES = SettingDefinition
        .of("login.provider.github.scopes", "GitHub Scopes", SettingType.STRING, "repo read:user")
        .withDescription("Granted scopes for GitHub sessions.");
    private static final SettingDefinition<String> GITHUB_ENTERPRISE_URL = SettingDefinition
        .of("login.provider.github.enterpriseApiUrl", "GitHub Enterprise API URL", SettingType.STRING, "https://api.github.com")
        .withDescription("Base API URL for GitHub Enterprise authentication.");

    private VBox pane;
    private CheckBox genericEnabledBox;
    private TextField genericDiscoveryField;
    private TextField genericClientIdField;
    private TextField genericScopesField;
    private CheckBox googleEnabledBox;
    private TextField googleClientIdField;
    private TextField googleScopesField;
    private TextField googleWorkspaceDomainField;
    private CheckBox githubEnabledBox;
    private TextField githubClientIdField;
    private TextField githubScopesField;
    private TextField githubEnterpriseApiField;
    private ComboBox<String> providerSelector;
    private TextField subjectField;
    private TextField displayNameField;
    private TextField emailField;
    private TextField sessionScopesField;
    private Label authStateLabel;
    private Label activeUserLabel;
    private Label activeProviderLabel;
    private Label activeScopesLabel;
    private Label activeExpiryLabel;
    private ListView<String> tokenList;
    private boolean dirty;

    @Override
    public String id() {
        return "authentication";
    }

    @Override
    public String displayName() {
        return "Authentication";
    }

    @Override
    public int order() {
        return 25;
    }

    @Override
    public List<SettingDefinition<?>> definitions() {
        return List.of(
            GENERIC_ENABLED,
            GENERIC_DISCOVERY_URL,
            GENERIC_CLIENT_ID,
            GENERIC_SCOPES,
            GOOGLE_ENABLED,
            GOOGLE_CLIENT_ID,
            GOOGLE_SCOPES,
            GOOGLE_WORKSPACE_DOMAIN,
            GITHUB_ENABLED,
            GITHUB_CLIENT_ID,
            GITHUB_SCOPES,
            GITHUB_ENTERPRISE_URL
        );
    }

    @Override
    public List<SettingsAction> actions() {
        return List.of(
            new SettingsAction("Test Connection", "Check the active session state.", this::testConnection),
            new SettingsAction("Refresh Token", "Refresh the active session.", this::refreshSession),
            new SettingsAction("Logout", "Clear the active session.", this::logout),
            new SettingsAction("Switch Account", "Activate the draft session.", this::switchAccount)
        );
    }

    @Override
    public Node buildSettingsPane(SettingsContext context) {
        if (pane == null) {
            genericEnabledBox = new CheckBox("Enable Generic OIDC");
            genericDiscoveryField = new TextField();
            genericClientIdField = new TextField();
            genericScopesField = new TextField();
            googleEnabledBox = new CheckBox("Enable Google");
            googleClientIdField = new TextField();
            googleScopesField = new TextField();
            googleWorkspaceDomainField = new TextField();
            githubEnabledBox = new CheckBox("Enable GitHub");
            githubClientIdField = new TextField();
            githubScopesField = new TextField();
            githubEnterpriseApiField = new TextField();
            providerSelector = new ComboBox<>();
            subjectField = new TextField();
            displayNameField = new TextField();
            emailField = new TextField();
            sessionScopesField = new TextField();
            authStateLabel = new Label();
            activeUserLabel = new Label();
            activeProviderLabel = new Label();
            activeScopesLabel = new Label();
            activeExpiryLabel = new Label();
            tokenList = new ListView<>();

            providerSelector.getItems().setAll(GENERIC_PROVIDER, GOOGLE_PROVIDER, GITHUB_PROVIDER);
            providerSelector.setValue(GITHUB_PROVIDER);

            installDirtyTracking();

            providerSelector.valueProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue != null && sessionScopesField.getText().isBlank()) {
                    sessionScopesField.setText(defaultScopes(newValue));
                }
            });

            AuthSessionBroker broker = LoginRuntime.broker();
            broker.authStateProperty().addListener((obs, oldValue, newValue) -> refreshSessionSummary());
            broker.sessionProperty().addListener((obs, oldValue, newValue) -> refreshSessionSummary());

            Button revokeTokenButton = new Button("Revoke Selected Token");
            revokeTokenButton.setOnAction(event -> revokeSelectedToken(context));

            VBox providers = section(
                "Provider Configuration",
                genericEnabledBox,
                field("Generic OIDC Discovery URL", genericDiscoveryField),
                field("Generic OIDC Client ID", genericClientIdField),
                field("Generic OIDC Scopes", genericScopesField),
                googleEnabledBox,
                field("Google Client ID", googleClientIdField),
                field("Google Scopes", googleScopesField),
                field("Google Workspace Domain", googleWorkspaceDomainField),
                githubEnabledBox,
                field("GitHub Client ID", githubClientIdField),
                field("GitHub Scopes", githubScopesField),
                field("GitHub Enterprise API URL", githubEnterpriseApiField)
            );
            VBox draft = section(
                "Session Draft",
                field("Provider", providerSelector),
                field("Subject", subjectField),
                field("Display Name", displayNameField),
                field("Email", emailField),
                field("Granted Scopes", sessionScopesField)
            );
            VBox activeSession = section(
                "Active Session",
                summaryRow("State", authStateLabel),
                summaryRow("User", activeUserLabel),
                summaryRow("Provider", activeProviderLabel),
                summaryRow("Scopes", activeScopesLabel),
                summaryRow("Expires", activeExpiryLabel)
            );
            VBox storedTokens = section(
                "Stored Refresh Tokens",
                tokenList,
                new HBox(8, revokeTokenButton)
            );
            VBox.setVgrow(tokenList, Priority.ALWAYS);

            pane = new VBox(16, providers, new Separator(), draft, new Separator(), activeSession, new Separator(), storedTokens);
            pane.setPadding(new Insets(8));
        }
        reset(context);
        return pane;
    }

    @Override
    public void apply(SettingsContext context) {
        context.storage().putBoolean(SettingScope.APPLICATION, GENERIC_ENABLED.key(), genericEnabledBox.isSelected());
        context.storage().putString(SettingScope.APPLICATION, GENERIC_DISCOVERY_URL.key(), genericDiscoveryField.getText());
        context.storage().putString(SettingScope.APPLICATION, GENERIC_CLIENT_ID.key(), genericClientIdField.getText());
        context.storage().putString(SettingScope.APPLICATION, GENERIC_SCOPES.key(), genericScopesField.getText());
        context.storage().putBoolean(SettingScope.APPLICATION, GOOGLE_ENABLED.key(), googleEnabledBox.isSelected());
        context.storage().putString(SettingScope.APPLICATION, GOOGLE_CLIENT_ID.key(), googleClientIdField.getText());
        context.storage().putString(SettingScope.APPLICATION, GOOGLE_SCOPES.key(), googleScopesField.getText());
        context.storage().putString(SettingScope.APPLICATION, GOOGLE_WORKSPACE_DOMAIN.key(), googleWorkspaceDomainField.getText());
        context.storage().putBoolean(SettingScope.APPLICATION, GITHUB_ENABLED.key(), githubEnabledBox.isSelected());
        context.storage().putString(SettingScope.APPLICATION, GITHUB_CLIENT_ID.key(), githubClientIdField.getText());
        context.storage().putString(SettingScope.APPLICATION, GITHUB_SCOPES.key(), githubScopesField.getText());
        context.storage().putString(SettingScope.APPLICATION, GITHUB_ENTERPRISE_URL.key(), githubEnterpriseApiField.getText());
        context.storage().save();
        dirty = false;
    }

    @Override
    public void reset(SettingsContext context) {
        genericEnabledBox.setSelected(context.storage().getBoolean(SettingScope.APPLICATION, GENERIC_ENABLED.key(), GENERIC_ENABLED.defaultValue()));
        genericDiscoveryField.setText(context.storage().getString(SettingScope.APPLICATION, GENERIC_DISCOVERY_URL.key(), GENERIC_DISCOVERY_URL.defaultValue()));
        genericClientIdField.setText(context.storage().getString(SettingScope.APPLICATION, GENERIC_CLIENT_ID.key(), GENERIC_CLIENT_ID.defaultValue()));
        genericScopesField.setText(context.storage().getString(SettingScope.APPLICATION, GENERIC_SCOPES.key(), GENERIC_SCOPES.defaultValue()));
        googleEnabledBox.setSelected(context.storage().getBoolean(SettingScope.APPLICATION, GOOGLE_ENABLED.key(), GOOGLE_ENABLED.defaultValue()));
        googleClientIdField.setText(context.storage().getString(SettingScope.APPLICATION, GOOGLE_CLIENT_ID.key(), GOOGLE_CLIENT_ID.defaultValue()));
        googleScopesField.setText(context.storage().getString(SettingScope.APPLICATION, GOOGLE_SCOPES.key(), GOOGLE_SCOPES.defaultValue()));
        googleWorkspaceDomainField.setText(context.storage().getString(SettingScope.APPLICATION, GOOGLE_WORKSPACE_DOMAIN.key(), GOOGLE_WORKSPACE_DOMAIN.defaultValue()));
        githubEnabledBox.setSelected(context.storage().getBoolean(SettingScope.APPLICATION, GITHUB_ENABLED.key(), GITHUB_ENABLED.defaultValue()));
        githubClientIdField.setText(context.storage().getString(SettingScope.APPLICATION, GITHUB_CLIENT_ID.key(), GITHUB_CLIENT_ID.defaultValue()));
        githubScopesField.setText(context.storage().getString(SettingScope.APPLICATION, GITHUB_SCOPES.key(), GITHUB_SCOPES.defaultValue()));
        githubEnterpriseApiField.setText(context.storage().getString(SettingScope.APPLICATION, GITHUB_ENTERPRISE_URL.key(), GITHUB_ENTERPRISE_URL.defaultValue()));
        refreshTokenList(context);
        refreshSessionSummary();
        dirty = false;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    private CompletableFuture<ValidationResult> testConnection(SettingsContext context) {
        AuthSession session = LoginRuntime.broker().activeSession().orElse(null);
        if (session == null) {
            return CompletableFuture.completedFuture(ValidationResult.warning("No active session."));
        }
        if (session.isExpired(Instant.now())) {
            refreshSessionSummary();
            return CompletableFuture.completedFuture(ValidationResult.warning("The active session has expired."));
        }
        return CompletableFuture.completedFuture(ValidationResult.info("Authenticated as " + session.subject() + " via " + session.providerId() + '.'));
    }

    private CompletableFuture<ValidationResult> refreshSession(SettingsContext context) {
        return LoginRuntime.broker().refresh(true).handle((session, error) -> {
            refreshSessionSummaryLater();
            if (error != null) {
                return ValidationResult.error(error.getMessage());
            }
            return ValidationResult.info("Refreshed token for " + session.subject() + '.');
        });
    }

    private CompletableFuture<ValidationResult> logout(SettingsContext context) {
        return LoginRuntime.broker().logout(false).handle((ignored, error) -> {
            refreshSessionSummaryLater();
            if (error != null) {
                return ValidationResult.error(error.getMessage());
            }
            return ValidationResult.info("Signed out of the active session.");
        });
    }

    private CompletableFuture<ValidationResult> switchAccount(SettingsContext context) {
        String providerId = value(providerSelector.getValue());
        String subject = value(subjectField.getText());
        if (providerId.isEmpty() || subject.isEmpty()) {
            return CompletableFuture.completedFuture(ValidationResult.warning("Select a provider and enter a subject."));
        }
        if (!isProviderEnabled(providerId)) {
            return CompletableFuture.completedFuture(ValidationResult.warning("The selected provider is disabled."));
        }

        String refreshTokenRef = SecretKeyNames.oauthRefreshToken(providerId, subject);
        if (!context.secretStore().hasSecret(refreshTokenRef)) {
            context.secretStore().setSecret(refreshTokenRef, "managed-session-token");
        }

        String displayName = value(displayNameField.getText()).isEmpty() ? subject : value(displayNameField.getText());
        UserPrincipal principal = new UserPrincipal(displayName, value(emailField.getText()), "");
        Instant issuedAt = Instant.now();
        AuthSession session = new AuthSession(
            providerId,
            subject,
            principal,
            parseScopes(sessionScopesField.getText().isBlank() ? defaultScopes(providerId) : sessionScopesField.getText()),
            "",
            refreshTokenRef,
            issuedAt.plusSeconds(3600),
            issuedAt
        );

        AuthSessionBroker broker = LoginRuntime.broker();
        if (broker instanceof DefaultAuthSessionBroker defaultBroker) {
            defaultBroker.upsertSession(session);
        }
        broker.setActiveSession(providerId, subject);

        refreshSessionSummary();
        refreshTokenList(context);
        return CompletableFuture.completedFuture(ValidationResult.info("Active account switched to " + subject + '.'));
    }

    private void revokeSelectedToken(SettingsContext context) {
        String key = tokenList.getSelectionModel().getSelectedItem();
        if (key == null || key.isBlank()) {
            return;
        }
        context.secretStore().clearSecret(key);
        SessionKey sessionKey = sessionKey(key);
        if (sessionKey != null && LoginRuntime.broker() instanceof DefaultAuthSessionBroker defaultBroker) {
            defaultBroker.removeSession(sessionKey.providerId(), sessionKey.subject());
        }
        refreshTokenList(context);
        refreshSessionSummary();
    }

    private void refreshTokenList(SettingsContext context) {
        tokenList.getItems().setAll(
            context.secretStore().listKeys().stream()
                .filter(key -> key.startsWith(TOKEN_PREFIX))
                .sorted()
                .toList()
        );
    }

    private void refreshSessionSummary() {
        AuthSessionBroker broker = LoginRuntime.broker();
        authStateLabel.setText(String.valueOf(broker.authStateProperty().get()));
        AuthSession session = broker.activeSession().orElse(null);
        if (session == null) {
            activeUserLabel.setText("-");
            activeProviderLabel.setText("-");
            activeScopesLabel.setText("-");
            activeExpiryLabel.setText("-");
            return;
        }
        activeUserLabel.setText(userLabel(session));
        activeProviderLabel.setText(session.providerId());
        activeScopesLabel.setText(session.scopes().isEmpty() ? "-" : String.join(", ", session.scopes()));
        activeExpiryLabel.setText(expiryLabel(session.expiresAt()));
    }

    private void refreshSessionSummaryLater() {
        if (Platform.isFxApplicationThread()) {
            refreshSessionSummary();
            return;
        }
        Platform.runLater(this::refreshSessionSummary);
    }

    private void installDirtyTracking() {
        List<CheckBox> checkBoxes = List.of(genericEnabledBox, googleEnabledBox, githubEnabledBox);
        for (CheckBox checkBox : checkBoxes) {
            checkBox.selectedProperty().addListener((obs, oldValue, newValue) -> dirty = true);
        }
        List<TextField> textFields = List.of(
            genericDiscoveryField,
            genericClientIdField,
            genericScopesField,
            googleClientIdField,
            googleScopesField,
            googleWorkspaceDomainField,
            githubClientIdField,
            githubScopesField,
            githubEnterpriseApiField,
            subjectField,
            displayNameField,
            emailField,
            sessionScopesField
        );
        for (TextField textField : textFields) {
            textField.textProperty().addListener((obs, oldValue, newValue) -> dirty = true);
        }
        providerSelector.valueProperty().addListener((obs, oldValue, newValue) -> dirty = true);
    }

    private boolean isProviderEnabled(String providerId) {
        return switch (providerId) {
            case GENERIC_PROVIDER -> genericEnabledBox.isSelected();
            case GOOGLE_PROVIDER -> googleEnabledBox.isSelected();
            case GITHUB_PROVIDER -> githubEnabledBox.isSelected();
            default -> false;
        };
    }

    private String defaultScopes(String providerId) {
        return switch (providerId) {
            case GENERIC_PROVIDER -> value(genericScopesField.getText()).isEmpty() ? GENERIC_SCOPES.defaultValue() : genericScopesField.getText();
            case GOOGLE_PROVIDER -> value(googleScopesField.getText()).isEmpty() ? GOOGLE_SCOPES.defaultValue() : googleScopesField.getText();
            case GITHUB_PROVIDER -> value(githubScopesField.getText()).isEmpty() ? GITHUB_SCOPES.defaultValue() : githubScopesField.getText();
            default -> "";
        };
    }

    private Set<String> parseScopes(String text) {
        return Arrays.stream(value(text).split("[,\\s]+"))
            .map(String::trim)
            .filter(token -> !token.isEmpty())
            .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    private String userLabel(AuthSession session) {
        UserPrincipal principal = session.principal();
        if (principal == null) {
            return session.subject();
        }
        String displayName = value(principal.displayName());
        String email = value(principal.email());
        if (!displayName.isEmpty() && !email.isEmpty()) {
            return displayName + " <" + email + '>';
        }
        if (!displayName.isEmpty()) {
            return displayName;
        }
        if (!email.isEmpty()) {
            return email;
        }
        return session.subject();
    }

    private String expiryLabel(Instant expiresAt) {
        if (expiresAt == null) {
            return "-";
        }
        Duration duration = Duration.between(Instant.now(), expiresAt);
        if (duration.isNegative() || duration.isZero()) {
            return "expired";
        }
        long hours = duration.toHours();
        long minutes = duration.minusHours(hours).toMinutes();
        if (hours > 0) {
            return "in " + hours + "h " + minutes + "m";
        }
        return "in " + minutes + "m";
    }

    private String value(String text) {
        return text == null ? "" : text.trim();
    }

    private SessionKey sessionKey(String secretKey) {
        if (!secretKey.startsWith(TOKEN_PREFIX)) {
            return null;
        }
        String remainder = secretKey.substring(TOKEN_PREFIX.length());
        int separator = remainder.indexOf(':');
        if (separator < 0) {
            return null;
        }
        String providerId = remainder.substring(0, separator);
        String subject = remainder.substring(separator + 1);
        if (providerId.isBlank() || subject.isBlank()) {
            return null;
        }
        return new SessionKey(providerId, subject);
    }

    private VBox section(String title, Node... content) {
        Label label = new Label(title);
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        VBox box = new VBox(10, label);
        box.getChildren().addAll(content);
        return box;
    }

    private VBox field(String labelText, Node field) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-weight: bold;");
        return new VBox(4, label, field);
    }

    private HBox summaryRow(String labelText, Label valueLabel) {
        Label label = new Label(labelText);
        label.setMinWidth(80);
        HBox row = new HBox(8, label, valueLabel);
        HBox.setHgrow(valueLabel, Priority.ALWAYS);
        return row;
    }

    private record SessionKey(String providerId, String subject) {
    }
}
