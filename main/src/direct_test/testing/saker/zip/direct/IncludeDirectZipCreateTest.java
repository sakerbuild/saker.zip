package testing.saker.zip.direct;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;

import saker.build.file.SakerFile;
import saker.build.file.path.SakerPath;
import saker.build.file.path.WildcardPath;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.io.ByteArrayRegion;
import saker.zip.api.create.IncludeResourceMapping;
import saker.zip.api.create.ZipResourceEntry;
import saker.zip.impl.create.ZipOutputSakerFile;
import saker.zip.impl.create.options.IdentityIncludeResourceMapping;
import saker.zip.impl.create.options.StoredCompressionIncludeResourceMapping;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;
import testing.saker.build.tests.TestUtils;
import testing.saker.zip.direct.transformers.IdentityZipResourceTransformerFactory;
import testing.saker.zip.test.utils.ZipCreatorUtils;

@SakerTest
public class IncludeDirectZipCreateTest extends SakerTestCase {

	private static final int ADD_TRANSFORMER = 1 << 0;
	private static final int STORED_INCLUDE = 1 << 1;
	private static final int DEFAULTED_IDENTITY_INCLUDE = 1 << 2;

	@Override
	public void runTest(Map<String, String> parameters) throws Throwable {
		TreeMap<String, String> includedzipcontents = TestUtils.<String, String>treeMapBuilder()
				.put("readme.txt", "readme_2").put("dir2/sub.txt", "sub").put("mydir2/", null).build();
		{
			//test with compressed included zip contents
			SakerFile includedzip = ZipCreatorUtils.getZipFile(includedzipcontents);

			ByteArrayRegion bytes = genZipBytes(includedzip, 0);

			ZipCreatorUtils.assertCompression(bytes, ZipEntry.DEFLATED);

			bytes = genZipBytes(includedzip, ADD_TRANSFORMER);

			ZipCreatorUtils.assertCompression(bytes, ZipEntry.DEFLATED);
		}
		{
			//test with compressed included zip contents
			SakerFile includedzip = ZipCreatorUtils.getStoredZipFile(includedzipcontents);

			ByteArrayRegion bytes = genZipBytes(includedzip, 0);

			//the included ZIP has stored entries, they should be copied as stored entries as well
			ZipCreatorUtils.assertCompression(bytes, ZipEntry.STORED, WildcardPath.valueOf("included/**/*"));
			ZipCreatorUtils.assertCompression(bytes, ZipEntry.DEFLATED, WildcardPath.valueOf("readme.txt"));

			bytes = genZipBytes(includedzip, ADD_TRANSFORMER);

			//the included ZIP has stored entries, they should be copied as stored entries as well
			ZipCreatorUtils.assertCompression(bytes, ZipEntry.STORED, WildcardPath.valueOf("included/**/*"));
			ZipCreatorUtils.assertCompression(bytes, ZipEntry.DEFLATED, WildcardPath.valueOf("readme.txt"));
		}
		{
			//test with compressed included zip contents, but storing them uncompressed using a chained include mapping
			SakerFile includedzip = ZipCreatorUtils.getZipFile(includedzipcontents);

			ByteArrayRegion bytes = genZipBytes(includedzip, STORED_INCLUDE);

			//the included ZIP has stored entries, they should be copied as stored entries as well
			ZipCreatorUtils.assertCompression(bytes, ZipEntry.STORED, WildcardPath.valueOf("included/**/*"));
			ZipCreatorUtils.assertCompression(bytes, ZipEntry.DEFLATED, WildcardPath.valueOf("readme.txt"));

			bytes = genZipBytes(includedzip, ADD_TRANSFORMER | STORED_INCLUDE);

			//the included ZIP has stored entries, they should be copied as stored entries as well
			ZipCreatorUtils.assertCompression(bytes, ZipEntry.STORED, WildcardPath.valueOf("included/**/*"));
			ZipCreatorUtils.assertCompression(bytes, ZipEntry.DEFLATED, WildcardPath.valueOf("readme.txt"));
		}
		{
			//test the default IncludeResourceMapping.mapResource implementation
			SakerFile includedzip = ZipCreatorUtils.getZipFile(includedzipcontents);
			ByteArrayRegion bytes = genZipBytes(includedzip, DEFAULTED_IDENTITY_INCLUDE);

			//everything is deflated
			ZipCreatorUtils.assertCompression(bytes, ZipEntry.DEFLATED);

			includedzip = ZipCreatorUtils.getStoredZipFile(includedzipcontents);
			bytes = genZipBytes(includedzip, DEFAULTED_IDENTITY_INCLUDE);

			//everything is stored readme.txt
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
	private static ByteArrayRegion genZipBytes(SakerFile includedzip, int flags) throws IOException {
		ZipOutputSakerFile.Builder builder = ZipOutputSakerFile.builder();
		builder.add(SakerPath.valueOf("readme.txt"), ZipCreatorUtils.byteFileHandle("readme_base"));
		IncludeResourceMapping incmapping = IncludeResourceMapping.targetDirectory(SakerPath.valueOf("included"));
		if ((flags & STORED_INCLUDE) != 0) {
			incmapping = IncludeResourceMapping.chain(incmapping, StoredCompressionIncludeResourceMapping.INSTANCE);
		}
		if ((flags & DEFAULTED_IDENTITY_INCLUDE) != 0) {

			incmapping = IncludeResourceMapping.chain(incmapping, new DefaultedIdentityIncludeResourceMapping());
		}
		builder.addIncludeFromArchive(includedzip, includedzip.getContentDescriptor(), incmapping);
		if ((flags & ADD_TRANSFORMER) != 0) {
			builder.addResourceTransformer(new IdentityZipResourceTransformerFactory());
		}

		ByteArrayRegion bytes = builder.build("test.zip").getBytes();

		//common assertion for the test cases
		ZipCreatorUtils.assertSameContents(TestUtils.<String, String>treeMapBuilder().put("readme.txt", "readme_base")
				.put("included/readme.txt", "readme_2").put("included/dir2/sub.txt", "sub")
				.put("included/mydir2/", null).build(), bytes);

		return bytes;
	}

	private static final class DefaultedIdentityIncludeResourceMapping
			implements IncludeResourceMapping, Externalizable {
		private static final long serialVersionUID = 1L;

		/**
		 * For {@link Externalizable}.
		 */
		public DefaultedIdentityIncludeResourceMapping() {
		}

		@Override
		@SuppressWarnings("deprecation")
		public Set<SakerPath> mapResourcePath(SakerPath archivepath, boolean directory) {
			return ImmutableUtils.singletonSet(archivepath);
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		}

		@Override
		public int hashCode() {
			return getClass().getName().hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return ObjectUtils.isSameClass(this, obj);
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "[]";
		}

	}

}
