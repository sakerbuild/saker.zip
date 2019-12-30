package saker.zip.impl.create.options;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import saker.build.file.path.SakerPath;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.zip.api.create.IncludeResourceMapping;

public final class ChainIncludeResourceMapping implements IncludeResourceMapping, Externalizable {
	private static final long serialVersionUID = 1L;

	private IncludeResourceMapping first;
	private IncludeResourceMapping second;

	/**
	 * For {@link Externalizable}.
	 */
	public ChainIncludeResourceMapping() {
	}

	public ChainIncludeResourceMapping(IncludeResourceMapping first, IncludeResourceMapping second) {
		this.first = first;
		this.second = second;
	}

	@Override
	public Set<SakerPath> mapResourcePath(SakerPath archivepath, boolean directory) {
		Set<SakerPath> firstres = first.mapResourcePath(archivepath, directory);
		int size;
		if (firstres == null || (size = firstres.size()) == 0) {
			return Collections.emptySet();
		}
		if (size == 1) {
			return second.mapResourcePath(firstres.iterator().next(), directory);
		}
		Set<SakerPath> result = new LinkedHashSet<>();
		for (SakerPath path : firstres) {
			Set<SakerPath> secondres = second.mapResourcePath(path, directory);
			if (!ObjectUtils.isNullOrEmpty(secondres)) {
				result.addAll(secondres);
			}
		}
		return result;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(first);
		out.writeObject(second);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		first = (IncludeResourceMapping) in.readObject();
		second = (IncludeResourceMapping) in.readObject();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((second == null) ? 0 : second.hashCode());
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
		ChainIncludeResourceMapping other = (ChainIncludeResourceMapping) obj;
		if (first == null) {
			if (other.first != null)
				return false;
		} else if (!first.equals(other.first))
			return false;
		if (second == null) {
			if (other.second != null)
				return false;
		} else if (!second.equals(other.second))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + (first != null ? "first=" + first + ", " : "")
				+ (second != null ? "second=" + second : "") + "]";
	}

}
