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

import saker.std.api.file.location.FileLocation;
import saker.zip.api.create.IncludeResourceMapping;

public final class ZipIncludeArchiveOption implements Externalizable {
	private static final long serialVersionUID = 1L;

	private FileLocation archive;
	private IncludeResourceMapping mapping;

	/**
	 * For {@link Externalizable}.
	 */
	public ZipIncludeArchiveOption() {
	}

	public ZipIncludeArchiveOption(FileLocation archive, IncludeResourceMapping mapping) {
		this.archive = archive;
		this.mapping = mapping;
	}

	public FileLocation getArchive() {
		return archive;
	}

	public IncludeResourceMapping getMapping() {
		return mapping;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(archive);
		out.writeObject(mapping);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		archive = (FileLocation) in.readObject();
		mapping = (IncludeResourceMapping) in.readObject();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((archive == null) ? 0 : archive.hashCode());
		result = prime * result + ((mapping == null) ? 0 : mapping.hashCode());
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
		ZipIncludeArchiveOption other = (ZipIncludeArchiveOption) obj;
		if (archive == null) {
			if (other.archive != null)
				return false;
		} else if (!archive.equals(other.archive))
			return false;
		if (mapping == null) {
			if (other.mapping != null)
				return false;
		} else if (!mapping.equals(other.mapping))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + (archive != null ? "archive=" + archive + ", " : "")
				+ (mapping != null ? "mapping=" + mapping : "") + "]";
	}

}
