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
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import saker.build.file.path.SakerPath;
import saker.build.file.path.WildcardPath;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.zip.api.create.IncludeResourceMapping;

public class WildcardFilterIncludeResourceMapping implements IncludeResourceMapping, Externalizable {
	private static final long serialVersionUID = 1L;

	private WildcardPath wildcard;

	/**
	 * For {@link Externalizable}.
	 */
	public WildcardFilterIncludeResourceMapping() {
	}

	public WildcardFilterIncludeResourceMapping(WildcardPath wildcard) throws NullPointerException {
		Objects.requireNonNull(wildcard, "wildcard");
		this.wildcard = wildcard;
	}

	@Override
	public Set<SakerPath> mapResourcePath(SakerPath archivepath, boolean directory) {
		if (!wildcard.includes(archivepath)) {
			return Collections.emptySet();
		}
		return ImmutableUtils.singletonSet(archivepath);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(wildcard);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		wildcard = (WildcardPath) in.readObject();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((wildcard == null) ? 0 : wildcard.hashCode());
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
		WildcardFilterIncludeResourceMapping other = (WildcardFilterIncludeResourceMapping) obj;
		if (wildcard == null) {
			if (other.wildcard != null)
				return false;
		} else if (!wildcard.equals(other.wildcard))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + wildcard + "]";
	}

}
