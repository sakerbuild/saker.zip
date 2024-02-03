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
package saker.zip.impl.create;

import java.io.Closeable;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import saker.build.exception.InvalidPathFormatException;
import saker.build.file.FileHandle;
import saker.build.file.SakerDirectory;
import saker.build.file.SakerFile;
import saker.build.file.SakerFileBase;
import saker.build.file.content.ContentDescriptor;
import saker.build.file.content.EmptyContentDescriptor;
import saker.build.file.content.HashContentDescriptor;
import saker.build.file.content.MultiContentDescriptor;
import saker.build.file.content.MultiPathContentDescriptor;
import saker.build.file.path.SakerPath;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.io.ByteArrayRegion;
import saker.build.thirdparty.saker.util.io.ConcatInputStream;
import saker.build.thirdparty.saker.util.io.FileUtils;
import saker.build.thirdparty.saker.util.io.IOUtils;
import saker.build.thirdparty.saker.util.io.SerialUtils;
import saker.build.thirdparty.saker.util.io.StreamUtils;
import saker.build.thirdparty.saker.util.io.UnsyncByteArrayInputStream;
import saker.build.thirdparty.saker.util.io.UnsyncByteArrayOutputStream;
import saker.zip.api.create.IncludeResourceMapping;
import saker.zip.api.create.ZipResourceEntry;
import saker.zip.api.create.ZipResourceTransformationContext;
import saker.zip.api.create.ZipResourceTransformer;
import saker.zip.api.create.ZipResourceTransformerFactory;

public class ZipOutputSakerFile extends SakerFileBase {
	private static final FileTime DEFAULT_ENTRY_MODIFICATION_TIME = FileTime.fromMillis(0);

	public interface Builder {
		@Deprecated
		public default void add(SakerPath path, SakerFile file) {
			this.add(path, file instanceof SakerDirectory ? null : file, file.getContentDescriptor());
		}

		public default void add(ZipResourceEntry resourceentry, SakerFile file) {
			this.add(resourceentry, file instanceof SakerDirectory ? null : file, file.getContentDescriptor());
		}

		public void setDefaultEntryModificationTime(FileTime defaultEntryModificationTime);

		//null file if directory
		@Deprecated
		public default void add(SakerPath path, FileHandle file, ContentDescriptor content) {
			this.add(ZipResourceEntry.create(path), file, content);
		}

		//null file if directory
		public void add(ZipResourceEntry resourceentry, FileHandle file, ContentDescriptor content);

		public void addIncludeFromArchive(FileHandle archivehandle, ContentDescriptor archivecontents,
				IncludeResourceMapping resourcemappings);

		public void addResourceTransformer(ZipResourceTransformerFactory transformer);

		public SakerFile build(String name);
	}

	private static final class IncludeFile {
		protected final ZipResourceEntry resourceEntry;
		protected final FileHandle fileHandle;

		public IncludeFile(ZipResourceEntry resourceEntry, FileHandle fileHandle) {
			this.resourceEntry = resourceEntry;
			this.fileHandle = fileHandle;
		}
	}

	protected static final class BuilderImpl implements Builder {
		protected NavigableMap<SakerPath, IncludeFile> files = new TreeMap<>();
		//linked has map for reproducible order
		protected Map<FileHandle, IncludeResourceMapping> includes = new LinkedHashMap<>();

		protected Collection<ContentDescriptor> subContents = new ArrayList<>();
		protected NavigableMap<SakerPath, ContentDescriptor> subEntryContents = new TreeMap<>();

		protected FileTime defaultEntryModificationTime;

		protected List<ZipResourceTransformerFactory> transformers = new ArrayList<>();

		@Override
		public void addResourceTransformer(ZipResourceTransformerFactory transformer) {
			this.transformers.add(transformer);
		}

		@Override
		public void setDefaultEntryModificationTime(FileTime defaultEntryModificationTime) {
			this.defaultEntryModificationTime = defaultEntryModificationTime;
		}

