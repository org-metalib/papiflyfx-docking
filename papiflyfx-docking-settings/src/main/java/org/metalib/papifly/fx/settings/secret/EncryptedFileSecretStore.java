package org.metalib.papifly.fx.settings.secret;

import org.metalib.papifly.fx.settings.api.SecretStore;
import org.metalib.papifly.fx.settings.internal.SettingsJsonCodec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class EncryptedFileSecretStore implements SecretStore {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String KDF = "PBKDF2WithHmacSHA256";
    private static final int KEY_LENGTH = 256;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 12;
    private static final int ITERATIONS = 65_536;

    private final Path secretsFile;
    private final SettingsJsonCodec codec = new SettingsJsonCodec();
    private final SecureRandom random = new SecureRandom();

    public EncryptedFileSecretStore() {
        this(Path.of(System.getProperty("user.home"), ".papiflyfx", "secrets.enc"));
    }

    public EncryptedFileSecretStore(Path secretsFile) {
        this.secretsFile = secretsFile;
    }

    @Override
    public synchronized Optional<String> getSecret(String key) {
        return Optional.ofNullable(loadSecrets().get(key)).filter(value -> !value.isBlank());
    }

    @Override
    public synchronized void setSecret(String key, String value) {
        Map<String, String> secrets = loadSecrets();
        if (value == null || value.isBlank()) {
            secrets.remove(key);
        } else {
            secrets.put(key, value);
        }
        saveSecrets(secrets);
    }

    @Override
    public synchronized void clearSecret(String key) {
        Map<String, String> secrets = loadSecrets();
        if (secrets.remove(key) != null) {
            saveSecrets(secrets);
        }
    }

    @Override
    public synchronized Set<String> listKeys() {
        return new LinkedHashSet<>(loadSecrets().keySet());
    }

    @Override
    public String backendName() {
        return "Encrypted File";
    }

    private Map<String, String> loadSecrets() {
        if (!Files.exists(secretsFile)) {
            return new LinkedHashMap<>();
        }
        try {
            String json = Files.readString(secretsFile, StandardCharsets.UTF_8);
            Map<String, Object> envelope = codec.fromJson(json);
            byte[] salt = decode(envelope.get("salt"));
            byte[] iv = decode(envelope.get("iv"));
            byte[] encrypted = decode(envelope.get("payload"));
            byte[] decrypted = decrypt(encrypted, salt, iv);
            try {
                String payloadJson = new String(decrypted, StandardCharsets.UTF_8);
                Map<String, Object> payload = codec.fromJson(payloadJson);
                Map<String, String> secrets = new LinkedHashMap<>();
                for (Map.Entry<String, Object> entry : payload.entrySet()) {
                    if (entry.getValue() != null) {
                        secrets.put(entry.getKey(), String.valueOf(entry.getValue()));
                    }
                }
                return secrets;
            } finally {
                Arrays.fill(decrypted, (byte) 0);
            }
        } catch (IOException | GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to read encrypted secrets from " + secretsFile, exception);
        }
    }

    private void saveSecrets(Map<String, String> secrets) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.putAll(secrets);
        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(salt);
        random.nextBytes(iv);
        byte[] plaintext = codec.toJson(new LinkedHashMap<>(payload)).getBytes(StandardCharsets.UTF_8);
        try {
            byte[] encrypted = encrypt(plaintext, salt, iv);
            try {
                Map<String, Object> envelope = new LinkedHashMap<>();
                envelope.put("version", 1);
                envelope.put("salt", Base64.getEncoder().encodeToString(salt));
                envelope.put("iv", Base64.getEncoder().encodeToString(iv));
                envelope.put("payload", Base64.getEncoder().encodeToString(encrypted));
                Path parent = secretsFile.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.writeString(secretsFile, codec.toJson(envelope), StandardCharsets.UTF_8);
            } finally {
                Arrays.fill(encrypted, (byte) 0);
            }
        } catch (IOException | GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to write encrypted secrets to " + secretsFile, exception);
        } finally {
            Arrays.fill(plaintext, (byte) 0);
        }
    }

    private byte[] encrypt(byte[] plaintext, byte[] salt, byte[] iv) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(salt), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        return cipher.doFinal(plaintext);
    }

    private byte[] decrypt(byte[] encrypted, byte[] salt, byte[] iv) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(salt), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        return cipher.doFinal(encrypted);
    }

    private SecretKey deriveKey(byte[] salt) throws GeneralSecurityException {
        char[] seed = machineSeed().toCharArray();
        try {
            PBEKeySpec spec = new PBEKeySpec(seed, salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF);
            byte[] encoded = factory.generateSecret(spec).getEncoded();
            try {
                return new SecretKeySpec(encoded, "AES");
            } finally {
                Arrays.fill(encoded, (byte) 0);
            }
        } finally {
            Arrays.fill(seed, '\0');
        }
    }

    private String machineSeed() {
        try {
            String host = InetAddress.getLocalHost().getHostName();
            return System.getProperty("user.name", "")
                + '|'
                + System.getProperty("os.name", "")
                + '|'
                + System.getProperty("os.arch", "")
                + '|'
                + host;
        } catch (IOException exception) {
            return System.getProperty("user.name", "")
                + '|'
                + System.getProperty("os.name", "")
                + '|'
                + System.getProperty("os.arch", "");
        }
    }

    private byte[] decode(Object value) {
        return Base64.getDecoder().decode(String.valueOf(value));
    }
}
