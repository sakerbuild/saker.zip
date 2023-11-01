package testing.saker.zip.direct;

import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.zip.ZipEntry;

import saker.build.file.SakerFile;
import saker.build.file.content.DirectoryContentDescriptor;
import saker.build.file.path.SakerPath;
import saker.build.file.path.WildcardPath;
import saker.build.thirdparty.saker.util.io.ByteArrayRegion;
import saker.zip.api.create.ZipResourceEntry;
import saker.zip.impl.create.ZipOutputSakerFile;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;
import testing.saker.build.tests.TestUtils;
import testing.saker.zip.test.utils.ZipCreatorUtils;

@SakerTest
public class SimpleDirectZipCreateTest extends SakerTestCase {

	@Override
	@SuppressWarnings("deprecation")
	public void runTest(Map<String, String> parameters) throws Throwable {
		{
			ZipOutputSakerFile.Builder builder = ZipOutputSakerFile.builder();
			builder.add(SakerPath.valueOf("readme.txt"), ZipCreatorUtils.byteFileHandle("readme"));
			builder.add(SakerPath.valueOf("dir/sub.txt"), ZipCreatorUtils.byteFileHandle("sub"));
			builder.add(SakerPath.valueOf("mydir"), null, DirectoryContentDescriptor.INSTANCE);
			SakerFile file = builder.build("test.zip");

			ByteArrayRegion bytes = file.getBytes();

			ZipCreatorUtils.assertSameContents(TestUtils.<String, String>treeMapBuilder().put("readme.txt", "readme")
					.put("dir/sub.txt", "sub").put("mydir/", null).build(), bytes);

			//all compressed
			ZipCreatorUtils.assertCompression(bytes, ZipEntry.DEFLATED);

			FileTime epoch = FileTime.fromMillis(0);
			Map<String, FileTime> modtimes = ZipCreatorUtils.getLastModificationTimes(bytes);
			assertTrue(modtimes.values().stream().allMatch(i -> i.equals(epoch)), modtimes::toString);
		}
		{
			//round to sec
			FileTime modtime = FileTime.fromMillis(System.currentTimeMillis() / 1000 * 1000);
			ZipOutputSakerFile.Builder builder = ZipOutputSakerFile.builder();
			builder.add(ZipResourceEntry.create(SakerPath.valueOf("readme.txt"), modtime),
					ZipCreatorUtils.byteFileHandle("readme"));
			builder.add(ZipResourceEntry.stored(SakerPath.valueOf("dir/sub.txt"), modtime),
					ZipCreatorUtils.byteFileHandle("sub"));
			builder.add(ZipResourceEntry.create(SakerPath.valueOf("mydir"), modtime), null,
					DirectoryContentDescriptor.INSTANCE);
			SakerFile file = builder.build("test.zip");

			ByteArrayRegion bytes = file.getBytes();

			ZipCreatorUtils.assertSameContents(TestUtils.<String, String>treeMapBuilder().put("readme.txt", "readme")
					.put("dir/sub.txt", "sub").put("mydir/", null).build(), bytes);

			ZipCreatorUtils.assertCompression(bytes, ZipEntry.DEFLATED, WildcardPath.valueOf("readme.txt"));
			ZipCreatorUtils.assertCompression(bytes, ZipEntry.STORED, WildcardPath.valueOf("dir/sub.txt"));
			ZipCreatorUtils.assertCompression(bytes, ZipEntry.DEFLATED, WildcardPath.valueOf("mydir"));

			Map<String, FileTime> modtimes = ZipCreatorUtils.getLastModificationTimes(bytes);
			assertTrue(modtimes.values().stream().allMatch(i -> i.equals(modtime)), modtimes::toString);
		}
	}

}
