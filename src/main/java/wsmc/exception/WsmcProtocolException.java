package wsmc.exception;

/**
 * Exception thrown when there is a protocol-level error during WebSocket communication.
 */
public class WsmcProtocolException extends WsmcException {
	private static final long serialVersionUID = 1L;

	public WsmcProtocolException(String message) {
		super(message);
	}

	public WsmcProtocolException(String message, Throwable cause) {
		super(message, cause);
	}
}
