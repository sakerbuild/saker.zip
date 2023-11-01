/*
 * Copyright (C) 2020 Bence Sipka
 *
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package saker.zip.impl.create.options;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;

import saker.build.file.path.SakerPath;
import saker.std.api.file.location.FileLocation;
import saker.zip.api.create.ZipResourceEntry;

public abstract class ZipResourceOption implements Externalizable {
	private static final long serialVersionUID = 1L;

	protected FileLocation fileLocation;

	/**
	 * For {@link Externalizable}.
	 */
	public ZipResourceOption() {
	}

	public ZipResourceOption(FileLocation filePath) {
		this.fileLocation = filePath;
	}

	public static ZipResourceOption create(FileLocation filePath, SakerPath archivePath) {
		return new PathZipResourceOption(filePath, archivePath);
	}

	public static ZipResourceOption create(FileLocation filePath, ZipResourceEntry resourceEntry) {
		return new ResourceEntryZipResourceOption(filePath, resourceEntry);
	}

	public FileLocation getFileLocation() {
		return fileLocation;
	}

	public abstract ZipResourceEntry getArchiveResourceEntry();

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(fileLocation);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		fileLocation = (FileLocation) in.readObject();
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(fileLocation);
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
		if (fileLocation == null) {
			if (other.fileLocation != null)
				return false;
		} else if (!fileLocation.equals(other.fileLocation))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName());
		builder.append("[fileLocation=");
		builder.append(fileLocation);
		builder.append("]");
		return builder.toString();
	}
}
