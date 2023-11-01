package testing.saker.zip.direct;

import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.zip.ZipEntry;

import saker.build.file.SakerFile;
import saker.build.file.path.SakerPath;
import saker.build.file.path.WildcardPath;
import saker.build.thirdparty.saker.util.io.ByteArrayRegion;
import saker.zip.impl.create.ZipOutputSakerFile;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;
import testing.saker.build.tests.TestUtils;
import testing.saker.zip.direct.transformers.PropertyAdderZipResourceTransformerFactory;
import testing.saker.zip.test.utils.ZipCreatorUtils;

@SakerTest
public class ContentModifierDirectZipCreateTest extends SakerTestCase {

	@Override
	@SuppressWarnings("deprecation")
	public void runTest(Map<String, String> parameters) throws Throwable {
		ZipOutputSakerFile.Builder builder = ZipOutputSakerFile.builder();
		builder.add(SakerPath.valueOf("readme.txt"), ZipCreatorUtils.byteFileHandle("readme"));
		builder.add(SakerPath.valueOf("empty.properties"), ZipCreatorUtils.byteFileHandle(""));

		builder.addResourceTransformer(new PropertyAdderZipResourceTransformerFactory(
				WildcardPath.valueOf("**/*.properties"), "prop1", "propval"));
		builder.addResourceTransformer(new PropertyAdderZipResourceTransformerFactory(
				WildcardPath.valueOf("**/*.properties"), "prop2", "secondval"));

		SakerFile file = builder.build("test.zip");

		ByteArrayRegion bytes = file.getBytes();

		ZipCreatorUtils.assertSameContents(TestUtils.<String, String>treeMapBuilder().put("readme.txt", "readme")
				.put("empty.properties", "prop1=propval\nprop2=secondval\n").build(), bytes);

		//all compressed
		ZipCreatorUtils.assertCompression(bytes, ZipEntry.DEFLATED);

		FileTime epoch = FileTime.fromMillis(0);
		Map<String, FileTime> modtimes = ZipCreatorUtils.getLastModificationTimes(bytes);
		assertTrue(modtimes.values().stream().allMatch(i -> i.equals(epoch)), modtimes::toString);
	}

}