		@Override
		public void add(ZipResourceEntry resourceentry, FileHandle file, ContentDescriptor content) {
			Objects.requireNonNull(resourceentry, "resourceentry");
			Objects.requireNonNull(content, "content");

			SakerPath path = resourceentry.getEntryPath();
			if (!path.isForwardRelative()) {
				throw new InvalidPathFormatException("Zip entry path must be forward relative: " + path);
			}

			IncludeFile prevsw = this.files.putIfAbsent(path, new IncludeFile(resourceentry, file));
			if (prevsw != null) {
				throw new IllegalArgumentException("Duplicate ZIP entries: " + path);
			}
			ContentDescriptor prevcd = this.subEntryContents.putIfAbsent(path, content);
			if (prevcd != null) {
				//shouldn't happen
				throw new IllegalArgumentException("Internal error, duplicate ZIP entry contents: " + path
						+ " : previous: " + prevcd + " new: " + content);
			}
		}

		@Override
		public void addIncludeFromArchive(FileHandle archivehandle, ContentDescriptor archivecontents,
				IncludeResourceMapping resourcemappings) {
			Objects.requireNonNull(archivehandle, "archive handle");
			Objects.requireNonNull(resourcemappings, "resource mappings");

			IncludeResourceMapping previnclude = includes.putIfAbsent(archivehandle, resourcemappings);
			if (previnclude != null) {
				throw new IllegalArgumentException("Multiple includes from archive: " + archivehandle + " with "
						+ previnclude + " and " + resourcemappings);
			}
			this.subContents.add(archivecontents);
		}

		@Override
		public SakerFile build(String name) {
			if (files == null) {
				throw new IllegalStateException("Builder already consumed.");
			}
			ZipOutputSakerFile result = new ZipOutputSakerFile(name, this);
			files = null;
			return result;
		}

		protected FileTime getDefaultEntryModificationTime() {
			FileTime defaultentrymodtime = defaultEntryModificationTime;
			if (defaultentrymodtime == null || files.isEmpty()) {
				//do not take into account if it is not actually used
				defaultentrymodtime = DEFAULT_ENTRY_MODIFICATION_TIME;
			}
			return defaultentrymodtime;
		}
	}

	public static Builder builder() {
		return new BuilderImpl();
	}

	protected static BuilderImpl zipBuilderImpl() {
		return new BuilderImpl();
	}

	protected final Collection<? extends IncludeFile> files;
	protected final Map<FileHandle, IncludeResourceMapping> includes;
	protected final ZipFileContentDescriptor contentDescriptor;

	protected ZipOutputSakerFile(String name, BuilderImpl builder)
			throws NullPointerException, InvalidPathFormatException {
		super(name);
		this.files = builder.files.values();
		ZipFileContentDescriptor contentdescriptor = new ZipFileContentDescriptor(
				builder.getDefaultEntryModificationTime(), MultiContentDescriptor.create(builder.subContents),
				new MultiPathContentDescriptor(builder.subEntryContents),
				getResourceEntriesContentDescriptor(this.files), builder.transformers);
		this.includes = builder.includes;
		this.contentDescriptor = contentdescriptor;
	}

	@Override
	public ContentDescriptor getContentDescriptor() {
		return contentDescriptor;
	}

	@Override
	public void writeToStreamImpl(OutputStream os) throws IOException, NullPointerException {
		ZipOutputStream zipos = new ZipOutputStream(os);
		Exception exc = null;
		try {
			if (contentDescriptor.transformers.isEmpty()) {
				writeNonTransformingZip(zipos);
			} else {
				writeZipWithTransformers(zipos);
			}
		} catch (Exception e) {
			exc = e;
			throw e;
		} finally {
			try {
				//try catch to not lose the exception happening inside the main body
				zipos.finish();
			} catch (IOException e) {
				throw IOUtils.addExc(e, exc);
			}
		}
	}

	private static abstract class AbstractTransformationContext {
		protected final ZipOutputStream zipOut;
		protected final UnsyncByteArrayOutputStream buffer = new UnsyncByteArrayOutputStream(1024 * 8);

		private int currentLevel = Deflater.DEFAULT_COMPRESSION;
		private CRC32 crc = null;

		public AbstractTransformationContext(ZipOutputStream zipOut) {
			this.zipOut = zipOut;
		}

		protected CRC32 getCrc() {
			if (crc == null) {
				crc = new CRC32();
			} else {
				crc.reset();
			}
			return crc;
		}

