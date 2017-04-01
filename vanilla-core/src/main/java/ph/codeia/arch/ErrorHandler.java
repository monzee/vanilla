package ph.codeia.arch;

/**
 * This file is a part of the vanilla project.
 */

public interface ErrorHandler<V> {
    void handle(Throwable error, V view);
}
