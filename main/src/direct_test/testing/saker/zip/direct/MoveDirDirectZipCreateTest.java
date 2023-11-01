package testing.saker.zip.direct;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import saker.build.file.SakerFile;
import saker.build.file.content.DirectoryContentDescriptor;
import saker.build.file.path.SakerPath;
import saker.build.thirdparty.saker.util.io.ByteArrayRegion;
import saker.zip.impl.create.ZipOutputSakerFile;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;
import testing.saker.build.tests.TestUtils;
import testing.saker.zip.direct.transformers.MoveToDirectoryZipResourceTransformerFactory;
import testing.saker.zip.test.utils.ZipCreatorUtils;

@SakerTest
public class MoveDirDirectZipCreateTest extends SakerTestCase {

	@Override
	public void runTest(Map<String, String> parameters) throws Throwable {
		//entries are moved under aaa/
		runTestWithTransformers(new MoveToDirectoryZipResourceTransformerFactory(SakerPath.valueOf("aaa")));

		//entries are moved under aaa/bbb/
		runTestWithTransformers(new MoveToDirectoryZipResourceTransformerFactory(SakerPath.valueOf("bbb")),
				new MoveToDirectoryZipResourceTransformerFactory(SakerPath.valueOf("aaa")));
	}

	@SuppressWarnings("deprecation")
	private static void runTestWithTransformers(MoveToDirectoryZipResourceTransformerFactory... transformers)
			throws IOException, AssertionError {
		SakerPath base = SakerPath.EMPTY;
		ZipOutputSakerFile.Builder builder = ZipOutputSakerFile.builder();
		builder.add(SakerPath.valueOf("readme.txt"), ZipCreatorUtils.byteFileHandle("readme"));
		builder.add(SakerPath.valueOf("dir/sub.txt"), ZipCreatorUtils.byteFileHandle("sub"));
		builder.add(SakerPath.valueOf("mydir"), null, DirectoryContentDescriptor.INSTANCE);
		for (MoveToDirectoryZipResourceTransformerFactory transformer : transformers) {
			base = transformer.getDirectory().resolve(base);
			builder.addResourceTransformer(transformer);
		}
		SakerFile file = builder.build("test.zip");

		ByteArrayRegion bytes = file.getBytes();

		TreeMap<String, String> contents = TestUtils.<String, String>treeMapBuilder()
				.put(base.resolve("readme.txt").toString(), "readme").put(base.resolve("dir/sub.txt").toString(), "sub")
				.put(base.resolve("mydir") + "/", null).build();
		ZipCreatorUtils.assertSameContents(contents, bytes);
	}
}
