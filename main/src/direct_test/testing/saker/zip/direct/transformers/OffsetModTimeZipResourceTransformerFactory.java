package testing.saker.zip.direct.transformers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.attribute.FileTime;

import saker.build.file.path.SakerPath;
import saker.zip.api.create.ZipResourceEntry;
import saker.zip.api.create.ZipResourceTransformationContext;
import saker.zip.api.create.ZipResourceTransformer;
import saker.zip.api.create.ZipResourceTransformerFactory;

public final class OffsetModTimeZipResourceTransformerFactory implements ZipResourceTransformerFactory {
	private final long offset;

	public OffsetModTimeZipResourceTransformerFactory(long offset) {
		this.offset = offset;
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
				System.out.println("offsetting: " + resourceentry);
				long ntime = offset;
				FileTime resmodtime = resourceentry.getModificationTime();
				if (resmodtime != null) {
					ntime += resmodtime.toMillis();
				}
				return resourceentry.withModificationTime(FileTime.fromMillis(ntime));
			}
		};
	}

}