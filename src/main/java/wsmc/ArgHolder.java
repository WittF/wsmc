package wsmc;

import wsmc.exception.WsmcStateException;

/**
 * Thread-safe holder for passing arguments through mixins.
 * Uses ThreadLocal to maintain per-thread state.
 * <p>
 * Important: Always use try-finally to ensure cleanup:
 * <pre>
 * holder.push(value);
 * try {
 *     // use the value
 *     T value = holder.pop();
 * } finally {
 *     holder.cleanup();
 * }
 * </pre>
 */
public final class ArgHolder<T> {
	private final ThreadLocal<T> wsAddress =
			ThreadLocal.withInitial(() -> null);

	private final boolean nullable;

	private ArgHolder(boolean nullable) {
		this.nullable = nullable;
	}

	/**
	 * Create an ArgHolder that allows null values.
	 */
	public static <T> ArgHolder<T> nullable() {
		return new ArgHolder<>(true);
	}

	/**
	 * Create an ArgHolder that requires non-null values.
	 */
	public static <T> ArgHolder<T> nonnull() {
		return new ArgHolder<>(false);
	}

	/**
	 * Push a value to the current thread's context.
	 * @param val the value to push
	 * @throws WsmcStateException if a previous value hasn't been consumed
	 */
	public void push(T val) {
		if (this.wsAddress.get() != null) {
			throw new WsmcStateException("Previous ArgHolder value has not been consumed! " +
					"This may indicate a mixin injection point mismatch.");
		}

		this.wsAddress.set(val);
	}

	/**
	 * Get the value without removing it from the context.
	 * @return the value set previously
	 * @throws WsmcStateException if no value is available and nullable is false
	 */
	public T peek() {
		T ret = this.wsAddress.get();

		if (ret == null && !nullable) {
			throw new WsmcStateException("ArgHolder value is not available! " +
					"Ensure push() was called before peek().");
		}

		return ret;
	}

	/**
	 * Get the value and remove it from the context.
	 * Automatically calls cleanup() to prevent memory leaks.
	 * @return the value set previously
	 * @throws WsmcStateException if no value is available and nullable is false
	 */
	public T pop() {
		try {
			return this.peek();
		} finally {
			// Always cleanup to prevent ThreadLocal memory leaks
			this.cleanup();
		}
	}

	/**
	 * Explicitly cleanup the ThreadLocal to prevent memory leaks.
	 * This is automatically called by pop(), but can be called manually
	 * in error recovery scenarios.
	 */
	public void cleanup() {
		this.wsAddress.remove();
	}
}
