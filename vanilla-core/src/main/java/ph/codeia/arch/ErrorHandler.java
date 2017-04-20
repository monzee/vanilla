package ph.codeia.arch;

/**
 * This file is a part of the vanilla project.
 */

public interface ErrorHandler<C> {
    void handle(Throwable error, C client);
}
