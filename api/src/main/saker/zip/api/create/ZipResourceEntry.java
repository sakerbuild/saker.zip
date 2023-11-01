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

	public static ZipResourceEntry create(SakerPath entrypath) {
		return new ZipResourceEntry(entrypath, null, -1, -1);
	}

	public static ZipResourceEntry create(SakerPath entrypath, FileTime modificationTime) {
		return new ZipResourceEntry(entrypath, modificationTime, -1, -1);
	}

	public static ZipResourceEntry stored(SakerPath entrypath) {
		return new ZipResourceEntry(entrypath, null, ZipEntry.STORED, -1);
	}

	public static ZipResourceEntry stored(SakerPath entrypath, FileTime modificationTime) {
		return new ZipResourceEntry(entrypath, modificationTime, ZipEntry.STORED, -1);
	}

	public static ZipResourceEntry deflated(SakerPath entrypath) {
		return new ZipResourceEntry(entrypath, null, ZipEntry.DEFLATED, -1);
	}

	public static ZipResourceEntry deflated(SakerPath entrypath, FileTime modificationTime) {
		return new ZipResourceEntry(entrypath, modificationTime, ZipEntry.DEFLATED, -1);
	}

	public static ZipResourceEntry deflated(SakerPath entrypath, int level) {
		return new ZipResourceEntry(entrypath, null, ZipEntry.DEFLATED, level);
	}

	public static ZipResourceEntry deflated(SakerPath entrypath, FileTime modificationTime, int level) {
		return new ZipResourceEntry(entrypath, modificationTime, ZipEntry.DEFLATED, level);
	}

	public static ZipResourceEntry from(ZipEntry zipentry) {
		return new ZipResourceEntry(SakerPath.valueOf(zipentry.getName()), zipentry.getLastModifiedTime(),
				zipentry.getMethod(), -1);
	}

	public SakerPath getEntryPath() {
		return entryPath;
	}

	public FileTime getModificationTime() {
		return modificationTime;
	}

	public int getLevel() {
		return level;
	}

	public int getMethod() {
		return method;
	}

	public ZipResourceEntry asStoredEntry() {
		return new ZipResourceEntry(entryPath, modificationTime, ZipOutputStream.STORED, -1);
	}

	public ZipResourceEntry asDeflatedEntry(int level) {
		return new ZipResourceEntry(entryPath, modificationTime, ZipOutputStream.DEFLATED, level);
	}

	public ZipResourceEntry asDeflatedEntry() {
		return new ZipResourceEntry(entryPath, modificationTime, ZipOutputStream.DEFLATED, -1);
	}

	public ZipResourceEntry withEntryPath(SakerPath entryPath) {
		return new ZipResourceEntry(entryPath, modificationTime, method, level);
	}

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
