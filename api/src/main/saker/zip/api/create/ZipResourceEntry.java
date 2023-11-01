package saker.zip.api.create;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import saker.build.file.path.SakerPath;

/**
 * Immutable data class containing information about an entry in a ZIP archive.
 * <p>
 * This class is used to convey information about ZIP archive entries during archive creation and transformation. It is
 * mainly used to change the attributes of the output ZIP entries.
 * <p>
 * Use the static factory methods to get a new instance.
 * 
 * @since saker.zip 0.8.5
 */
public final class ZipResourceEntry implements Externalizable {
	private static final long serialVersionUID = 1L;

	protected SakerPath entryPath;
	protected FileTime modificationTime;

	protected int method;
	protected int level;

	/**
	 * For {@link Externalizable}.
	 */
	@Deprecated
	public ZipResourceEntry() {
	}

	private ZipResourceEntry(SakerPath entryPath, FileTime modificationTime, int method, int level) {
		Objects.requireNonNull(entryPath, "entryPath");
		if (level < -1) {
			//normalize negative level to -1
			level = -1;
		}

		this.entryPath = entryPath;
		this.modificationTime = modificationTime;
		this.method = method;
		this.level = level;
	}

	/**
	 * Creates a new instance with the given entry path.
	 * 
	 * @param entrypath
	 *            The entry path.
	 * @return The new instance.
	 * @throws NullPointerException
	 *             If the entry path is <code>null</code>.
	 */
	public static ZipResourceEntry create(SakerPath entrypath) throws NullPointerException {
		return new ZipResourceEntry(entrypath, null, -1, -1);
	}

	/**
	 * Creates a new instance with the given entry path and modification time.
	 * 
	 * @param entrypath
	 *            The entry path.
	 * @param modificationTime
	 *            The last modification time.
	 * @return
	 * @throws NullPointerException
	 *             If the entry path is <code>null</code>.
	 */
	public static ZipResourceEntry create(SakerPath entrypath, FileTime modificationTime) throws NullPointerException {
		return new ZipResourceEntry(entrypath, modificationTime, -1, -1);
	}

	/**
	 * Creates a new instance with the given entry path and no compression ({@link ZipEntry#STORED STORED}) .
	 * 
	 * @param entrypath
	 *            The entry path.
	 * @return The new instance.
	 * @throws NullPointerException
	 *             If the entry path is <code>null</code>.
	 */
	public static ZipResourceEntry stored(SakerPath entrypath) throws NullPointerException {
		return new ZipResourceEntry(entrypath, null, ZipEntry.STORED, -1);
	}

	/**
	 * Creates a new instance with the given entry path, modification time and no compression ({@link ZipEntry#STORED
	 * STORED}).
	 * 
	 * @param entrypath
	 *            The entry path.
	 * @param modificationTime
	 *            The last modification time.
	 * @return The new instance.
	 * @throws NullPointerException
	 *             If the entry path is <code>null</code>.
	 */
	public static ZipResourceEntry stored(SakerPath entrypath, FileTime modificationTime) throws NullPointerException {
		return new ZipResourceEntry(entrypath, modificationTime, ZipEntry.STORED, -1);
	}

	/**
	 * Creates a new instance with the given entry path and {@link ZipEntry#DEFLATED DEFLATED} compression.
	 * 
	 * @param entrypath
	 *            The entry path.
	 * @return The new instance.
	 * @throws NullPointerException
	 *             If the entry path is <code>null</code>.
	 */
	public static ZipResourceEntry deflated(SakerPath entrypath) throws NullPointerException {
		return new ZipResourceEntry(entrypath, null, ZipEntry.DEFLATED, -1);
	}

	/**
	 * Creates a new instance with the given entry path, modification time and {@link ZipEntry#DEFLATED DEFLATED}
	 * compression.
	 * 
	 * @param entrypath
	 *            The entry path.
	 * @param modificationTime
	 *            The last modification time.
	 * @return The new instance.
	 * @throws NullPointerException
	 *             If the entry path is <code>null</code>.
	 */
	public static ZipResourceEntry deflated(SakerPath entrypath, FileTime modificationTime)
			throws NullPointerException {
		return new ZipResourceEntry(entrypath, modificationTime, ZipEntry.DEFLATED, -1);
	}

	/**
	 * Creates a new instance with the given entry path and the specified level of {@link ZipEntry#DEFLATED DEFLATED}
	 * compression.
	 * 
	 * @param entrypath
	 *            The entry path.
	 * @param level
	 *            The compression level.
	 * @return The new instance.
	 * @throws NullPointerException
	 *             If the entry path is <code>null</code>.
	 */
	public static ZipResourceEntry deflated(SakerPath entrypath, int level) throws NullPointerException {
		return new ZipResourceEntry(entrypath, null, ZipEntry.DEFLATED, level);
	}

