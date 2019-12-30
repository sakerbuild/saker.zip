package testing.saker.tests.tasks.zip.create;

import saker.build.file.path.SakerPath;
import testing.saker.SakerTest;
import testing.saker.nest.util.RepositoryLoadingVariablesMetricEnvironmentTestCase;

@SakerTest
public class ZipIncrementalityBugTest extends RepositoryLoadingVariablesMetricEnvironmentTestCase {

	@Override
	protected void runTestImpl() throws Throwable {
		runScriptTask("export", SakerPath.valueOf("subdir/saker.build"));
		
		runScriptTask("export", SakerPath.valueOf("subdir/saker.build"));
		assertEmpty(getMetric().getRunTaskIdFactories());
	}

}