		protected void updateCompression(ZipEntry entry, int method, int level) {
			if (method < 0) {
				//in case of unspecified method, use the defaults
				method = ZipOutputStream.DEFLATED;
				level = Deflater.DEFAULT_COMPRESSION;
			}
			switch (method) {
				case ZipOutputStream.DEFLATED: {
					entry.setMethod(ZipOutputStream.DEFLATED);
					if (level < 0) {
						level = Deflater.DEFAULT_COMPRESSION;
					}
					if (this.currentLevel != level) {
						zipOut.setLevel(level);
						this.currentLevel = level;
					}
					break;
				}
				case ZipOutputStream.STORED: {
					//no need to change the output stream itself
					entry.setMethod(ZipOutputStream.STORED);
					break;
				}
				default: {
					//unknown method to us,
					//update the method and level of the stream
					//pass though the level value even if < 0
					zipOut.setMethod(method);
					if (this.currentLevel != level) {
						zipOut.setLevel(level);
						this.currentLevel = level;
					}
					break;
				}
			}
		}

		public abstract void transform(ZipResourceEntry resourceentry, ZipEntry entry,
				UnsyncByteArrayOutputStream contentbuffer) throws IOException;

		public abstract void transform(ZipResourceEntry resourceentry, ZipEntry entry, InputStream input)
				throws IOException;

		public abstract void transform(ZipResourceEntry resourceentry, FileHandle handle) throws IOException;

		public abstract void transform(ZipResourceEntry entry, InputStream input) throws IOException;

		public abstract void transformDirectory(ZipResourceEntry entry) throws IOException;
	}

	private static class NonTransformationContext extends AbstractTransformationContext {
		private final NavigableMap<SakerPath, Boolean> entries = new TreeMap<>(SakerPath::compareToIgnoreCase);
		private final FileTime defaultModificationTime;

		public NonTransformationContext(ZipOutputStream zipOut, FileTime defaultmodtime) {
			super(zipOut);
			this.defaultModificationTime = defaultmodtime;
		}

		@Override
		public void transform(ZipResourceEntry resourceentry, ZipEntry entry, UnsyncByteArrayOutputStream contentbuffer)
				throws IOException {
			SakerPath entrypath = resourceentry.getEntryPath();
			checkEntryFileDuplication(entrypath);

			ZipEntry ze = createNextEntry(entrypath.toString(), resourceentry);
			updateCompression(ze, resourceentry.getMethod(), resourceentry.getLevel());
			if (ze.getMethod() == ZipEntry.STORED) {
				long zcrc = entry.getCrc();
				if (zcrc < 0) {
					CRC32 crc = getCrc();
					int size = buffer.size();
					crc.update(buffer.getBuffer(), 0, size);
					zcrc = crc.getValue();
				}
				ze.setCrc(zcrc);
				ze.setSize(contentbuffer.size());
			}

			zipOut.putNextEntry(ze);
			contentbuffer.writeTo(zipOut);
			zipOut.closeEntry();
		}

		@Override
		public void transform(ZipResourceEntry resourceentry, ZipEntry entry, InputStream input) throws IOException {
			SakerPath entrypath = resourceentry.getEntryPath();
			checkEntryFileDuplication(entrypath);

			ZipEntry ze = createNextEntry(entrypath.toString(), resourceentry);
			updateCompression(ze, resourceentry.getMethod(), resourceentry.getLevel());
			if (ze.getMethod() == ZipEntry.STORED) {
				long zcrc = entry.getCrc();
				long zsize = entry.getSize();
				if (zcrc < 0 || zsize < 0) {
					//if we need to do the calculation, reuse the code with the other function
					transformStoredStream(input, ze);
					return;
				}
				ze.setCrc(zcrc);
				ze.setSize(zsize);
			}

			zipOut.putNextEntry(ze);
			StreamUtils.copyStream(input, zipOut, buffer.getBuffer());
			zipOut.closeEntry();
		}

		@Override
		public void transform(ZipResourceEntry resourceentry, FileHandle handle) throws IOException {
			SakerPath entrypath = resourceentry.getEntryPath();
			checkEntryFileDuplication(entrypath);

			ZipEntry ze = createNextEntry(entrypath.toString(), resourceentry);
			updateCompression(ze, resourceentry.getMethod(), resourceentry.getLevel());

			if (ze.getMethod() == ZipEntry.STORED) {
				UnsyncByteArrayOutputStream buffer = this.buffer;
				buffer.reset();
				handle.writeTo((OutputStream) buffer);

				CRC32 crc = getCrc();
				int size = buffer.size();
				crc.update(buffer.getBuffer(), 0, size);

				ze.setCrc(crc.getValue());
				ze.setSize(size);
				zipOut.putNextEntry(ze);
				buffer.writeTo(zipOut);
			} else {
				zipOut.putNextEntry(ze);
				handle.writeTo(zipOut);
			}

			zipOut.closeEntry();
		}

