package org.metalib.papifly.fx.settings.secret;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EncryptedFileSecretStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void storesRetrievesAndClearsSecrets() {
        EncryptedFileSecretStore store = new EncryptedFileSecretStore(tempDir.resolve("secrets.enc"));

        store.setSecret("github:pat", "token-123");
        store.setSecret("settings:openai:api-key", "sk-test");

        assertEquals("token-123", store.getSecret("github:pat").orElseThrow());
        assertEquals(Set.of("github:pat", "settings:openai:api-key"), store.listKeys());

        store.clearSecret("github:pat");

        assertTrue(store.getSecret("github:pat").isEmpty());
        assertFalse(store.listKeys().contains("github:pat"));
    }

    @Test
    void createsBackupOnSubsequentWrite() {
        Path secretsFile = tempDir.resolve("secrets.enc");
        EncryptedFileSecretStore store = new EncryptedFileSecretStore(secretsFile);

        store.setSecret("key1", "value1");
        store.setSecret("key2", "value2");

        assertTrue(Files.exists(secretsFile.resolveSibling("secrets.enc.bak")),
            "Backup should exist after second write");
    }

    @Test
    void recoversFromCorruptedFileUsingBackup() {
        Path secretsFile = tempDir.resolve("secrets.enc");
        EncryptedFileSecretStore store = new EncryptedFileSecretStore(secretsFile);
        store.setSecret("apikey", "sk-test-123");

        // Write again so .bak exists with the first state
        store.setSecret("other", "val");

        // Corrupt the primary file
        try {
            Files.writeString(secretsFile, "CORRUPTED DATA", StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // New store instance should recover from backup
        EncryptedFileSecretStore recovered = new EncryptedFileSecretStore(secretsFile);
        assertEquals("sk-test-123", recovered.getSecret("apikey").orElse("missing"));
        assertFalse(recovered.listKeys().contains("other"),
            "Backup was taken before 'other' was added");
    }

    @Test
    void resetsToEmptyWhenBothFilesCorrupted() throws Exception {
        Path secretsFile = tempDir.resolve("secrets.enc");
        Files.writeString(secretsFile, "CORRUPT", StandardCharsets.UTF_8);
        Files.writeString(secretsFile.resolveSibling("secrets.enc.bak"), "ALSO CORRUPT", StandardCharsets.UTF_8);

        EncryptedFileSecretStore store = new EncryptedFileSecretStore(secretsFile);
        assertTrue(store.listKeys().isEmpty(), "Should reset to empty when both files are corrupted");
    }

    @Test
    void supportsBinarySecretsThroughDefaultApi() {
        EncryptedFileSecretStore store = new EncryptedFileSecretStore(tempDir.resolve("secrets.enc"));
        byte[] payload = "refresh-token".getBytes(StandardCharsets.UTF_8);

        store.putBytes("login:oauth:refresh:test:user", payload);

        assertArrayEquals(payload, store.getBytes("login:oauth:refresh:test:user").orElseThrow());
    }
}
