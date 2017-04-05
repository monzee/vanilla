package ph.codeia.arch;

/**
 * This file is a part of the vanilla project.
 */

public enum LogLevel {
    D, I, E;

    public void to(Logger logger, String message, Object... fmtArgs) {
        if (logger.active(this)) {
            logger.log(this, String.format(message, fmtArgs));
        }
    }

    public void to(Logger logger, Throwable error) {
        if (logger.active(this)) {
            logger.log(this, error);
        }
    }

    public void to(Logger logger, Throwable error, String message, Object... fmtArgs) {
        to(logger, message, fmtArgs);
        to(logger, error);
    }
}
