package saker.zip.main.create.option;

public interface ZipCompressionTaskOption {
	public default ZipCompressionTaskOption clone() {
		return new SimpleZipCompressionTaskOption(this);
	}

	public default ZipCompression getMethod() {
		return null;
	}

	public default int getLevel() {
		return -1;
	}

	public static ZipCompressionTaskOption valueOf(String value) {
		int colonidx = value.indexOf(':');
		ZipCompression method;
		int level = -1;
		if (colonidx >= 0) {
			level = Integer.parseInt(value.substring(colonidx + 1));
			method = ZipCompression.valueOf(value.substring(0, colonidx));
		} else {
			try {
				method = ZipCompression.valueOf(value);
			} catch (IllegalArgumentException e) {
				try {
					level = Integer.parseInt(value);
					method = null;
				} catch (NumberFormatException e2) {
					e.addSuppressed(e2);
					throw e;
				}
			}
		}
		return new SimpleZipCompressionTaskOption(method, level);
	}
}
