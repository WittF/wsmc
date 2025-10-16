package wsmc.exception;

/**
 * Base exception for all WSMC-related errors.
 */
public class WsmcException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public WsmcException(String message) {
		super(message);
	}

	public WsmcException(String message, Throwable cause) {
		super(message, cause);
	}

	public WsmcException(Throwable cause) {
		super(cause);
	}
}
