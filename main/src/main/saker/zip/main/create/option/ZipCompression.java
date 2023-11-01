package saker.zip.main.create.option;

import java.util.zip.ZipEntry;

import saker.nest.scriptinfo.reflection.annot.NestFieldInformation;
import saker.nest.scriptinfo.reflection.annot.NestInformation;
import saker.nest.scriptinfo.reflection.annot.NestTypeInformation;

@NestInformation("ZIP compression methods.")
@NestTypeInformation(enumValues = {

		@NestFieldInformation(value = "STORED", info = @NestInformation("Store the data as is, without compression.")),
		@NestFieldInformation(value = "DEFLATED", info = @NestInformation("Use deflate compression for the data.")),

})
public enum ZipCompression {
	DEFLATED(ZipEntry.DEFLATED),
	STORED(ZipEntry.STORED);

	public final int zipValue;

	private ZipCompression(int zipValue) {
		this.zipValue = zipValue;
	}

}
