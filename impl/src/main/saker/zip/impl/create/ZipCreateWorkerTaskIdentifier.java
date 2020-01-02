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
package saker.zip.impl.create;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import saker.build.file.path.SakerPath;
import saker.build.task.identifier.TaskIdentifier;

public class ZipCreateWorkerTaskIdentifier implements TaskIdentifier, Externalizable {
	private static final long serialVersionUID = 1L;

	protected SakerPath outputPath;

	/**
	 * For {@link Externalizable}.
	 */
	public ZipCreateWorkerTaskIdentifier() {
	}

	public ZipCreateWorkerTaskIdentifier(SakerPath outputPath) {
		this.outputPath = outputPath;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(outputPath);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		outputPath = (SakerPath) in.readObject();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((outputPath == null) ? 0 : outputPath.hashCode());
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
		ZipCreateWorkerTaskIdentifier other = (ZipCreateWorkerTaskIdentifier) obj;
		if (outputPath == null) {
			if (other.outputPath != null)
				return false;
		} else if (!outputPath.equals(other.outputPath))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[outputPath=" + outputPath + "]";
	}

}