	/**
	 * Creates a new instance with the given entry path, modification time and the specified level of
	 * {@link ZipEntry#DEFLATED DEFLATED} compression.
	 * 
	 * @param entrypath
	 *            The entry path.
	 * @param modificationTime
	 *            The last modification time.
	 * @return The new instance.
	 * @throws NullPointerException
	 *             If the entry path is <code>null</code>.
	 */
	public static ZipResourceEntry deflated(SakerPath entrypath, FileTime modificationTime, int level)
			throws NullPointerException {
		return new ZipResourceEntry(entrypath, modificationTime, ZipEntry.DEFLATED, level);
	}

	/**
	 * Creates a new instance based on the argument {@link ZipEntry}.
	 * 
	 * @param zipentry
	 *            The ZIP entry.
	 * @return The new instance.
	 * @throws NullPointerException
	 *             If the zip entry is <code>null</code>.
	 */
	public static ZipResourceEntry from(ZipEntry zipentry) throws NullPointerException {
		Objects.requireNonNull(zipentry, "zip entry");
		return new ZipResourceEntry(SakerPath.valueOf(zipentry.getName()), zipentry.getLastModifiedTime(),
				zipentry.getMethod(), -1);
	}

	/**
	 * Gets the entry path in the ZIP archive.
	 * 
	 * @return The entry path, not <code>null</code>.
	 * @see ZipEntry#getName()
	 */
	public SakerPath getEntryPath() {
		return entryPath;
	}

	/**
	 * Gets the modification time of the entry.
	 * <p>
	 * If the modification time is <code>null</code>, then it may be unknown, or the default modification time will be
	 * used when creating the associated ZIP entry.
	 * 
	 * @return The modification time or <code>null</code> if not available.
	 * @see ZipEntry#setLastModifiedTime(FileTime)
	 */
	public FileTime getModificationTime() {
		return modificationTime;
	}

	/**
	 * Gets the compression method constant.
	 * <p>
	 * The value is one of the {@link ZipEntry} method constants.
	 * 
	 * @return The compression method, or negative integer if not set.
	 * @see ZipEntry#STORED
	 * @see ZipEntry#DEFLATED
	 * @see ZipEntry#setMethod(int)
	 */
	public int getMethod() {
		return method;
	}

	/**
	 * Gets the compression level that should be used for deflate compression.
	 * 
	 * @return The compression level or negative integer if the default compression level is to be used.
	 * @see ZipOutputStream#setLevel(int)
	 */
	public int getLevel() {
		return level;
	}

	/**
	 * Creates a new instance by copying this one and setting the compression method to {@link ZipEntry#STORED STORED}.
	 * 
	 * @return The new instance.
	 */
	public ZipResourceEntry asStoredEntry() {
		return new ZipResourceEntry(entryPath, modificationTime, ZipOutputStream.STORED, -1);
	}

	/**
	 * Creates a new instance by setting the compression method to {@link ZipEntry#DEFLATED DEFLATED} and setting the
	 * compression level.
	 * 
	 * @param level
	 *            The compression leve.
	 * @return The new instance.
	 */
	public ZipResourceEntry asDeflatedEntry(int level) {
		return new ZipResourceEntry(entryPath, modificationTime, ZipOutputStream.DEFLATED, level);
	}

	/**
	 * Creates a new instance by setting the compression method to {@link ZipEntry#DEFLATED DEFLATED} and setting the
	 * compression level to default.
	 * 
	 * @return The new instance.
	 */
	public ZipResourceEntry asDeflatedEntry() {
		return new ZipResourceEntry(entryPath, modificationTime, ZipOutputStream.DEFLATED, -1);
	}

	/**
	 * Creates a new instance with a new entry path.
	 * 
	 * @param entryPath
	 *            The entry path.
	 * @return The new instance.
	 * @throws NullPointerException
	 *             If the entry path is <code>null</code>
	 */
	public ZipResourceEntry withEntryPath(SakerPath entryPath) throws NullPointerException {
		return new ZipResourceEntry(entryPath, modificationTime, method, level);
	}

	/**
	 * Creates a new instance that has a different modification time.
	 * 
	 * @param modificationTime
	 *            The modification time.
	 * @return The new instance.
	 */
	public ZipResourceEntry withModificationTime(FileTime modificationTime) {
		return new ZipResourceEntry(entryPath, modificationTime, method, level);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(entryPath);
		out.writeObject(modificationTime);
		out.writeInt(method);
		out.writeInt(level);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		entryPath = (SakerPath) in.readObject();
		modificationTime = (FileTime) in.readObject();
		method = in.readInt();
		level = in.readInt();
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(entryPath);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ZipResourceEntry other = (ZipResourceEntry) obj;
		if (entryPath == null) {
			if (other.entryPath != null)
				return false;
		} else if (!entryPath.equals(other.entryPath))
			return false;
		if (level != other.level)
			return false;
		if (method != other.method)
			return false;
		if (modificationTime == null) {
			if (other.modificationTime != null)
				return false;
		} else if (!modificationTime.equals(other.modificationTime))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName());
		builder.append("[entryPath=");
		builder.append(entryPath);
		builder.append(", modificationTime=");
		builder.append(modificationTime);
		builder.append(", method=");
		builder.append(method);
		builder.append(", level=");
		builder.append(level);
		builder.append("]");
		return builder.toString();
	}

}
