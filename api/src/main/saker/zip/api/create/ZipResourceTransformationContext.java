package saker.zip.api.create;

import java.io.OutputStream;
import java.nio.file.attribute.FileTime;
import java.util.Date;

import saker.build.file.path.SakerPath;

/**
 * Context providing write access to the created ZIP archive during resource transformation with
 * {@linkplain ZipResourceTransformer resource transformers}.
 * <p>
 * This interface should not be implemented by clients.
 */
public interface ZipResourceTransformationContext {
	/**
	 * Adds a directory entry to the created ZIP archive.
	 * <p>
	 * The added entry will be subject to further processing.
	 * 
	 * @param entrypath
	 *            The path to the added directory.
	 * @param modificationtime
	 *            The modification time for the entry, or <code>null</code> to use the
	 *            {@linkplain ZipCreationTaskBuilder#setModificationTime(Date) default}.
	 * @throws NullPointerException
	 *             If the entry path is <code>null</code>.
	 */
	public void appendDirectory(SakerPath entrypath, FileTime modificationtime) throws NullPointerException;

	/**
	 * Adds a file entry to the created ZIP archive.
	 * <p>
	 * The contents of the file should be written to the returned output stream.
	 * <p>
	 * <b>The stream must be closed by the caller.</b> If it is not closed, the entry will not be written to the
	 * archive.
	 * <p>
	 * The added entry will be subject to further processing.
	 * 
	 * @param entrypath
	 *            The path to the added file.
	 * @param modificationtime
	 *            The modification time for the entry, or <code>null</code> to use the
	 *            {@linkplain ZipCreationTaskBuilder#setModificationTime(Date) default}.
	 * @return The output stream to which the contents of the file should be written to. The stream must be closed by
	 *             the caller.
	 * @throws NullPointerException
	 *             If the entry path is <code>null</code>.
	 */
	public OutputStream appendFile(SakerPath entrypath, FileTime modificationtime) throws NullPointerException;
}
