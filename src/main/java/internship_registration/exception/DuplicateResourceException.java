package internship_registration.exception;

/** Thrown when a master-data record (university, district, college) already exists. Mapped to HTTP 409. */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}