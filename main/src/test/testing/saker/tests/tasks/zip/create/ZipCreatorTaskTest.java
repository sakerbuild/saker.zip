package testing.saker.tests.tasks.zip.create;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import saker.build.file.path.SakerPath;
import testing.saker.SakerTest;
import testing.saker.build.tests.TestUtils;
import testing.saker.nest.util.RepositoryLoadingVariablesMetricEnvironmentTestCase;

@SakerTest
public class ZipCreatorTaskTest extends RepositoryLoadingVariablesMetricEnvironmentTestCase {
	@Override
	protected void runTestImpl() throws Throwable {
		files.putFile(PATH_WORKING_DIRECTORY.resolve("include.zip"), ZipCreatorUtils
				.getZipBytes(TestUtils.<String, String>treeMapBuilder().put("inc1.txt", "incval").build()));

		Map<String, String> contents = new TreeMap<>();
		contents.put("a.txt", "alpha");
		contents.put("b.txt", "beta");
		contents.put("inc1.txt", "incval");
		contents.put("inctarget/inc1.txt", "incval");

		CombinedTargetTaskResult res;

		res = runScriptTask("build");
		assertSameContents(res, contents);

		res = runScriptTask("build");
		assertEmpty(getMetric().getRunTaskIdFactories());
		assertSameContents(res, contents);

		SakerPath ctxtpath = PATH_WORKING_DIRECTORY.resolve("c.txt");
		files.putFile(ctxtpath, "gamma");
		contents.put("c.txt", "gamma");
		res = runScriptTask("build");
		assertSameContents(res, contents);

		files.putFile(ctxtpath, "gammamod");
		contents.put("c.txt", "gammamod");
		res = runScriptTask("build");
		assertSameContents(res, contents);

		files.delete(ctxtpath);
		contents.remove("c.txt");
		res = runScriptTask("build");
		assertSameContents(res, contents);

		files.putFile(PATH_WORKING_DIRECTORY.resolve("include.zip"), ZipCreatorUtils.getZipBytes(TestUtils
				.<String, String>treeMapBuilder().put("inc1.txt", "incval").put("incadd.txt", "incadd").build()));
		contents.put("incadd.txt", "incadd");
		contents.put("inctarget/incadd.txt", "incadd");
		res = runScriptTask("build");
		assertSameContents(res, contents);
		
		files.delete((SakerPath) res.getTargetTaskResult("zippath"));
		res = runScriptTask("build");
		assertSameContents(res, contents);
	}

	private void assertSameContents(CombinedTargetTaskResult res, Map<String, String> expectedcontents)
			throws IOException, FileNotFoundException {
		ZipCreatorUtils.assertSameContents((SakerPath) res.getTargetTaskResult("zippath"), files, expectedcontents);
	}
}
