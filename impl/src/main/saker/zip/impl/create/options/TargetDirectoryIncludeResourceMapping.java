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
import java.util.Set;

import saker.build.exception.InvalidPathFormatException;
import saker.build.file.path.SakerPath;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.zip.api.create.IncludeResourceMapping;

public final class TargetDirectoryIncludeResourceMapping implements IncludeResourceMapping, Externalizable {
	private static final long serialVersionUID = 1L;

	private SakerPath targetDirectory;

	/**
	 * For {@link Externalizable}.
	 */
	public TargetDirectoryIncludeResourceMapping() {
	}

	public TargetDirectoryIncludeResourceMapping(SakerPath targetDirectory)
			throws NullPointerException, InvalidPathFormatException {
		Objects.requireNonNull(targetDirectory, "target directory");
		if (!targetDirectory.isForwardRelative()) {
			throw new InvalidPathFormatException("Target directory path must be forward relative: " + targetDirectory);
		}
		this.targetDirectory = targetDirectory;
	}

	@Override
	public Set<SakerPath> mapResourcePath(SakerPath archivepath, boolean directory) {
		return ImmutableUtils.singletonSet(targetDirectory.resolve(archivepath));
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(targetDirectory);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		targetDirectory = (SakerPath) in.readObject();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((targetDirectory == null) ? 0 : targetDirectory.hashCode());
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
		TargetDirectoryIncludeResourceMapping other = (TargetDirectoryIncludeResourceMapping) obj;
		if (targetDirectory == null) {
			if (other.targetDirectory != null)
				return false;
		} else if (!targetDirectory.equals(other.targetDirectory))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + targetDirectory + "]";
	}

}
