package saker.zip.api.create;

import java.io.IOException;
import java.io.InputStream;

import saker.build.file.path.SakerPath;

/**
 * A streaming ZIP archive resource transformer interface.
 * <p>
 * ZIP resource transformers are used to manipulate the contents of the created archive. They are created by a
 * {@link ZipResourceTransformerFactory}, and instantiated when the ZIP archive creation is started.
 * <p>
 * The resource transformers process the archive entries in a streaming way, meaning that as the entries are being
 * written to the result archive, the transformers are presented an opportunity to examine and modify or otherwise
 * manipulate the contents of an entry.
 * <p>
 * Resource transformers may modify the contents, write new entries, omit entries, and otherwise manipulate the created
 * archive. They work with a {@link ZipResourceTransformationContext} that provides write access to the created ZIP.
 * <p>
 * During an archive creation, multiple resource transformers may be present. When there are multiple transformers, the
 * processing will be done after each other. It may be the case that a resource transformer works with an already
 * transformed entry.
 * <p>
 * The methods {@link #process(ZipResourceTransformationContext, SakerPath, InputStream) process},
 * {@link #flush(ZipResourceTransformationContext) flush} and {@link #end(ZipResourceTransformationContext) end} are
 * called on a transformer in this order. The {@link #process(ZipResourceTransformationContext, SakerPath, InputStream)
 * process} method is called multiple times for each processed entry.
 * <p>
 * Clients should implement this interface.
 */
public interface ZipResourceTransformer {
	/**
	 * Processes the ZIP resource specified by the arguments.
	 * <p>
	 * This method is called by the ZIP archiver to ask the transformer to process the specified resource. The
	 * transformer can either consume the resource or not.
	 * <p>
	 * When a resource is consumed, it will not be passed to subsequent transformers, and will not be written to the ZIP
	 * archive output.
	 * <p>
	 * The transformers can decide to consume the resource, and write a resource to the archive using the transformation
	 * context.
	 * <p>
	 * If the transformer doesn't consume the resource, subsequent transformers will be asked to process it. If no
	 * transformer consumes the resource, it will be written to the archive as is.
	 * <p>
	 * Implementations should take care to implement guarding when they process and generate a resource with the same
	 * name. Not employing guarding may result in infinite looping by the transformation context.
	 * 
	 * @param context
	 *            The transformation context.
	 * @param resourcepath
	 *            The path of the resource in the archive.
	 * @param resourceinput
	 *            The input stream to the resource bytes, or <code>null</code> if the resource is a directory.
	 * @return <code>true</code> if the resource is consumed by this transformer, and shouldn't be written to the
	 *             output. <code>false</code> if the transformer doesn't consume this resource.
	 * @throws IOException
	 *             Transformers may throw in case of errors.
	 */
	public boolean process(ZipResourceTransformationContext context, SakerPath resourcepath, InputStream resourceinput)
			throws IOException;

	/**
	 * Asks the processor to write any pending resources to the ZIP archive.
	 * <p>
	 * Any written resources will be subject to further processing. Implementations should handle if
	 * {@link #process(ZipResourceTransformationContext, SakerPath, InputStream)} is called after this function being
	 * called.
	 * <p>
	 * The default implementation does nothing.
	 * 
	 * @param context
	 *            The transformation context.
	 * @throws IOException
	 *             Transformers may throw in case of errors.
	 */
	public default void flush(ZipResourceTransformationContext context) throws IOException {
	}

	/**
	 * Ends the transformation for this transformer.
	 * <p>
	 * This method is similar to {@link #flush(ZipResourceTransformationContext)}, however, this transformer won't be
	 * called for any more resource transformations.
	 * <p>
	 * Any generated resources won't be passed to
	 * {@link #process(ZipResourceTransformationContext, SakerPath, InputStream)}, however, they will be processed by
	 * subsequent transformers.
	 * <p>
	 * After this method is called, the transformer won't be used any more.
	 * <p>
	 * It is generally recommended to use {@link #flush(ZipResourceTransformationContext)} for end-of-transformation
	 * resource generation.
	 * <p>
	 * The default implementation does nothing.
	 * 
	 * @param context
	 *            The transformation context.
	 * @throws IOException
	 *             Transformers may throw in case of errors.
	 */
	public default void end(ZipResourceTransformationContext context) throws IOException {
	}
}
