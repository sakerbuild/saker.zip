package saker.zip.api.create;

import saker.build.file.path.SakerPath;

/**
 * Represents the output of a ZIP archive creation task.
 */
public interface ZipCreatorTaskOutput {
	/**
	 * Gets the output execution path of the created ZIP archive.
	 * 
	 * @return The output path.
	 */
	public SakerPath getPath();
}