		@Override
		public void transform(ZipResourceEntry resourceentry, InputStream input) throws IOException {
			SakerPath entrypath = resourceentry.getEntryPath();
			checkEntryFileDuplication(entrypath);

			ZipEntry ze = createNextEntry(entrypath.toString(), resourceentry);
			updateCompression(ze, resourceentry.getMethod(), resourceentry.getLevel());

			transformStream(input, ze);
		}

		private void transformStream(InputStream input, ZipEntry ze) throws IOException {
			if (ze.getMethod() == ZipEntry.STORED) {
				transformStoredStream(input, ze);
			} else {
				zipOut.putNextEntry(ze);
				StreamUtils.copyStream(input, zipOut, buffer.getBuffer());
				zipOut.closeEntry();
			}
		}

		private void transformStoredStream(InputStream input, ZipEntry ze) throws IOException {
			UnsyncByteArrayOutputStream buffer = this.buffer;
			buffer.reset();
			buffer.readFrom(input);
			CRC32 crc = getCrc();
			int size = buffer.size();
			crc.update(buffer.getBuffer(), 0, size);

			ze.setCrc(crc.getValue());
			ze.setSize(size);

			zipOut.putNextEntry(ze);
			buffer.writeTo(zipOut);
			zipOut.closeEntry();
		}

		@Override
		public void transformDirectory(ZipResourceEntry entry) throws IOException {
			SakerPath entrypath = entry.getEntryPath();
			if (addCheckEntryDirectoryDuplication(entrypath)) {
				ZipEntry ze = createNextEntry(entrypath + "/", entry);
				if (ze.getMethod() == ZipEntry.STORED) {
					ze.setCrc(0); // zero length data has 0 crc
					ze.setSize(0);
				}
				zipOut.putNextEntry(ze);

				zipOut.closeEntry();
			}

		}

		private ZipEntry createNextEntry(String entrypath, ZipResourceEntry resourceentry) {
			ZipEntry ze = new ZipEntry(entrypath);
			FileTime modtime = resourceentry.getModificationTime();

			ze.setLastModifiedTime(modtime == null ? defaultModificationTime : modtime);
			updateCompression(ze, resourceentry.getMethod(), resourceentry.getLevel());
			return ze;
		}

		private void checkEntryFileDuplication(SakerPath entrypath) {
			if (entries.putIfAbsent(entrypath, Boolean.FALSE) != null) {
				throw new IllegalArgumentException("Duplicate zip file entry: " + entrypath);
			}
		}

		private boolean addCheckEntryDirectoryDuplication(SakerPath entrypath) {
			Boolean prev = entries.putIfAbsent(entrypath, Boolean.TRUE);
			if (prev == Boolean.FALSE) {
				//already present as a file
				throw new IllegalArgumentException("Zip file entry already exists for directory: " + entrypath);
			}
			return prev == null;
		}

	}

	private static class ResourceBufferingInputStream extends InputStream {
		//XXX we can implement guarding, when a resource with the same bytes as the source is being regenerated to avoid infinite looping

		protected UnsyncByteArrayOutputStream buffer;
		protected InputStream in;

		public ResourceBufferingInputStream(UnsyncByteArrayOutputStream buffer, InputStream in) {
			this.buffer = buffer;
			this.in = in;
		}

		@Override
		public int read() throws IOException {
			int result = in.read();
			if (result >= 0) {
				buffer.write(result);
			}
			return result;
		}

		@Override
		public int read(byte[] b) throws IOException {
			return this.read(b, 0, b.length);
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int result = in.read(b, off, len);
			if (result > 0) {
				buffer.write(b, off, result);
			}
			return result;
		}

	}

