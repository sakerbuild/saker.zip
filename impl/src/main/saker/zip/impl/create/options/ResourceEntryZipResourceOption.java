package saker.zip.impl.create.options;

import java.io.IOException;
import java.io.ObjectOutput;
import java.util.Objects;

import saker.std.api.file.location.FileLocation;
import saker.zip.api.create.ZipResourceEntry;

final class ResourceEntryZipResourceOption extends ZipResourceOption {
	private ZipResourceEntry resourceEntry;

	protected ResourceEntryZipResourceOption(FileLocation filePath, ZipResourceEntry resourceEntry) {
		super(filePath);
		this.resourceEntry = resourceEntry;
	}

	@Override
	public ZipResourceEntry getArchiveResourceEntry() {
		return resourceEntry;
	}

	@Override
	protected void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeObject(resourceEntry);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(resourceEntry);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ResourceEntryZipResourceOption other = (ResourceEntryZipResourceOption) obj;
		if (resourceEntry == null) {
			if (other.resourceEntry != null)
				return false;
		} else if (!resourceEntry.equals(other.resourceEntry))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName());
		builder.append("[resourceEntry=");
		builder.append(resourceEntry);
		builder.append(", fileLocation=");
		builder.append(fileLocation);
		builder.append("]");
		return builder.toString();
	}

}
