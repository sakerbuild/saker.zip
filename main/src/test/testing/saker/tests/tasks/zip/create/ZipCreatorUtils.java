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
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import saker.build.file.path.SakerPath;
import saker.build.file.provider.SakerFileProvider;
import saker.build.thirdparty.saker.util.io.ByteArrayRegion;
import saker.build.thirdparty.saker.util.io.ByteSource;
import saker.build.thirdparty.saker.util.io.StreamUtils;
import saker.build.thirdparty.saker.util.io.UnsyncByteArrayOutputStream;
import testing.saker.SakerTestCase;

public class ZipCreatorUtils {
	private ZipCreatorUtils() {
		throw new UnsupportedOperationException();
	}

	public static ByteArrayRegion getZipBytes(Map<String, String> contents) throws IOException {
		ByteArrayRegion includezipbytes;
		try (UnsyncByteArrayOutputStream baos = new UnsyncByteArrayOutputStream()) {
			try (ZipOutputStream zipos = new ZipOutputStream(baos)) {
				for (Entry<String, String> entry : contents.entrySet()) {
					zipos.putNextEntry(new ZipEntry(entry.getKey()));
					String entrycontents = entry.getValue();
					if (entrycontents != null) {
						zipos.write(entrycontents.getBytes());
					}
					zipos.closeEntry();
				}
			}
			includezipbytes = baos.toByteArrayRegion();
		}
		return includezipbytes;
	}

	public static void assertSameContents(SakerPath outpath, SakerFileProvider files,
			Map<String, String> expectedcontents) throws IOException, FileNotFoundException {
		Map<String, String> filesmap = new TreeMap<>(expectedcontents);

		try (ByteSource fileinput = files.openInput(outpath);
				ZipInputStream zis = new ZipInputStream(ByteSource.toInputStream(fileinput))) {
			for (ZipEntry e; (e = zis.getNextEntry()) != null;) {
				String name = e.getName();
				if (e.isDirectory()) {
					SakerTestCase.assertTrue(filesmap.containsKey(name), name);
					SakerTestCase.assertNull(filesmap.remove(name), name);
				} else {
					String expectedcontent = filesmap.remove(name);
					SakerTestCase.assertNonNull(expectedcontent, name);

					ByteArrayRegion entrybytes = StreamUtils.readStreamFully(zis);
					try {
						SakerTestCase.assertEquals(expectedcontent.getBytes(), entrybytes.copyOptionally(), name);
					} catch (AssertionError ex) {
						System.err.println(expectedcontent);
						System.err.println(entrybytes);
						throw ex;
					}
				}
			}
		}
		SakerTestCase.assertEmpty(filesmap);
	}
}
