package testing.saker.tests.tasks.zip.create;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import saker.build.file.path.SakerPath;
import testing.saker.SakerTest;
import testing.saker.nest.util.RepositoryLoadingVariablesMetricEnvironmentTestCase;

@SakerTest
public class ZipModificationTimeTaskTest extends RepositoryLoadingVariablesMetricEnvironmentTestCase {
	public static final SimpleDateFormat PARSER_NOMILLIS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public static final Date ENTRYDATE;
	static {
		try {
			ENTRYDATE = PARSER_NOMILLIS.parse("1999-10-22 12:13:14");
		} catch (ParseException e) {
			throw new AssertionError();
		}
	}

	@Override
	protected void runTestImpl() throws Throwable {
		Map<String, String> contents = new TreeMap<>();
		contents.put("a.txt", "alpha");
		contents.put("b.txt", "beta");

		CombinedTargetTaskResult res;

		res = runScriptTask("build");
		assertSameContents(res, contents);

		res = runScriptTask("build");
		assertEquals(getMetric().getRunTaskIdFactories().keySet(), strTaskIdSetOf());
		assertSameContents(res, contents);

		SakerPath ctxtpath = PATH_WORKING_DIRECTORY.resolve("c.txt");
		files.putFile(ctxtpath, "gamma");
		contents.put("c.txt", "gamma");
		res = runScriptTask("build");
		assertSameContents(res, contents);

		files.delete(ctxtpath);
		contents.remove("c.txt");
		res = runScriptTask("build");
		assertSameContents(res, contents);
	}

	private void assertSameContents(CombinedTargetTaskResult res, Map<String, String> expectedcontents)
			throws IOException, FileNotFoundException {
		ZipCreatorUtils.assertSameContents((SakerPath) res.getTargetTaskResult("zippath"), files, expectedcontents);
	}

}
