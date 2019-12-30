package saker.zip.impl.create.options;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Set;

import saker.build.file.path.SakerPath;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.zip.api.create.IncludeResourceMapping;

public final class ExcludeIncludeResourceMapping implements IncludeResourceMapping, Externalizable {
	private static final long serialVersionUID = 1L;

	public static final ExcludeIncludeResourceMapping INSTANCE = new ExcludeIncludeResourceMapping();

	/**
	 * For {@link Externalizable}.
	 */
	public ExcludeIncludeResourceMapping() {
	}

	@Override
	public Set<SakerPath> mapResourcePath(SakerPath archivepath, boolean directory) {
		return Collections.emptySet();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
	}

	@Override
	public int hashCode() {
		return getClass().getName().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return ObjectUtils.isSameClass(this, obj);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[]";
	}

}
