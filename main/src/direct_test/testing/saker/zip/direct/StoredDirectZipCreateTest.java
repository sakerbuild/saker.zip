package testing.saker.zip.direct;

import java.util.Map;
import java.util.zip.ZipEntry;

import saker.build.file.SakerFile;
import saker.build.file.content.DirectoryContentDescriptor;
import saker.build.file.path.SakerPath;
import saker.build.thirdparty.saker.util.io.ByteArrayRegion;
import saker.zip.impl.create.ZipOutputSakerFile;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;
import testing.saker.build.tests.TestUtils;
import testing.saker.zip.direct.transformers.NoCompressZipResourceTransformerFactory;
import testing.saker.zip.test.utils.ZipCreatorUtils;

@SakerTest
public class StoredDirectZipCreateTest extends SakerTestCase {

	@Override
	@SuppressWarnings("deprecation")
	public void runTest(Map<String, String> parameters) throws Throwable {
		ZipOutputSakerFile.Builder builder = ZipOutputSakerFile.builder();
		builder.add(SakerPath.valueOf("readme.txt"), ZipCreatorUtils.byteFileHandle("readme"));
		builder.add(SakerPath.valueOf("dir/sub.txt"), ZipCreatorUtils.byteFileHandle("sub"));
		builder.add(SakerPath.valueOf("mydir"), null, DirectoryContentDescriptor.INSTANCE);
		builder.addResourceTransformer(new NoCompressZipResourceTransformerFactory());
		SakerFile file = builder.build("test.zip");

		ByteArrayRegion bytes = file.getBytes();

		ZipCreatorUtils.assertSameContents(TestUtils.<String, String>treeMapBuilder().put("readme.txt", "readme")
				.put("dir/sub.txt", "sub").put("mydir/", null).build(), bytes);

		//all stored
		ZipCreatorUtils.assertCompression(bytes, ZipEntry.STORED);
	}

}
