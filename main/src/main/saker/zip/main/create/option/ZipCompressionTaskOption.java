package saker.zip.main.create.option;

import saker.nest.scriptinfo.reflection.annot.NestFieldInformation;
import saker.nest.scriptinfo.reflection.annot.NestInformation;
import saker.nest.scriptinfo.reflection.annot.NestTypeInformation;
import saker.nest.scriptinfo.reflection.annot.NestTypeUsage;

@NestInformation("Configures the compression to be used.\n"
		+ "Accepts one of the compression methods, STORED (for uncompressed data) or DEFLATE (for compressed).\n"
		+ "The option also accepts string with the format DEFLATED:<L> where <L> is the compression level.\n"
		+ "Passing a single integer as compression level (implying DEFLATED) is also supported.\n"
		+ "Extended configuration using the Method and Level fields are also possible.")

@NestTypeInformation(relatedTypes = @NestTypeUsage(ZipCompression.class))

@NestFieldInformation(value = "Method",
		type = @NestTypeUsage(ZipCompression.class),
		info = @NestInformation("Specifies the compression method."))
@NestFieldInformation(value = "Level",
		type = @NestTypeUsage(int.class),
		info = @NestInformation("Specifies the compression level (for DEFLATED compression)."))
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
