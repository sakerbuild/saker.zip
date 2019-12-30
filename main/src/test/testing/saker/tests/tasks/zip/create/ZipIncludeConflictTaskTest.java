package testing.saker.tests.tasks.zip.create;

import testing.saker.SakerTest;
import testing.saker.build.tests.TestUtils;
import testing.saker.nest.util.RepositoryLoadingVariablesMetricEnvironmentTestCase;

@SakerTest
public class ZipIncludeConflictTaskTest extends RepositoryLoadingVariablesMetricEnvironmentTestCase {
	@Override
	protected void runTestImpl() throws Throwable {
		//there should be a case-insensitive conflict
		files.putFile(PATH_WORKING_DIRECTORY.resolve("first.zip"), ZipCreatorUtils
				.getZipBytes(TestUtils.<String, String>treeMapBuilder().put("inc.txt", "incval1").build()));
		files.putFile(PATH_WORKING_DIRECTORY.resolve("second.zip"), ZipCreatorUtils
				.getZipBytes(TestUtils.<String, String>treeMapBuilder().put("INC.TXT", "incval2").build()));

		assertTaskException(IllegalArgumentException.class, () -> runScriptTask("build"));
	}

}
