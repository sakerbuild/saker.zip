package saker.zip.impl.create;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import saker.build.file.path.SakerPath;
import saker.zip.api.create.ZipCreatorTaskOutput;

public class SimpleZipCreatorTaskOutput implements ZipCreatorTaskOutput, Externalizable {
	private static final long serialVersionUID = 1L;
	private SakerPath path;

	/**
	 * For {@link Externalizable}.
	 */
	public SimpleZipCreatorTaskOutput() {
	}

	public SimpleZipCreatorTaskOutput(SakerPath path) {
		this.path = path;
	}

	@Override
	public SakerPath getPath() {
		return path;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(path);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		path = (SakerPath) in.readObject();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[path=" + path + "]";
	}

}
