package org.metalib.papifly.fx.settings.secret;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
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
    void supportsBinarySecretsThroughDefaultApi() {
        EncryptedFileSecretStore store = new EncryptedFileSecretStore(tempDir.resolve("secrets.enc"));
        byte[] payload = "refresh-token".getBytes(StandardCharsets.UTF_8);

        store.putBytes("login:oauth:refresh:test:user", payload);

        assertArrayEquals(payload, store.getBytes("login:oauth:refresh:test:user").orElseThrow());
    }
}
