package saker.zip.impl.create.options;

import java.io.IOException;
import java.io.ObjectOutput;
import java.util.Objects;

import saker.build.file.path.SakerPath;
import saker.std.api.file.location.FileLocation;
import saker.zip.api.create.ZipResourceEntry;

final class PathZipResourceOption extends ZipResourceOption {
	private SakerPath archivePath;

	protected PathZipResourceOption(FileLocation filePath, SakerPath archivePath) {
		super(filePath);
		this.archivePath = archivePath;
	}

	public SakerPath getArchivePath() {
		return archivePath;
	}

	@Override
	public ZipResourceEntry getArchiveResourceEntry() {
		return ZipResourceEntry.create(archivePath);
	}

	@Override
	protected void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeObject(archivePath);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(archivePath);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		PathZipResourceOption other = (PathZipResourceOption) obj;
		if (archivePath == null) {
			if (other.archivePath != null)
				return false;
		} else if (!archivePath.equals(other.archivePath))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName());
		builder.append("[archivePath=");
		builder.append(archivePath);
		builder.append(", fileLocation=");
		builder.append(fileLocation);
		builder.append("]");
		return builder.toString();
	}

}
