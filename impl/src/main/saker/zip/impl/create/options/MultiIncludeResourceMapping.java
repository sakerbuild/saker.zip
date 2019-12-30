package saker.zip.impl.create.options;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import saker.build.file.path.SakerPath;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.io.SerialUtils;
import saker.zip.api.create.IncludeResourceMapping;

public final class MultiIncludeResourceMapping implements IncludeResourceMapping, Externalizable {
	private static final long serialVersionUID = 1L;

	protected List<IncludeResourceMapping> mappings;

	/**
	 * For {@link Externalizable}.
	 */
	public MultiIncludeResourceMapping() {
	}

	public static IncludeResourceMapping create(IncludeResourceMapping... mappings) {
		if (ObjectUtils.isNullOrEmpty(mappings)) {
			return ExcludeIncludeResourceMapping.INSTANCE;
		}
		MultiIncludeResourceMapping result = new MultiIncludeResourceMapping();
		result.mappings = ImmutableUtils.makeImmutableList(mappings);
		return result;
	}

	public static IncludeResourceMapping create(Iterable<? extends IncludeResourceMapping> mappings) {
		List<IncludeResourceMapping> mappingslist = ImmutableUtils.makeImmutableList(mappings);
		if (ObjectUtils.isNullOrEmpty(mappingslist)) {
			return ExcludeIncludeResourceMapping.INSTANCE;
		}
		MultiIncludeResourceMapping result = new MultiIncludeResourceMapping();
		result.mappings = mappingslist;
		return result;
	}

	public List<IncludeResourceMapping> getMappings() {
		return mappings;
	}

	@Override
	public Set<SakerPath> mapResourcePath(SakerPath archivepath, boolean directory) {
		LinkedHashSet<SakerPath> result = new LinkedHashSet<>();
		for (IncludeResourceMapping mapping : mappings) {
			Set<SakerPath> mapres = mapping.mapResourcePath(archivepath, directory);
			if (mapres != null) {
				result.addAll(mapres);
			}
		}
		return result;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		SerialUtils.writeExternalCollection(out, mappings);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		mappings = SerialUtils.readExternalImmutableList(in);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mappings == null) ? 0 : mappings.hashCode());
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
		MultiIncludeResourceMapping other = (MultiIncludeResourceMapping) obj;
		if (mappings == null) {
			if (other.mappings != null)
				return false;
		} else if (!mappings.equals(other.mappings))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "MultiIncludeResourceMapping[" + (mappings != null ? "mappings=" + mappings : "") + "]";
	}

}