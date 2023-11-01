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

import testing.saker.SakerTest;
import testing.saker.build.tests.TestUtils;
import testing.saker.nest.util.RepositoryLoadingVariablesMetricEnvironmentTestCase;
import testing.saker.zip.test.utils.ZipCreatorUtils;

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
