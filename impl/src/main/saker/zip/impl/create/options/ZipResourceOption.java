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

import saker.build.file.path.SakerPath;
import saker.std.api.file.location.FileLocation;
import saker.zip.api.create.ZipResourceEntry;

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

	public ZipResourceEntry getArchiveResourceEntry() {
		return ZipResourceEntry.create(archivePath);
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
