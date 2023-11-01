package saker.zip.api.create;

import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.zip.ZipOutputStream;

import saker.build.file.path.SakerPath;

public final class ZipResourceEntry {
	protected SakerPath entryPath;
	protected FileTime modificationTime;

	protected int method;
	protected int level;

	public ZipResourceEntry(SakerPath entryPath) {
		Objects.requireNonNull(entryPath, "entryPath");
		this.entryPath = entryPath;
		this.method = -1;
		this.level = -1;
	}

	public ZipResourceEntry(SakerPath entryPath, FileTime modificationTime) {
		Objects.requireNonNull(entryPath, "entryPath");
		this.entryPath = entryPath;
		this.modificationTime = modificationTime;
		this.method = -1;
		this.level = -1;
	}

	public ZipResourceEntry(SakerPath entryPath, FileTime modificationTime, int method, int level) {
		Objects.requireNonNull(entryPath, "entryPath");
		this.entryPath = entryPath;
		this.modificationTime = modificationTime;
		this.method = method;
		this.level = level;
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
