package saker.zip.impl.create.options;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import saker.build.file.path.SakerPath;
import saker.std.api.file.location.FileLocation;

public class ZipResourceOption implements Externalizable {
	private static final long serialVersionUID = 1L;

	private FileLocation fileLocation;
	private SakerPath archivePath;

	/**
	 * For {@link Externalizable}.
	 */
	public ZipResourceOption() {
	}

	public ZipResourceOption(FileLocation filePath, SakerPath archivePath) {
		this.fileLocation = filePath;
		this.archivePath = archivePath;
	}

	public FileLocation getFileLocation() {
		return fileLocation;
	}

	public SakerPath getArchivePath() {
		return archivePath;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(fileLocation);
		out.writeObject(archivePath);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		fileLocation = (FileLocation) in.readObject();
		archivePath = (SakerPath) in.readObject();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((archivePath == null) ? 0 : archivePath.hashCode());
		result = prime * result + ((fileLocation == null) ? 0 : fileLocation.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ZipResourceOption other = (ZipResourceOption) obj;
		if (archivePath == null) {
			if (other.archivePath != null)
				return false;
		} else if (!archivePath.equals(other.archivePath))
			return false;
		if (fileLocation == null) {
			if (other.fileLocation != null)
				return false;
		} else if (!fileLocation.equals(other.fileLocation))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + (fileLocation != null ? "filePath=" + fileLocation + ", " : "")
				+ (archivePath != null ? "archivePath=" + archivePath : "") + "]";
	}

}
