package wsmc.exception;

/**
 * Exception thrown when there is a configuration error.
 */
public class WsmcConfigException extends WsmcException {
	private static final long serialVersionUID = 1L;

	public WsmcConfigException(String message) {
		super(message);
	}

	public WsmcConfigException(String message, Throwable cause) {
		super(message, cause);
	}
}
