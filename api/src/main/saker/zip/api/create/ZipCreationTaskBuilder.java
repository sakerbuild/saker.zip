package saker.zip.api.create;

import java.util.Date;

import saker.build.exception.InvalidPathFormatException;
import saker.build.file.path.SakerPath;
import saker.build.task.TaskFactory;
import saker.build.task.identifier.TaskIdentifier;
import saker.std.api.file.location.FileLocation;

/**
 * Interface for configuring and creating a ZIP creation task.
 * <p>
 * The builder allows setting up a build task invocation that generates a ZIP archive with the configured contents.
 * <p>
 * The builder can be instantiated using {@link #newBuilder()}.
 * <p>
 * The interface may be implemented in some cases, but in general, it is not recommended doing so.
 */
public interface ZipCreationTaskBuilder {
	/**
	 * Sets the output location where the archive should be created to.
	 * <p>
	 * The specified path must be {@linkplain SakerPath#isAbsolute() absolute}.
	 * <p>
	 * Setting the output path is <b>requierd</b> before calling the build methods.
	 * 
	 * @param outputPath
	 *            The output path.
	 * @throws NullPointerException
	 *             If the argument is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             If the argument is not an {@linkplain SakerPath#isAbsolute() absolute} path.
	 */
	public void setOutputPath(SakerPath outputPath) throws NullPointerException, IllegalArgumentException;

	/**
	 * Sets the default modification time that should be set for the archive entries.
	 * <p>
	 * The archive entries will have this modification time set for them.
	 * <p>
	 * This modification time will not be set for {@linkplain #addIncludeArchive(FileLocation, IncludeResourceMapping)
	 * included archive entries}. The modification time for them is based on their modification time in the source
	 * archive.
	 * 
	 * @param modificationTime
	 *            The modification time or <code>null</code> to use the default.
	 */
	public void setModificationTime(Date modificationTime);

	/**
	 * Adds an archive of which the contents of should be added to the created ZIP.
	 * <p>
	 * The archiving task will enumerate the entries in the specified ZIP archive and include them with the paths
	 * specified with the given mapping.
	 * 
	 * @param archive
	 *            The archive from which the entries should be included.
	 * @param mapping
	 *            The resource mapping that defines the archive paths of the included entries. If <code>null</code>, the
	 *            {@linkplain IncludeResourceMapping#identity() identity mapping} is used.
	 * @throws NullPointerException
	 *             If the archive is <code>null</code>.
	 */
	public void addIncludeArchive(FileLocation archive, IncludeResourceMapping mapping) throws NullPointerException;

	/**
	 * Adds a file to be added to the created archive.
	 * 
	 * @param location
	 *            The location of the file.
	 * @param archivepath
	 *            The archive path that the file should be added as.
	 * @throws NullPointerException
	 *             If any of the arguments are <code>null</code>.
	 * @throws InvalidPathFormatException
	 *             If the resouces archive path is not {@linkplain SakerPath#isForwardRelative() forward relative}.
	 */
	public void addResource(FileLocation location, SakerPath archivepath)
			throws NullPointerException, InvalidPathFormatException;

	/**
	 * Adds a resource transformer to the ZIP creation task.
	 * <p>
	 * Resource transformers can be used to modify the contents of the created ZIP in a dynamic manner.
	 * 
	 * @param transformer
	 *            The resource transformer.
	 * @throws NullPointerException
	 *             If the argument is <code>null</code>.
	 */
	public void addResourceTransformer(ZipResourceTransformerFactory transformer) throws NullPointerException;

	/**
	 * Builds a task identifier that can be used to start the {@linkplain #buildTaskFactory() task}.
	 * <p>
	 * The builder can be reused after this call.
	 * 
	 * @return The task identifier.
	 * @throws IllegalStateException
	 *             If the required properties of the task is not set.
	 */
	public TaskIdentifier buildTaskIdentifier() throws IllegalStateException;

	/**
	 * Builds the task based on the current state of the builder.
	 * <p>
	 * The builder can be reused after this call.
	 * 
	 * @return The task factory.
	 * @throws IllegalStateException
	 *             If the required properties of the task is not set.
	 */
	public TaskFactory<?> buildTaskFactory() throws IllegalStateException;

	/**
	 * Creates a new builder instance.
	 * 
	 * @return The created builder.
	 */
	public static ZipCreationTaskBuilder newBuilder() {
		return new ZipCreationTaskBuilderImpl();
	}
}