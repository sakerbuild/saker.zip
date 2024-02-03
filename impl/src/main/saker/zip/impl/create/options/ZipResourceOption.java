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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;

import saker.build.file.path.SakerPath;
import saker.std.api.file.location.FileLocation;
import saker.zip.api.create.ZipResourceEntry;

public abstract class ZipResourceOption {

	protected FileLocation fileLocation;

	protected ZipResourceOption(FileLocation filePath) {
		this.fileLocation = filePath;
	}

	public static ZipResourceOption create(FileLocation filePath, SakerPath archivePath) {
		return new PathZipResourceOption(filePath, archivePath);
	}

	public static ZipResourceOption create(FileLocation filePath, ZipResourceEntry resourceEntry) {
		return new ResourceEntryZipResourceOption(filePath, resourceEntry);
	}

	// Optimized read and write functions to reduce the number of objects written to the output streams
	// as we know all the subclasses and their serialization layouts

	/**
	 * Reads from an external object input based on the known layout of the {@link ZipResourceOption} implementations.
	 */
	public static ZipResourceOption readFromExternal(ObjectInput in) throws ClassNotFoundException, IOException {
		FileLocation fileLocation = (FileLocation) in.readObject();
		Object obj = in.readObject();
		if (obj instanceof SakerPath) {
			return new PathZipResourceOption(fileLocation, (SakerPath) obj);
		}
		return new ResourceEntryZipResourceOption(fileLocation, (ZipResourceEntry) obj);
	}

	/**
	 * Writes the {@link ZipResourceOption} to the given {@link ObjectOutput} in a manner to be read using
	 * {@link #readFromExternal(ObjectInput)}.
	 */
	public static void writeToExternal(ObjectOutput out, ZipResourceOption opt) throws IOException {
		opt.writeExternal(out);
	}

	public FileLocation getFileLocation() {
		return fileLocation;
	}

	public abstract ZipResourceEntry getArchiveResourceEntry();

	protected void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(fileLocation);
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