	private static class TransformationContextImpl extends AbstractTransformationContext
			implements ZipResourceTransformationContext, Closeable {
		private static class TransformerState {
			protected final ZipResourceTransformer transformer;

			public TransformerState(ZipResourceTransformer transformer) {
				this.transformer = transformer;
			}
		}

		private static class PendingResource {
			protected final ZipResourceEntry entry;
			/**
			 * The bytes of the entry.
			 * <p>
			 * <code>null</code> if represents a directory.
			 */
			protected final ByteArrayRegion bytes;

			public PendingResource(ZipResourceEntry entry, ByteArrayRegion bytes) {
				this.entry = entry;
				this.bytes = bytes;
			}

		}

		protected final UnsyncByteArrayOutputStream transformingBuffer = new UnsyncByteArrayOutputStream(8 * 1024);
		protected final List<TransformerState> transformers;
		protected final ArrayDeque<PendingResource> pendingResources = new ArrayDeque<>();
		protected final FileTime defaultModificationTime;
		private final NavigableSet<SakerPath> entries = new TreeSet<>(SakerPath::compareToIgnoreCase);

		public TransformationContextImpl(ZipOutputStream zipos, List<ZipResourceTransformer> transformers,
				FileTime defaultmodificationtime) {
			super(zipos);
			this.defaultModificationTime = defaultmodificationtime;
			this.transformers = new ArrayList<>();
			for (ZipResourceTransformer transformer : transformers) {
				this.transformers.add(new TransformerState(transformer));
			}
		}

		@Override
		public void appendDirectory(SakerPath entrypath, FileTime modificationtime) {
			appendDirectory(ZipResourceEntry.create(entrypath, modificationtime));
		}

		@Override
		public void appendDirectory(ZipResourceEntry resourceentry) throws NullPointerException {
			Objects.requireNonNull(resourceentry, "resource entry");

			PendingResource pendingres = new PendingResource(resourceentry, null);
			pendingResources.add(pendingres);
		}

		@Override
		public OutputStream appendFile(SakerPath entrypath, FileTime modificationtime) {
			Objects.requireNonNull(entrypath, "entry path");
			return appendFile(ZipResourceEntry.create(entrypath, modificationtime));
		}

		@Override
		public OutputStream appendFile(ZipResourceEntry resourceentry) throws NullPointerException {
			Objects.requireNonNull(resourceentry, "resource entry");
			return new UnsyncByteArrayOutputStream() {
				private boolean closed = false;

				@Override
				public void close() {
					//guard to avoid adding the resource multiple times
					if (closed) {
						return;
					}
					closed = true;

					PendingResource pendingres = new PendingResource(resourceentry, toByteArrayRegion());
					pendingResources.add(pendingres);
					super.close();
				}
			};
		}

		@Override
		public void transform(ZipResourceEntry resourceentry, ZipEntry zipentry,
				UnsyncByteArrayOutputStream contentbuffer) throws IOException {
			try (UnsyncByteArrayInputStream is = new UnsyncByteArrayInputStream(contentbuffer.toByteArrayRegion())) {
				transformSingleEntryImpl(resourceentry, is, zipentry);

				executePendingTransformations();
			}
		}

		@Override
		public void transform(ZipResourceEntry resourceentry, ZipEntry zipentry, InputStream input) throws IOException {
			transformSingleEntryImpl(resourceentry, input, zipentry);

			executePendingTransformations();
		}

		@Override
		public void transformDirectory(ZipResourceEntry entry) throws IOException {
			transformSingleEntryImpl(entry, null, null);

			executePendingTransformations();
		}

		@Override
		public void transform(ZipResourceEntry resourceentry, FileHandle handle) throws IOException {
			try (InputStream is = handle.openInputStream()) {
				transform(resourceentry, is);
			}
		}

		@Override
		public void transform(ZipResourceEntry entry, InputStream input) throws IOException {
			transformSingleEntryImpl(entry, input, null);

			executePendingTransformations();
		}

		@Override
		public void close() throws IOException {
			for (TransformerState transformer : transformers) {
				transformer.transformer.flush(this);
				executePendingTransformations();
			}
			for (Iterator<TransformerState> it = transformers.iterator(); it.hasNext();) {
				TransformerState transformer = it.next();
				//remove first, so no more transformations
				it.remove();
				transformer.transformer.end(this);
				executePendingTransformations();
			}
		}

		private void executePendingTransformations() throws IOException {
			while (true) {
				PendingResource pendingres = pendingResources.pollFirst();
				if (pendingres == null) {
					break;
				}
				transformSingleEntryImpl(pendingres.entry,
						pendingres.bytes == null ? null : new UnsyncByteArrayInputStream(pendingres.bytes), null);
			}
		}

		private ZipResourceEntry callTransformationsSingleEntryImpl(ZipResourceEntry entry, InputStream input)
				throws IOException {
			if (transformers.isEmpty()) {
				return entry;
			}
			transformingBuffer.reset();
			ResourceBufferingInputStream bufferingin = input == null ? null
					: new ResourceBufferingInputStream(transformingBuffer, input);
			for (TransformerState transformer : transformers) {
				InputStream transformerin;
				if (bufferingin == null || transformingBuffer.isEmpty()) {
					transformerin = bufferingin;
				} else {
					transformerin = new ConcatInputStream(
							new UnsyncByteArrayInputStream(transformingBuffer.toByteArrayRegion()), bufferingin);
				}
				ZipResourceEntry replacemententry = transformer.transformer.process(this, entry, transformerin);
				if (replacemententry == null) {
					//consumed
					return null;
				}
				entry = replacemententry;
			}
			return entry;
		}

		/**
		 * @param input
		 *            <code>null</code> for directories.
		 * @param zipentry
		 *            may be <code>null</code> if not from a zip.
		 */
		private void transformSingleEntryImpl(ZipResourceEntry entry, InputStream input, ZipEntry zipentry)
				throws IOException {
			ZipResourceEntry replacemententry = callTransformationsSingleEntryImpl(entry, input);
			if (replacemententry == null) {
				//the entry was consumed
				return;
			}
			entry = replacemententry;

			//the input was not consumed

			SakerPath entrypath = entry.getEntryPath();
			FileTime modtime = entry.getModificationTime();

			checkEntryDuplication(entrypath);
			ZipEntry ze = new ZipEntry(input == null ? entrypath.toString() + "/" : entrypath.toString());
			ze.setLastModifiedTime(modtime == null ? defaultModificationTime : modtime);
			int method = entry.getMethod();
			updateCompression(ze, method, entry.getLevel());
			if (method == ZipEntry.STORED) {
				//these need to be set in case of stored
				if (input == null) {
					ze.setCrc(0);
					ze.setSize(0);
				} else if (zipentry != null && zipentry.getCrc() >= 0 && zipentry.getSize() >= 0) {
					//set the crc and size from the zip entry, if both are available, otherwise we will need to calculate them
					ze.setCrc(zipentry.getCrc());
					ze.setSize(zipentry.getSize());
				} else {
					//we need to read the data and calculate the length and crc
					CRC32 crc = getCrc();
					transformingBuffer.readFrom(input);
					int size = transformingBuffer.size();
					crc.update(transformingBuffer.getBuffer(), 0, size);
					ze.setCrc(crc.getValue());
					ze.setSize(size);
				}
			}

			zipOut.putNextEntry(ze);
			if (input != null) {
				transformingBuffer.writeTo(zipOut);
				StreamUtils.copyStream(input, zipOut, transformingBuffer.getBuffer());
			}
			zipOut.closeEntry();
		}

		private void checkEntryDuplication(SakerPath entrypath) {
			if (!entries.add(entrypath)) {
				throw new IllegalArgumentException("Duplicate zip entry: " + entrypath);
			}
		}
	}

