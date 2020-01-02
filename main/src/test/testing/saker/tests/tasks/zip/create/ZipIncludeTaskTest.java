/*
 * Copyright (C) 2020 Bence Sipka
 *
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
public class ZipIncludeTaskTest extends RepositoryLoadingVariablesMetricEnvironmentTestCase {
	@Override
	protected void runTestImpl() throws Throwable {
		files.putFile(PATH_WORKING_DIRECTORY.resolve("include.zip"), ZipCreatorUtils
				.getZipBytes(TestUtils.<String, String>treeMapBuilder().put("inc1.txt", "incval").build()));
		files.putFile(PATH_WORKING_DIRECTORY.resolve("resinclude.zip"),
				ZipCreatorUtils.getZipBytes(TestUtils.<String, String>treeMapBuilder().put("resinc.txt", "rinc")
						.put("notinc", "no-no").put("dir/inc.txt", "dirinc").put("dir/notinc", "no-no").build()));
		files.putFile(PATH_WORKING_DIRECTORY.resolve("dirinclude.zip"), ZipCreatorUtils
				.getZipBytes(TestUtils.<String, String>treeMapBuilder().put("dir/", null).build()));

		Map<String, String> contents = new TreeMap<>();
		contents.put("inc1.txt", "incval");
		contents.put("inctarget/inc1.txt", "incval");
		contents.put("resinc.txt", "rinc");
		contents.put("dir/inc.txt", "dirinc");
		contents.put("dir/", null);
		contents.put("dirtarget/dir/", null);

		CombinedTargetTaskResult res;

		res = runScriptTask("build");
		assertSameContents(res, contents);

		res = runScriptTask("build");
		assertEmpty(getMetric().getRunTaskIdFactories());
		assertSameContents(res, contents);

		files.putFile(PATH_WORKING_DIRECTORY.resolve("include.zip"), ZipCreatorUtils.getZipBytes(TestUtils
				.<String, String>treeMapBuilder().put("inc1.txt", "incval").put("incadd.txt", "incadd").build()));
		contents.put("incadd.txt", "incadd");
		contents.put("inctarget/incadd.txt", "incadd");
		res = runScriptTask("build");
		assertSameContents(res, contents);
	}

	private void assertSameContents(CombinedTargetTaskResult res, Map<String, String> expectedcontents)
			throws IOException, FileNotFoundException {
		ZipCreatorUtils.assertSameContents((SakerPath) res.getTargetTaskResult("zippath"), files, expectedcontents);
	}
}
