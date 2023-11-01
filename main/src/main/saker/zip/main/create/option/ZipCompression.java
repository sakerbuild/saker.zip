package saker.zip.main.create.option;

import java.util.zip.ZipEntry;

public enum ZipCompression {
	DEFLATED(ZipEntry.DEFLATED),
	STORED(ZipEntry.STORED);

	public final int zipValue;

	private ZipCompression(int zipValue) {
		this.zipValue = zipValue;
	}

}