	private void writeZipWithTransformers(ZipOutputStream zipos) throws IOException {
		List<ZipResourceTransformer> transformers = new ArrayList<>();
		for (ZipResourceTransformerFactory factory : contentDescriptor.transformers) {
			ZipResourceTransformer transformer = factory.createTransformer();
			if (transformer == null) {
				throw new NullPointerException("Zip resource transformer factory returned null: " + factory);
			}
			transformers.add(transformer);
		}
		try (TransformationContextImpl context = new TransformationContextImpl(zipos, transformers,
				getDefaultModificationTime())) {
			writeZipFiles(context);
			writeZipIncludes(context);
		}
	}

	private void writeNonTransformingZip(ZipOutputStream zipos) throws IOException {
		NonTransformationContext context = new NonTransformationContext(zipos, getDefaultModificationTime());
		writeZipFiles(context);
		writeZipIncludes(context);
	}

	private void writeZipFiles(AbstractTransformationContext context) throws IOException {
		if (files.isEmpty()) {
			return;
		}
		FileTime defaultmodtime = getDefaultModificationTime();
		for (IncludeFile includefile : files) {
			FileHandle handle = includefile.fileHandle;
			ZipResourceEntry resourceentry = includefile.resourceEntry;
			if (resourceentry.getModificationTime() == null) {
				resourceentry = resourceentry.withModificationTime(defaultmodtime);
			}
			if (handle == null) {
				context.transformDirectory(resourceentry);
			} else {
				context.transform(resourceentry, handle);
			}
		}
	}

