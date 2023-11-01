package testing.saker.zip.direct;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;

import saker.build.file.SakerFile;
import saker.build.file.path.SakerPath;
import saker.build.file.path.WildcardPath;
import saker.build.thirdparty.saker.util.io.ByteArrayRegion;
import saker.zip.api.create.IncludeResourceMapping;
import saker.zip.impl.create.ZipOutputSakerFile;
import saker.zip.impl.create.options.StoredCompressionIncludeResourceMapping;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;
import testing.saker.build.tests.TestUtils;
import testing.saker.zip.direct.transformers.IdentityZipResourceTransformerFactory;
import testing.saker.zip.test.utils.ZipCreatorUtils;

@SakerTest
public class IncludeDirectZipCreateTest extends SakerTestCase {

	@Override
	public void runTest(Map<String, String> parameters) throws Throwable {
		TreeMap<String, String> includedzipcontents = TestUtils.<String, String>treeMapBuilder()
				.put("readme.txt", "readme_2").put("dir2/sub.txt", "sub").put("mydir2/", null).build();
		{
			//test with compressed included zip contents
			SakerFile includedzip = ZipCreatorUtils.getZipFile(includedzipcontents);

			ByteArrayRegion bytes = genZipBytes(includedzip, false, false);

			ZipCreatorUtils.assertCompression(bytes, ZipEntry.DEFLATED);

			bytes = genZipBytes(includedzip, true, false);

			ZipCreatorUtils.assertCompression(bytes, ZipEntry.DEFLATED);
		}
		{
			//test with compressed included zip contents
			SakerFile includedzip = ZipCreatorUtils.getStoredZipFile(includedzipcontents);

			ByteArrayRegion bytes = genZipBytes(includedzip, false, false);

			//the included ZIP has stored entries, they should be copied as stored entries as well
			ZipCreatorUtils.assertCompression(bytes, ZipEntry.STORED, WildcardPath.valueOf("included/**/*"));
			ZipCreatorUtils.assertCompression(bytes, ZipEntry.DEFLATED, WildcardPath.valueOf("readme.txt"));

			bytes = genZipBytes(includedzip, true, false);

			//the included ZIP has stored entries, they should be copied as stored entries as well
			ZipCreatorUtils.assertCompression(bytes, ZipEntry.STORED, WildcardPath.valueOf("included/**/*"));
			ZipCreatorUtils.assertCompression(bytes, ZipEntry.DEFLATED, WildcardPath.valueOf("readme.txt"));
		}
		{
			//test with compressed included zip contents, but storing them uncompressed using a chained include mapping
			SakerFile includedzip = ZipCreatorUtils.getZipFile(includedzipcontents);

			ByteArrayRegion bytes = genZipBytes(includedzip, false, true);

			//the included ZIP has stored entries, they should be copied as stored entries as well
			ZipCreatorUtils.assertCompression(bytes, ZipEntry.STORED, WildcardPath.valueOf("included/**/*"));
			ZipCreatorUtils.assertCompression(bytes, ZipEntry.DEFLATED, WildcardPath.valueOf("readme.txt"));

			bytes = genZipBytes(includedzip, true, true);

			//the included ZIP has stored entries, they should be copied as stored entries as well
			ZipCreatorUtils.assertCompression(bytes, ZipEntry.STORED, WildcardPath.valueOf("included/**/*"));
			ZipCreatorUtils.assertCompression(bytes, ZipEntry.DEFLATED, WildcardPath.valueOf("readme.txt"));
		}
	}

	/**
	 * @param addtransformer
	 *            To run with or without a transformer. Used to change the generating backend by adding a dummy identity
	 *            transformer.
	 */
	@SuppressWarnings("deprecation")
	private static ByteArrayRegion genZipBytes(SakerFile includedzip, boolean addtransformer, boolean storedinclude)
			throws IOException {
		ZipOutputSakerFile.Builder builder = ZipOutputSakerFile.builder();
		builder.add(SakerPath.valueOf("readme.txt"), ZipCreatorUtils.byteFileHandle("readme_base"));
		IncludeResourceMapping incmapping = IncludeResourceMapping.targetDirectory(SakerPath.valueOf("included"));
		if (storedinclude) {
			incmapping = IncludeResourceMapping.chain(incmapping, StoredCompressionIncludeResourceMapping.INSTANCE);
		}
		builder.addIncludeFromArchive(includedzip, includedzip.getContentDescriptor(), incmapping);
		if (addtransformer) {
			builder.addResourceTransformer(new IdentityZipResourceTransformerFactory());
		}

		ByteArrayRegion bytes = builder.build("test.zip").getBytes();

		//common assertion for the test cases
		ZipCreatorUtils.assertSameContents(TestUtils.<String, String>treeMapBuilder().put("readme.txt", "readme_base")
				.put("included/readme.txt", "readme_2").put("included/dir2/sub.txt", "sub")
				.put("included/mydir2/", null).build(), bytes);

		return bytes;
	}

}
