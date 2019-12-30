package saker.zip.api.create;

import java.io.Externalizable;

/**
 * Stateless factory class for creating a {@linkplain ZipResourceTransformer ZIP resource transformer}.
 * <p>
 * Clients should implement this interface.
 * <p>
 * Implementations should adhere the {@link #hashCode()} and {@link #equals(Object)} specification, and are recommended
 * to be {@link Externalizable}.
 */
public interface ZipResourceTransformerFactory {
	/**
	 * Creates a new transformer.
	 * <p>
	 * This is called when the ZIP archive creation is started. The transformer is only used for transforming a single
	 * ZIP archive.
	 * 
	 * @return The created ZIP transformer.
	 */
	public ZipResourceTransformer createTransformer();

	@Override
	public int hashCode();

	@Override
	public boolean equals(Object obj);
}