	private void writeZipIncludes(AbstractTransformationContext context) throws IOException {
		if (includes.isEmpty()) {
			return;
		}
		NavigableMap<SakerPath, ZipResourceEntry> multientryaddpaths = new TreeMap<>();
		for (Entry<FileHandle, IncludeResourceMapping> incentry : includes.entrySet()) {
			FileHandle handle = incentry.getKey();
			IncludeResourceMapping inc = incentry.getValue();
			try (InputStream archivein = handle.openInputStream();
					ZipInputStream zis = new ZipInputStream(archivein)) {
				for (ZipEntry ze; (ze = zis.getNextEntry()) != null;) {
					boolean directory = ze.isDirectory();

					ZipResourceEntry zipresourceentry = ZipResourceEntry.from(ze);
					SakerPath path = zipresourceentry.getEntryPath();

					Collection<? extends ZipResourceEntry> addentrypaths = inc.mapResource(zipresourceentry, directory);
					if (ObjectUtils.isNullOrEmpty(addentrypaths)) {
						//don't include
						continue;
					}

					if (directory) {
						for (ZipResourceEntry addentry : addentrypaths) {
							validateMappingResultPath(path, inc, addentry.getEntryPath());

							context.transformDirectory(addentry);
						}
					} else {
						for (ZipResourceEntry addentry : addentrypaths) {
							SakerPath entrypath = addentry.getEntryPath();
							validateMappingResultPath(path, inc, entrypath);

							ZipResourceEntry prev = multientryaddpaths.putIfAbsent(entrypath, addentry);
							if (prev != null) {
								//multiple entries found for the same name
								//XXX maybe handle this somehow? currently taking one is okay, doesn't seem very relevant at the moment
							}
						}

						Entry<SakerPath, ZipResourceEntry> addentrypath = multientryaddpaths.pollFirstEntry();
						if (addentrypath != null) {
							if (!multientryaddpaths.isEmpty()) {
								//buffer the resource data and transform for each additional entry path
								UnsyncByteArrayOutputStream bytebuffer = context.buffer;
								bytebuffer.reset();
								bytebuffer.readFrom(zis);

								while (true) {
									context.transform(addentrypath.getValue(), ze, bytebuffer);
									addentrypath = multientryaddpaths.pollFirstEntry();
									if (addentrypath == null) {
										break;
									}
								}
							} else {
								context.transform(addentrypath.getValue(), ze, zis);
							}
						}
					}

				}
			}
		}
	}

	private static void validateMappingResultPath(SakerPath path, IncludeResourceMapping mapping,
			SakerPath resultpath) {
		if (resultpath == null) {
			throw new NullPointerException(
					"Include resource mapping produced null path for: " + path + " by " + mapping);
		}
		if (!resultpath.isForwardRelative()) {
			throw new InvalidPathFormatException("Include resource mapped to non forward relative path: " + path
					+ " to " + resultpath + " by " + mapping);
		}
		if (SakerPath.EMPTY.equals(resultpath)) {
			throw new InvalidPathFormatException(
					"Include resource mapped to empty path: " + path + " to " + resultpath + " by " + mapping);
		}
	}

	protected final FileTime getDefaultModificationTime() {
		return contentDescriptor.defaultEntryModificationTime;
	}

	/**
	 * Creates a content descriptor that contains the information from the resource entries in the argument.
	 */
	private static ContentDescriptor getResourceEntriesContentDescriptor(Iterable<? extends IncludeFile> entries) {
		//a hash content descriptor is created instead of storing each and every resource entry and checking for equality
		Iterator<? extends IncludeFile> it = entries.iterator();
		if (!it.hasNext()) {
			return EmptyContentDescriptor.INSTANCE;
		}

		//buffer for other fields
		byte[] buffer = new byte[Long.BYTES + Integer.BYTES * 2];
		MessageDigest hasher = FileUtils.getDefaultFileHasher();
		do {
			IncludeFile incfile = it.next();
			ZipResourceEntry entry = incfile.resourceEntry;
			hasher.update(entry.getEntryPath().toString().getBytes(StandardCharsets.UTF_8));
			FileTime modtime = entry.getModificationTime();
			int idx = 0;
			if (modtime != null) {
				SerialUtils.writeLongToBuffer(modtime.toMillis(), buffer, idx);
				idx += Long.BYTES;
			}
			int method = entry.getMethod();
			int level = entry.getLevel();
			SerialUtils.writeIntToBuffer(method, buffer, idx);
			idx += Integer.BYTES;
			SerialUtils.writeIntToBuffer(level, buffer, idx);
			idx += Integer.BYTES;

			hasher.update(buffer, 0, idx);
		} while (it.hasNext());
		return HashContentDescriptor.createWithHash(hasher.digest());
	}

