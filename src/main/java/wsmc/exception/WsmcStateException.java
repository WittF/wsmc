package wsmc.exception;

/**
 * Exception thrown when an operation is performed in an invalid state.
 * For example, when ArgHolder operations are called out of order.
 */
public class WsmcStateException extends WsmcException {
	private static final long serialVersionUID = 1L;

	public WsmcStateException(String message) {
		super(message);
	}

	public WsmcStateException(String message, Throwable cause) {
		super(message, cause);
	}
}
