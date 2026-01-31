package org.metalib.papifly.fx.docks.testutil;

import javafx.application.Platform;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class FxTestUtil {

    private FxTestUtil() {
    }

    public static void runFx(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });

        await(latch);
        rethrowIfAny(error.get());
    }

    public static <T> T callFx(Callable<T> action) {
        if (Platform.isFxApplicationThread()) {
            try {
                return action.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> value = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                value.set(action.call());
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });

        await(latch);
        rethrowIfAny(error.get());
        return value.get();
    }

    public static void waitForFxEvents() {
        runFx(() -> {
        });
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting for FX thread");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private static void rethrowIfAny(Throwable t) {
        if (t == null) return;
        if (t instanceof RuntimeException re) throw re;
        if (t instanceof Error err) throw err;
        throw new RuntimeException(t);
    }
}
