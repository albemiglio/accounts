package it.albemiglio.accounts.core.modules;

public class MigrationException extends RuntimeException {

    public MigrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
