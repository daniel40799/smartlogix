package com.smartlogix.exception;

/**
 * Unchecked exception thrown when a requested resource cannot be found, or when a
 * tenant-scoped lookup returns no result (i.e. the resource exists but belongs to a
 * different tenant).
 * <p>
 * Caught by {@link GlobalExceptionHandler#handleResourceNotFoundException} and mapped to
 * a {@code 404 Not Found} HTTP response.
 * </p>
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructs the exception with a custom message.
     *
     * @param message a human-readable description of what was not found
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Convenience constructor that formats a standard "not found" message from the resource
     * type name and its identifier.
     *
     * @param resourceName the name of the resource type (e.g. {@code "Order"})
     * @param id           the identifier that was searched for (typically a UUID)
     */
    public ResourceNotFoundException(String resourceName, Object id) {
        super(resourceName + " not found with id: " + id);
    }
}
