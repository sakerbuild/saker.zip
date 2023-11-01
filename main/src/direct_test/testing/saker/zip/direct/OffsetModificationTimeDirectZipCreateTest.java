package testing.saker.zip.direct;

import java.nio.file.attribute.FileTime;
import java.util.Map;

import saker.build.file.SakerFile;
import saker.build.file.content.DirectoryContentDescriptor;
import saker.build.file.path.SakerPath;
import saker.build.thirdparty.saker.util.io.ByteArrayRegion;
import saker.zip.impl.create.ZipOutputSakerFile;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;
import testing.saker.build.tests.TestUtils;
import testing.saker.zip.direct.transformers.OffsetModTimeZipResourceTransformerFactory;
import testing.saker.zip.test.utils.ZipCreatorUtils;

@SakerTest
public class OffsetModificationTimeDirectZipCreateTest extends SakerTestCase {

	@Override
	@SuppressWarnings("deprecation")
	public void runTest(Map<String, String> parameters) throws Throwable {
		//millis might not get tracked? idk, but round it just in case, as rounding is irrelevant for this test
		long basemillis = (System.currentTimeMillis()) / 1000 * 1000;
		FileTime modtime = FileTime.fromMillis(basemillis);
		System.out.println("Mod time: " + modtime);
		int offset = 60 * 1000;
		FileTime newtime = FileTime.fromMillis(basemillis + offset);
		System.out.println("New time: " + newtime);

		ZipOutputSakerFile.Builder builder = ZipOutputSakerFile.builder();
		builder.add(SakerPath.valueOf("readme.txt"), ZipCreatorUtils.byteFileHandle("readme"));
		builder.add(SakerPath.valueOf("dir/sub.txt"), ZipCreatorUtils.byteFileHandle("sub"));
		builder.add(SakerPath.valueOf("mydir"), null, DirectoryContentDescriptor.INSTANCE);
		builder.setDefaultEntryModificationTime(modtime);
		builder.addResourceTransformer(new OffsetModTimeZipResourceTransformerFactory(offset));
		SakerFile file = builder.build("test.zip");

		ByteArrayRegion bytes = file.getBytes();

		ZipCreatorUtils.assertSameContents(TestUtils.<String, String>treeMapBuilder().put("readme.txt", "readme")
				.put("dir/sub.txt", "sub").put("mydir/", null).build(), bytes);

		Map<String, FileTime> modtimes = ZipCreatorUtils.getLastModificationTimes(bytes);
		assertTrue(modtimes.values().stream().allMatch(i -> i.equals(newtime)), modtimes::toString);
	}

}
