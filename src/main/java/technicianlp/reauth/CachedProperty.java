package technicianlp.reauth;

final class CachedProperty<T> {
	private final T invalid;
	private final long validity;
	private T value;
	private boolean valid;
	private long timestamp;

	protected CachedProperty(final long validity, final T invalid) {
		this.validity = validity;
		this.invalid = invalid;
	}

	protected final T get() {
		return valid ? value : invalid;
	}

	protected final boolean check() {
		if (System.currentTimeMillis() - timestamp > validity) {
			invalidate();
		}

		return valid;
	}

	protected final void invalidate() {
		valid = false;
	}

	protected final void set(final T value) {
		this.value = value;
		this.timestamp = System.currentTimeMillis();
		this.valid = true;
	}
}
