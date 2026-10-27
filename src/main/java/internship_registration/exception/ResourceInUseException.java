package internship_registration.exception;

/** Thrown when a record cannot be deleted or moved because other records still use it. Mapped to HTTP 409. */
public class ResourceInUseException extends RuntimeException {

    public ResourceInUseException(String message) {
        super(message);
    }
}