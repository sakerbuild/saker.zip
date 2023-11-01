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
package testing.saker.zip.tasks;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;

import saker.build.file.path.SakerPath;
import saker.build.file.path.WildcardPath;
import saker.build.thirdparty.saker.util.io.ByteArrayRegion;
import testing.saker.SakerTest;
import testing.saker.build.tests.TestUtils;
import testing.saker.nest.util.RepositoryLoadingVariablesMetricEnvironmentTestCase;
import testing.saker.zip.test.utils.ZipCreatorUtils;

@SakerTest
public class ZipIncludeCompressionTaskTest extends RepositoryLoadingVariablesMetricEnvironmentTestCase {
	@Override
	protected void runTestImpl() throws Throwable {
		files.putFile(PATH_WORKING_DIRECTORY.resolve("default.zip"),
				ZipCreatorUtils.getZipBytes(TestUtils.<String, String>treeMapBuilder().put("default", "def").build()));

		files.putFile(PATH_WORKING_DIRECTORY.resolve("stored.zip"),
				ZipCreatorUtils.getZipBytes(TestUtils.<String, String>treeMapBuilder().put("stored", "str").build()));

		files.putFile(PATH_WORKING_DIRECTORY.resolve("deflated.zip"), ZipCreatorUtils
				.getZipBytes(TestUtils.<String, String>treeMapBuilder().put("deflated", "defl").build()));

		files.putFile(PATH_WORKING_DIRECTORY.resolve("fromstored.zip"), ZipCreatorUtils
				.getStoredZipBytes(TestUtils.<String, String>treeMapBuilder().put("fromstored", "fstr").build()));

		Map<String, String> contents = new TreeMap<>();
		contents.put("default", "def");
		contents.put("stored", "str");
		contents.put("st2/stored", "str");
		contents.put("deflated", "defl");
		contents.put("fromstored", "fstr");
		contents.put("fs2/fromstored", "fstr");

		CombinedTargetTaskResult res;

		res = runScriptTask("build");
		assertSameContents(res, contents);

		res = runScriptTask("build");
		assertEmpty(getMetric().getRunTaskIdFactories());
		assertSameContents(res, contents);
	}

	private void assertSameContents(CombinedTargetTaskResult res, Map<String, String> expectedcontents)
			throws IOException, FileNotFoundException {
		SakerPath zippath = (SakerPath) res.getTargetTaskResult("zippath");
		ByteArrayRegion zipbytes = files.getAllBytes(zippath);
		ZipCreatorUtils.assertSameContents(zippath, files, expectedcontents);

		ZipCreatorUtils.assertCompression(zipbytes, ZipEntry.DEFLATED, WildcardPath.valueOf("default"));
		ZipCreatorUtils.assertCompression(zipbytes, ZipEntry.STORED, WildcardPath.valueOf("stored"));
		ZipCreatorUtils.assertCompression(zipbytes, ZipEntry.STORED, WildcardPath.valueOf("st2/stored"));
		ZipCreatorUtils.assertCompression(zipbytes, ZipEntry.STORED, WildcardPath.valueOf("fromstored"));
		ZipCreatorUtils.assertCompression(zipbytes, ZipEntry.DEFLATED, WildcardPath.valueOf("fs2/fromstored"));
	}
}
