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
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.zip.ZipEntry;

import saker.build.file.path.SakerPath;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.zip.api.create.IncludeResourceMapping;
import saker.zip.api.create.ZipResourceEntry;

public final class DeflatedCompressionIncludeResourceMapping implements IncludeResourceMapping, Externalizable {
	private static final long serialVersionUID = 1L;

	/**
	 * Cache with levels from -1 to 9
	 */
	private static final DeflatedCompressionIncludeResourceMapping[] CACHE = new DeflatedCompressionIncludeResourceMapping[11];

	static {
		for (int i = 0; i < CACHE.length; i++) {
			CACHE[i] = new DeflatedCompressionIncludeResourceMapping(i - 1);
		}
	}

	private int level;

	/**
	 * For {@link Externalizable}.
	 */
	public DeflatedCompressionIncludeResourceMapping() {
	}

	private DeflatedCompressionIncludeResourceMapping(int level) {
		this.level = level;
	}

	public static DeflatedCompressionIncludeResourceMapping get(int level) {
		if (level < -1) {
			//normalize negative level to -1
			level = -1;
		}
		if (level <= 9) {
			return CACHE[level + 1];
		}
		return new DeflatedCompressionIncludeResourceMapping(level);
	}

	@Override
	@SuppressWarnings("deprecation")
	public Set<SakerPath> mapResourcePath(SakerPath archivepath, boolean directory) {
		return Collections.emptySet();
	}

	@Override
	public Collection<? extends ZipResourceEntry> mapResource(ZipResourceEntry resourceentry, boolean directory) {
		if (resourceentry.getMethod() != ZipEntry.DEFLATED || resourceentry.getLevel() != level) {
			return ImmutableUtils.singletonSet(resourceentry.asDeflatedEntry(level));
		}
		return ImmutableUtils.singletonSet(resourceentry);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(level);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		level = in.readInt();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + level;
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
		DeflatedCompressionIncludeResourceMapping other = (DeflatedCompressionIncludeResourceMapping) obj;
		if (level != other.level)
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName());
		builder.append("[level=");
		builder.append(level);
		builder.append("]");
		return builder.toString();
	}

}
