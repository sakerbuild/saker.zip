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
package testing.saker.zip.test.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import saker.build.file.ByteArraySakerFile;
import saker.build.file.SakerFile;
import saker.build.file.path.SakerPath;
import saker.build.file.path.WildcardPath;
import saker.build.file.provider.SakerFileProvider;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.io.ByteArrayRegion;
import saker.build.thirdparty.saker.util.io.ByteSource;
import saker.build.thirdparty.saker.util.io.StreamUtils;
import saker.build.thirdparty.saker.util.io.UnsyncByteArrayInputStream;
import saker.build.thirdparty.saker.util.io.UnsyncByteArrayOutputStream;
import testing.saker.SakerTestCase;

public class ZipCreatorUtils {
	private ZipCreatorUtils() {
		throw new UnsupportedOperationException();
	}

	public static SakerFile byteFileHandle(byte[] bytes) {
		return new ByteArraySakerFile("_", bytes);
	}

	public static SakerFile byteFileHandle(ByteArrayRegion bytes) {
		return new ByteArraySakerFile("_", bytes);
	}

	public static SakerFile byteFileHandle(String bytes) {
		return byteFileHandle(bytes.getBytes(StandardCharsets.UTF_8));
	}

	public static SakerFile getZipFile(Map<String, String> contents) throws IOException {
		return byteFileHandle(getZipBytes(contents));
	}

	public static ByteArrayRegion getZipBytes(Map<String, String> contents) throws IOException {
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
			return baos.toByteArrayRegion();
		}
	}

	public static SakerFile getStoredZipFile(Map<String, String> contents) throws IOException {
		return byteFileHandle(getStoredZipBytes(contents));
	}

	public static ByteArrayRegion getStoredZipBytes(Map<String, String> contents) throws IOException {
		try (UnsyncByteArrayOutputStream baos = new UnsyncByteArrayOutputStream()) {
			try (ZipOutputStream zipos = new ZipOutputStream(baos)) {
				for (Entry<String, String> entry : contents.entrySet()) {
					String entrycontents = entry.getValue();
					byte[] entrybytes = entrycontents == null ? null : entrycontents.getBytes();

					ZipEntry ze = new ZipEntry(entry.getKey());
					ze.setMethod(ZipEntry.STORED);
					if (entrybytes != null) {
						ze.setSize(entrybytes.length);
						ze.setCrc(crc(entrybytes));
					} else {
						ze.setSize(0);
						ze.setCrc(crc(ObjectUtils.EMPTY_BYTE_ARRAY));
					}
					zipos.putNextEntry(ze);
					if (entrybytes != null) {
						zipos.write(entrybytes);
					}
					zipos.closeEntry();
				}
			}
			return baos.toByteArrayRegion();
		}
	}

	public static void assertCompression(ByteArrayRegion bytes, int compression) throws IOException, AssertionError {
		assertCompression(bytes, compression, null);
	}

	public static void assertCompression(ByteArrayRegion bytes, int compression, WildcardPath entries)
			throws IOException, AssertionError {
		Map<String, Integer> compressionmethods = ZipCreatorUtils.getCompressionMethods(bytes);
		Stream<Entry<String, Integer>> stream = compressionmethods.entrySet().stream();
		if (entries != null) {
			stream = stream.filter(e -> entries.includes(SakerPath.valueOf(e.getKey())));
		}
		SakerTestCase.assertTrue(stream.allMatch(e -> e.getValue().equals(compression)), compressionmethods::toString);
	}

	public static long crc(byte[] data) {
		CRC32 crc = new CRC32();
		crc.update(data);
		return crc.getValue();
	}

	public static void assertSameContents(SakerPath outpath, SakerFileProvider files,
			Map<String, String> expectedcontents) throws IOException, FileNotFoundException {
		try (ByteSource fileinput = files.openInput(outpath)) {
			assertSameContents(expectedcontents, ByteSource.toInputStream(fileinput));
		}
	}

	public static Map<String, Integer> getCompressionMethods(ByteArrayRegion bytes) throws IOException, AssertionError {
		TreeMap<String, Integer> result = new TreeMap<>();
		try (ZipInputStream zis = new ZipInputStream(new UnsyncByteArrayInputStream(bytes))) {
			for (ZipEntry e; (e = zis.getNextEntry()) != null;) {
				result.put(e.getName(), e.getMethod());
			}
		}
		return result;
	}

	public static Map<String, FileTime> getLastModificationTimes(ByteArrayRegion bytes)
			throws IOException, AssertionError {
		Map<String, FileTime> result = new TreeMap<>();
		try (ZipInputStream zis = new ZipInputStream(new UnsyncByteArrayInputStream(bytes))) {
			for (ZipEntry e; (e = zis.getNextEntry()) != null;) {
				result.put(e.getName(), e.getLastModifiedTime());
			}
		}
		return result;
	}

	public static void assertSameContents(Map<String, String> expectedcontents, ByteArrayRegion bytes)
			throws IOException, AssertionError {
		assertSameContents(expectedcontents, new UnsyncByteArrayInputStream(bytes));
	}

	public static void assertSameContents(Map<String, String> expectedcontents, InputStream inputstream)
			throws IOException, AssertionError {
		Map<String, String> filesmap = new TreeMap<>(expectedcontents);
		try (ZipInputStream zis = new ZipInputStream(inputstream)) {
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
						System.err.println("Expected content:");
						System.err.println(expectedcontent);
						System.err.println("Actual content:");
						System.err.println(entrybytes);
						throw ex;
					}
				}
			}
		}
		SakerTestCase.assertEmpty(filesmap);
	}
}
