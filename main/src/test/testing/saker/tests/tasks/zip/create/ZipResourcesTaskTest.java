package testing.saker.tests.tasks.zip.create;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import saker.build.file.path.SakerPath;
import testing.saker.SakerTest;
import testing.saker.nest.util.RepositoryLoadingVariablesMetricEnvironmentTestCase;

@SakerTest
public class ZipResourcesTaskTest extends RepositoryLoadingVariablesMetricEnvironmentTestCase {
	@Override
	protected void runTestImpl() throws Throwable {
		Map<String, String> contents = new TreeMap<>();
		contents.put("a.txt", "alpha");
		contents.put("b.txt", "beta");
		contents.put("c.txt", "cval");
		contents.put("subdir/d.txt", "dval");
		contents.put("targetdir/a.txt", "alpha");
		contents.put("targetdir/b.txt", "beta");
		contents.put("directtargetdir/target.direct", "td");
		contents.put("directtargetdir/dir.direct", "dd");

		contents.put("file.direct", "fd");
		contents.put("dir.direct", "dd");

		CombinedTargetTaskResult res;

		res = runScriptTask("build");
		assertSameContents(res, contents);

		res = runScriptTask("build");
		assertEmpty(getMetric().getRunTaskIdFactories());
		assertSameContents(res, contents);

	}

	private void assertSameContents(CombinedTargetTaskResult res, Map<String, String> expectedcontents)
			throws IOException, FileNotFoundException {
		ZipCreatorUtils.assertSameContents((SakerPath) res.getTargetTaskResult("zippath"), files, expectedcontents);
	}
}
