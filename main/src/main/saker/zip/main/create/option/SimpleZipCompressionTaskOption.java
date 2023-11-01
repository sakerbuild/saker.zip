package saker.zip.main.create.option;

import saker.build.file.path.SakerPath;
import saker.zip.api.create.ZipResourceEntry;

public class SimpleZipCompressionTaskOption implements ZipCompressionTaskOption {

	private ZipCompression method;
	private int level;

	public SimpleZipCompressionTaskOption(ZipCompression method, int level) {
		this.method = method;
		this.level = level;
	}

	public SimpleZipCompressionTaskOption(ZipCompressionTaskOption copy) {
		this(copy.getMethod(), copy.getLevel());
	}

	@Override
	public ZipCompressionTaskOption clone() {
		return this;
	}

	@Override
	public ZipCompression getMethod() {
		return method;
	}

	@Override
	public int getLevel() {
		return level;
	}

	public static ZipResourceEntry createEntry(SakerPath entrypath, ZipCompressionTaskOption compression) {
		if (compression == null) {
			return ZipResourceEntry.create(entrypath);
		}
		ZipCompression method = compression.getMethod();
		int level = compression.getLevel();
		if (method == null) {
			if (level < 0) {
				//use defaults
				return ZipResourceEntry.create(entrypath);
			}
			//no method, but level is given, so use deflated 
			return ZipResourceEntry.deflated(entrypath, level);
		}
		switch (method) {
			case DEFLATED:
				return ZipResourceEntry.deflated(entrypath, level);
			case STORED:
				return ZipResourceEntry.stored(entrypath);
			default: {
				throw new UnsupportedOperationException("Unsupported compression method: " + method);
			}
		}
	}
}