	protected static class ZipFileContentDescriptor implements ContentDescriptor, Externalizable {
		private static final long serialVersionUID = 1L;

		protected FileTime defaultEntryModificationTime;
		protected ContentDescriptor subContents;
		protected ContentDescriptor subPathContents;
		protected ContentDescriptor entriesContents;
		protected List<ZipResourceTransformerFactory> transformers;

		/**
		 * For {@link Externalizable}.
		 */
		public ZipFileContentDescriptor() {
		}

		public ZipFileContentDescriptor(FileTime defaultEntryModificationTime, ContentDescriptor subContents,
				ContentDescriptor subPathContents, ContentDescriptor entriesContents,
				List<ZipResourceTransformerFactory> transformers) {
			this.defaultEntryModificationTime = defaultEntryModificationTime;
			this.subContents = subContents;
			this.subPathContents = subPathContents;
			this.entriesContents = entriesContents;
			this.transformers = transformers;
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			out.writeLong(defaultEntryModificationTime.toMillis());
			out.writeObject(subContents);
			out.writeObject(subPathContents);
			out.writeObject(entriesContents);
			SerialUtils.writeExternalCollection(out, transformers);
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			defaultEntryModificationTime = FileTime.fromMillis(in.readLong());
			subContents = (ContentDescriptor) in.readObject();
			subPathContents = (ContentDescriptor) in.readObject();
			entriesContents = (ContentDescriptor) in.readObject();
			transformers = SerialUtils.readExternalImmutableList(in);
		}

		@Override
		public boolean isChanged(ContentDescriptor previouscontent) {
			if (!(previouscontent instanceof ZipFileContentDescriptor)) {
				return true;
			}
			ZipFileContentDescriptor zipcd = (ZipFileContentDescriptor) previouscontent;
			if (!Objects.equals(defaultEntryModificationTime, zipcd.defaultEntryModificationTime)) {
				return true;
			}
			if (this.subContents.isChanged(zipcd.subContents)) {
				return true;
			}
			if (this.subPathContents.isChanged(zipcd.subPathContents)) {
				return true;
			}
			if (this.entriesContents.isChanged(zipcd.entriesContents)) {
				return true;
			}
			if (!Objects.equals(transformers, zipcd.transformers)) {
				return true;
			}
			return false;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((defaultEntryModificationTime == null) ? 0 : defaultEntryModificationTime.hashCode());
			result = prime * result + ((subContents == null) ? 0 : subContents.hashCode());
			result = prime * result + ((subPathContents == null) ? 0 : subPathContents.hashCode());
			result = prime * result + ((entriesContents == null) ? 0 : entriesContents.hashCode());
			result = prime * result + ((transformers == null) ? 0 : transformers.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ZipFileContentDescriptor other = (ZipFileContentDescriptor) obj;
			if (defaultEntryModificationTime == null) {
				if (other.defaultEntryModificationTime != null)
					return false;
			} else if (!defaultEntryModificationTime.equals(other.defaultEntryModificationTime))
				return false;
			if (subContents == null) {
				if (other.subContents != null)
					return false;
			} else if (!subContents.equals(other.subContents))
				return false;
			if (subPathContents == null) {
				if (other.subPathContents != null)
					return false;
			} else if (!subPathContents.equals(other.subPathContents))
				return false;
			if (entriesContents == null) {
				if (other.entriesContents != null)
					return false;
			} else if (!entriesContents.equals(other.entriesContents))
				return false;
			if (transformers == null) {
				if (other.transformers != null)
					return false;
			} else if (!transformers.equals(other.transformers))
				return false;
			return true;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder(getClass().getSimpleName());
			builder.append("[defaultEntryModificationTime=");
			builder.append(defaultEntryModificationTime);
			builder.append(", subContents=");
			builder.append(subContents);
			builder.append(", subPathContents=");
			builder.append(subPathContents);
			builder.append(", entriesContents=");
			builder.append(entriesContents);
			builder.append(", transformers=");
			builder.append(transformers);
			builder.append("]");
			return builder.toString();
		}
	}

}
