package testing.saker.zip.direct.transformers;

import java.io.IOException;
import java.io.InputStream;

import saker.build.file.path.SakerPath;
import saker.zip.api.create.ZipResourceEntry;
import saker.zip.api.create.ZipResourceTransformationContext;
import saker.zip.api.create.ZipResourceTransformer;
import saker.zip.api.create.ZipResourceTransformerFactory;

public final class MoveToDirectoryZipResourceTransformerFactory implements ZipResourceTransformerFactory {
	private final SakerPath directory;

	public MoveToDirectoryZipResourceTransformerFactory(SakerPath directory) {
		this.directory = directory;
	}

	@Override
	public ZipResourceTransformer createTransformer() {
		return new ZipResourceTransformer() {
			@Override
			@SuppressWarnings("deprecation")
			public boolean process(ZipResourceTransformationContext context, SakerPath resourcepath,
					InputStream resourceinput) throws IOException {
				throw new UnsupportedOperationException();
			}

			@Override
			public ZipResourceEntry process(ZipResourceTransformationContext context, ZipResourceEntry resourceentry,
					InputStream resourceinput) throws IOException {
				return resourceentry.withEntryPath(directory.resolve(resourceentry.getEntryPath()));
			}
		};
	}

	public SakerPath getDirectory() {
		return directory;
	}
}