package wsmc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main WSMC class providing centralized logging and configuration.
 */
public class WSMC {
	public static final String MODID = "wsmc";

	private static final Logger LOGGER = LoggerFactory.getLogger(MODID);

	private static final boolean DEBUG_ENABLED =
			System.getProperty("wsmc.debug", "false").equalsIgnoreCase("true");

	public static final boolean DUMP_BYTES_ENABLED =
			System.getProperty("wsmc.dumpBytes", "false").equalsIgnoreCase("true");

	/**
	 * Log a debug message. Only shown when wsmc.debug=true.
	 * @param msg the message to log
	 */
	public static void debug(String msg) {
		if (DEBUG_ENABLED) {
			LOGGER.info("[DEBUG] {}", msg);
		}
	}

	/**
	 * Log a debug message with format arguments.
	 * @param format the format string
	 * @param args the arguments
	 */
	public static void debug(String format, Object... args) {
		if (DEBUG_ENABLED) {
			LOGGER.info("[DEBUG] " + format, args);
		}
	}

	/**
	 * Log an info message.
	 * @param msg the message to log
	 */
	public static void info(String msg) {
		LOGGER.info(msg);
	}

	/**
	 * Log an info message with format arguments.
	 * @param format the format string
	 * @param args the arguments
	 */
	public static void info(String format, Object... args) {
		LOGGER.info(format, args);
	}

	/**
	 * Log a warning message.
	 * @param msg the message to log
	 */
	public static void warn(String msg) {
		LOGGER.warn(msg);
	}

	/**
	 * Log a warning message with format arguments.
	 * @param format the format string
	 * @param args the arguments
	 */
	public static void warn(String format, Object... args) {
		LOGGER.warn(format, args);
	}

	/**
	 * Log an error message.
	 * @param msg the message to log
	 */
	public static void error(String msg) {
		LOGGER.error(msg);
	}

	/**
	 * Log an error message with exception.
	 * @param msg the message to log
	 * @param throwable the exception
	 */
	public static void error(String msg, Throwable throwable) {
		LOGGER.error(msg, throwable);
	}

	/**
	 * Check if debug logging is enabled.
	 * @return true if debug is enabled
	 */
	public static boolean debug() {
		return DEBUG_ENABLED;
	}

	/**
	 * Check if byte dumping is enabled.
	 * @return true if byte dumping is enabled
	 */
	public static boolean dumpBytes() {
		return DUMP_BYTES_ENABLED;
	}
}
