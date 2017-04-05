package ph.codeia.arch;

/**
 * This file is a part of the vanilla project.
 */

public interface Logger {
    boolean active(LogLevel level);
    void log(LogLevel level, String message);
    void log(LogLevel level, Throwable error);
}
