package testing.saker.zip.direct;

import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.zip.ZipEntry;

import saker.build.file.content.ContentDescriptor;
import saker.build.file.path.SakerPath;
import saker.zip.api.create.ZipResourceEntry;
import saker.zip.impl.create.ZipOutputSakerFile;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;
import testing.saker.zip.test.utils.ZipCreatorUtils;

@SakerTest
public class ContentDescriptorDirectZipCreateTest extends SakerTestCase {

	@Override
	@SuppressWarnings("deprecation")
	public void runTest(Map<String, String> parameters) throws Throwable {
		ContentDescriptor contentdefault;
		ContentDescriptor contententrydefault;
		ContentDescriptor contentmodtime;
		ContentDescriptor contentstoredmethod;
		{
			ZipOutputSakerFile.Builder builder = ZipOutputSakerFile.builder();
			builder.add(SakerPath.valueOf("readme.txt"), ZipCreatorUtils.byteFileHandle("readme"));
			contentdefault = builder.build("test.zip").getContentDescriptor();
		}
		{
			ZipOutputSakerFile.Builder builder = ZipOutputSakerFile.builder();
			builder.add(new ZipResourceEntry(SakerPath.valueOf("readme.txt")),
					ZipCreatorUtils.byteFileHandle("readme"));
			contententrydefault = builder.build("test.zip").getContentDescriptor();
		}
		{
			ZipOutputSakerFile.Builder builder = ZipOutputSakerFile.builder();
			builder.add(new ZipResourceEntry(SakerPath.valueOf("readme.txt"),
					FileTime.fromMillis(System.currentTimeMillis())), ZipCreatorUtils.byteFileHandle("readme"));
			contentmodtime = builder.build("test.zip").getContentDescriptor();
		}
		{
			ZipOutputSakerFile.Builder builder = ZipOutputSakerFile.builder();
			builder.add(new ZipResourceEntry(SakerPath.valueOf("readme.txt"), null, ZipEntry.STORED, -1),
					ZipCreatorUtils.byteFileHandle("readme"));
			contentstoredmethod = builder.build("test.zip").getContentDescriptor();
		}

		assertEquals(contentdefault, contententrydefault);

		//different modification times or store methods, so file content descriptor should not equal
		assertNotEquals(contentdefault, contentmodtime);
		assertNotEquals(contententrydefault, contentmodtime);

		assertNotEquals(contentdefault, contentstoredmethod);
		assertNotEquals(contententrydefault, contentstoredmethod);

		assertNotEquals(contentmodtime, contentstoredmethod);
	}

}
