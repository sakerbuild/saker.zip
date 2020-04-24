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
import java.nio.file.attribute.FileTime;
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
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import saker.build.exception.InvalidPathFormatException;
import saker.build.file.FileHandle;
import saker.build.file.SakerDirectory;
import saker.build.file.SakerFile;
import saker.build.file.SakerFileBase;
import saker.build.file.content.ContentDescriptor;
import saker.build.file.content.MultiContentDescriptor;
import saker.build.file.path.SakerPath;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.io.ByteArrayRegion;
import saker.build.thirdparty.saker.util.io.ConcatInputStream;
import saker.build.thirdparty.saker.util.io.IOUtils;
import saker.build.thirdparty.saker.util.io.SerialUtils;
import saker.build.thirdparty.saker.util.io.StreamUtils;
import saker.build.thirdparty.saker.util.io.UnsyncByteArrayInputStream;
import saker.build.thirdparty.saker.util.io.UnsyncByteArrayOutputStream;
import saker.zip.api.create.IncludeResourceMapping;
import saker.zip.api.create.ZipResourceTransformationContext;
import saker.zip.api.create.ZipResourceTransformer;
import saker.zip.api.create.ZipResourceTransformerFactory;

public class ZipOutputSakerFile extends SakerFileBase {
	private static final FileTime DEFAULT_ENTRY_MODIFICATION_TIME = FileTime.fromMillis(0);

	public interface Builder {
		public void add(SakerPath path, SakerFile file);

		public void setDefaultEntryModificationTime(FileTime defaultEntryModificationTime);

		//null file if directory
		public void add(SakerPath path, FileHandle file, ContentDescriptor content);

		public void addIncludeFromArchive(FileHandle archivehandle, ContentDescriptor archivecontents,
				IncludeResourceMapping resourcemappings);

		public void addResourceTransformer(ZipResourceTransformerFactory transformer);

		public SakerFile build(String name);
	}

	protected static final class BuilderImpl implements Builder {
		protected NavigableMap<SakerPath, FileHandle> files = new TreeMap<>();
		//linked has map for reproducible order
		protected Map<FileHandle, ZipInclude> includes = new LinkedHashMap<>();

		protected Collection<ContentDescriptor> subContents = new ArrayList<>();

		protected FileTime defaultEntryModificationTime;

		protected List<ZipResourceTransformerFactory> transformers = new ArrayList<>();

		@Override
		public void addResourceTransformer(ZipResourceTransformerFactory transformer) {
			this.transformers.add(transformer);
		}

		@Override
		public void add(SakerPath path, SakerFile file) {
			this.add(path, file instanceof SakerDirectory ? null : file, file.getContentDescriptor());
		}

		@Override
		public void setDefaultEntryModificationTime(FileTime defaultEntryModificationTime) {
			this.defaultEntryModificationTime = defaultEntryModificationTime;
		}

		@Override
		public void add(SakerPath path, FileHandle file, ContentDescriptor content) {
			Objects.requireNonNull(path, "path");
			Objects.requireNonNull(content, "content");
			if (!path.isForwardRelative()) {
				throw new InvalidPathFormatException("Zip entry path must be forward relative.");
			}
			FileHandle prevsw = this.files.putIfAbsent(path, file);
			if (prevsw != null) {
				throw new IllegalArgumentException("Duplicate ZIP entries: " + path);
			}
			EntryPathContentDescriptor entrypathcd = new EntryPathContentDescriptor(path, content);
			this.subContents.add(entrypathcd);
		}

		@Override
		public void addIncludeFromArchive(FileHandle archivehandle, ContentDescriptor archivecontents,
				IncludeResourceMapping resourcemappings) {
			Objects.requireNonNull(archivehandle, "archive handle");
			Objects.requireNonNull(resourcemappings, "resource mappings");

			ZipInclude include = new ZipInclude(resourcemappings);
			ZipInclude previnclude = includes.putIfAbsent(archivehandle, include);
			if (previnclude != null) {
				throw new IllegalArgumentException("Multiple includes from archive: " + archivehandle);
			}
			subContents.add(archivecontents);
		}

		@Override
		public SakerFile build(String name) {
			if (files == null) {
				throw new IllegalStateException();
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

	protected static class ZipInclude {
		protected final IncludeResourceMapping resources;

		public ZipInclude(IncludeResourceMapping resources) {
			this.resources = resources;
		}
	}

	public static Builder builder() {
		return new BuilderImpl();
	}

	protected static BuilderImpl zipBuilderImpl() {
		return new BuilderImpl();
	}

	protected final NavigableMap<SakerPath, FileHandle> files;
	protected final Map<FileHandle, ZipInclude> includes;
	protected final ZipFileContentDescriptor contentDescriptor;

	protected ZipOutputSakerFile(String name, BuilderImpl builder)
			throws NullPointerException, InvalidPathFormatException {
		super(name);
		ZipFileContentDescriptor contentdescriptor = new ZipFileContentDescriptor(
				builder.getDefaultEntryModificationTime(), MultiContentDescriptor.create(builder.subContents),
				builder.transformers);
		this.files = builder.files;
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

		public AbstractTransformationContext(ZipOutputStream zipOut) {
			this.zipOut = zipOut;
		}

		public abstract void transform(SakerPath entrypath, FileHandle handle, FileTime modtime) throws IOException;

		public abstract void transform(SakerPath entrypath, UnsyncByteArrayOutputStream contentbuffer, FileTime modtime)
				throws IOException;

		public abstract void transform(SakerPath entrypath, InputStream input, FileTime modtime) throws IOException;

		public abstract void transformDirectory(SakerPath entrypath, FileTime modtime) throws IOException;
	}

	private static class NonTransformationContext extends AbstractTransformationContext {
		private final NavigableMap<SakerPath, Boolean> entries = new TreeMap<>(SakerPath::compareToIgnoreCase);

		public NonTransformationContext(ZipOutputStream zipOut) {
			super(zipOut);
		}

		@Override
		public void transform(SakerPath entrypath, UnsyncByteArrayOutputStream contentbuffer, FileTime modtime)
				throws IOException {
			checkEntryFileDuplication(entrypath);
			ZipEntry ze = new ZipEntry(entrypath.toString());
			ze.setLastModifiedTime(modtime);
			zipOut.putNextEntry(ze);
			contentbuffer.writeTo(zipOut);
			zipOut.closeEntry();
		}

		@Override
		public void transform(SakerPath entrypath, InputStream input, FileTime modtime) throws IOException {
			checkEntryFileDuplication(entrypath);
			ZipEntry ze = new ZipEntry(entrypath.toString());
			ze.setLastModifiedTime(modtime);
			zipOut.putNextEntry(ze);
			StreamUtils.copyStream(input, zipOut, buffer.getBuffer());
			zipOut.closeEntry();
		}

		@Override
		public void transform(SakerPath entrypath, FileHandle handle, FileTime modtime) throws IOException {
			checkEntryFileDuplication(entrypath);
			ZipEntry ze = new ZipEntry(entrypath.toString());
			ze.setLastModifiedTime(modtime);
			zipOut.putNextEntry(ze);
			handle.writeTo(zipOut);
			zipOut.closeEntry();
		}

		@Override
		public void transformDirectory(SakerPath entrypath, FileTime modtime) throws IOException {
			checkEntryDirectoryDuplication(entrypath);
			ZipEntry addentry = new ZipEntry(entrypath + "/");
			addentry.setLastModifiedTime(modtime);
			zipOut.putNextEntry(addentry);
			zipOut.closeEntry();
		}

		private void checkEntryFileDuplication(SakerPath entrypath) {
			if (entries.putIfAbsent(entrypath, Boolean.FALSE) != null) {
				throw new IllegalArgumentException("Duplicate zip file entry: " + entrypath);
			}
		}

		private void checkEntryDirectoryDuplication(SakerPath entrypath) {
			Boolean prev = entries.putIfAbsent(entrypath, Boolean.TRUE);
			if (prev == Boolean.FALSE) {
				//already present as a file
				throw new IllegalArgumentException("Zip file entry already exists for directory: " + entrypath);
			}
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
			protected final SakerPath path;
			/**
			 * The bytes of the entry.
			 * <p>
			 * <code>null</code> if represents a directory.
			 */
			protected final ByteArrayRegion bytes;
			protected final FileTime modificationTime;

			public PendingResource(SakerPath path, ByteArrayRegion bytes, FileTime modificationTime) {
				this.path = path;
				this.bytes = bytes;
				this.modificationTime = modificationTime;
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
			Objects.requireNonNull(entrypath, "entry path");
			PendingResource pendingres = new PendingResource(entrypath, null,
					modificationtime == null ? defaultModificationTime : modificationtime);
			pendingResources.add(pendingres);
		}

		@Override
		public OutputStream appendFile(SakerPath entrypath, FileTime modificationtime) {
			Objects.requireNonNull(entrypath, "entry path");
			return new UnsyncByteArrayOutputStream() {
				private boolean closed = false;

				@Override
				public void close() {
					//guard to avoid adding the resource multiple times
					if (closed) {
						return;
					}
					closed = true;
					PendingResource pendingres = new PendingResource(entrypath, toByteArrayRegion(),
							modificationtime == null ? defaultModificationTime : modificationtime);
					pendingResources.add(pendingres);
					super.close();
				}
			};
		}

		@Override
		public void transform(SakerPath entrypath, UnsyncByteArrayOutputStream contentbuffer, FileTime modtime)
				throws IOException {
			try (UnsyncByteArrayInputStream is = new UnsyncByteArrayInputStream(contentbuffer.toByteArrayRegion())) {
				transform(entrypath, is, modtime);
			}
		}

		@Override
		public void transform(SakerPath entrypath, FileHandle handle, FileTime modtime) throws IOException {
			try (InputStream is = handle.openInputStream()) {
				transform(entrypath, is, modtime);
			}
		}

		@Override
		public void transformDirectory(SakerPath entrypath, FileTime modtime) throws IOException {
			transform(entrypath, (InputStream) null, modtime);
		}

		@Override
		public void transform(SakerPath entrypath, InputStream input, FileTime modtime) throws IOException {
			transformSingleEntryImpl(entrypath, input, modtime);

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
				transformSingleEntryImpl(pendingres.path,
						pendingres.bytes == null ? null : new UnsyncByteArrayInputStream(pendingres.bytes),
						pendingres.modificationTime);
			}
		}

		private boolean callTransformationsSingleEntryImpl(SakerPath entrypath, InputStream input) throws IOException {
			if (transformers.isEmpty()) {
				return false;
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
				boolean consumed = transformer.transformer.process(this, entrypath, transformerin);
				if (consumed) {
					return true;
				}
			}
			return false;
		}

		private void transformSingleEntryImpl(SakerPath entrypath, InputStream input, FileTime modtime)
				throws IOException {
			if (callTransformationsSingleEntryImpl(entrypath, input)) {
				//the entry was consumed
				return;
			}

			checkEntryDuplication(entrypath);
			//the input was not consumed
			ZipEntry ze = new ZipEntry(input == null ? entrypath.toString() + "/" : entrypath.toString());
			ze.setLastModifiedTime(modtime == null ? defaultModificationTime : modtime);
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
		FileTime defaultmodificationtime = getDefaultModificationTime();
		try (TransformationContextImpl context = new TransformationContextImpl(zipos, transformers,
				defaultmodificationtime)) {
			writeZipFiles(defaultmodificationtime, context);
			writeZipIncludes(context);
		}
	}

	private void writeNonTransformingZip(ZipOutputStream zipos) throws IOException {
		NonTransformationContext context = new NonTransformationContext(zipos);
		writeZipFiles(getDefaultModificationTime(), context);
		writeZipIncludes(context);
	}

	private void writeZipFiles(FileTime defaultmodtime, AbstractTransformationContext context) throws IOException {
		if (files.isEmpty()) {
			return;
		}
		for (Entry<SakerPath, ? extends FileHandle> entry : files.entrySet()) {
			FileHandle handle = entry.getValue();
			if (handle == null) {
				context.transformDirectory(entry.getKey(), defaultmodtime);
			} else {
				context.transform(entry.getKey(), handle, defaultmodtime);
			}
		}
	}

	private void writeZipIncludes(AbstractTransformationContext context) throws IOException {
		if (includes.isEmpty()) {
			return;
		}
		NavigableSet<SakerPath> multientryaddpaths = new TreeSet<>();
		for (Entry<FileHandle, ZipInclude> incentry : includes.entrySet()) {
			FileHandle handle = incentry.getKey();
			ZipInclude inc = incentry.getValue();
			try (InputStream archivein = handle.openInputStream();
					ZipInputStream zis = new ZipInputStream(archivein)) {
				for (ZipEntry ze; (ze = zis.getNextEntry()) != null;) {
					String name = ze.getName();
					boolean directory = ze.isDirectory();
					FileTime lastmodifiedtime = ze.getLastModifiedTime();

					multientryaddpaths.clear();
					SakerPath path = SakerPath.valueOf(name);

					IncludeResourceMapping mapping = inc.resources;
					Set<SakerPath> addentrypaths = mapping.mapResourcePath(path, directory);
					if (ObjectUtils.isNullOrEmpty(addentrypaths)) {
						//don't include
						continue;
					}
					if (directory) {
						for (SakerPath addentrypath : addentrypaths) {
							validateMappingResultPath(path, mapping, addentrypath);

							context.transformDirectory(addentrypath, lastmodifiedtime);
						}
					} else {
						for (SakerPath addentrypath : addentrypaths) {
							validateMappingResultPath(path, mapping, addentrypath);

							multientryaddpaths.add(addentrypath);
						}
					}

					if (!multientryaddpaths.isEmpty()) {
						if (multientryaddpaths.size() > 1) {
							if (directory) {
								for (SakerPath addentrypath : multientryaddpaths) {
									context.transformDirectory(addentrypath, lastmodifiedtime);
								}
							} else {
								UnsyncByteArrayOutputStream bytebuffer = context.buffer;
								bytebuffer.reset();
								bytebuffer.readFrom(zis);
								for (SakerPath addentrypath : multientryaddpaths) {
									context.transform(addentrypath, bytebuffer, lastmodifiedtime);
								}
							}
						} else {
							SakerPath addentrypath = multientryaddpaths.pollFirst();
							if (directory) {
								context.transformDirectory(addentrypath, lastmodifiedtime);
							} else {
								context.transform(addentrypath, zis, lastmodifiedtime);
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

	protected static class ZipFileContentDescriptor implements ContentDescriptor, Externalizable {
		private static final long serialVersionUID = 1L;

		protected FileTime defaultEntryModificationTime;
		protected ContentDescriptor subContents;
		protected List<ZipResourceTransformerFactory> transformers;

		/**
		 * For {@link Externalizable}.
		 */
		public ZipFileContentDescriptor() {
		}

		public ZipFileContentDescriptor(FileTime defaultEntryModificationTime, ContentDescriptor subContents,
				List<ZipResourceTransformerFactory> transformers) {
			this.defaultEntryModificationTime = defaultEntryModificationTime;
			this.subContents = subContents;
			this.transformers = transformers;
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			out.writeLong(defaultEntryModificationTime.toMillis());
			out.writeObject(subContents);
			SerialUtils.writeExternalCollection(out, transformers);
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			defaultEntryModificationTime = FileTime.fromMillis(in.readLong());
			subContents = (ContentDescriptor) in.readObject();
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
			if (transformers == null) {
				if (other.transformers != null)
					return false;
			} else if (!transformers.equals(other.transformers))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "["
					+ (defaultEntryModificationTime != null
							? "defaultEntryModificationTime=" + defaultEntryModificationTime + ", "
							: "")
					+ (subContents != null ? "subContents=" + subContents + ", " : "")
					+ (transformers != null ? "transformers=" + transformers : "") + "]";
		}
	}

	private static class EntryPathContentDescriptor implements ContentDescriptor, Externalizable {
		private static final long serialVersionUID = 1L;

		private SakerPath path;
		private ContentDescriptor content;

		public EntryPathContentDescriptor() {
		}

		public EntryPathContentDescriptor(SakerPath path, ContentDescriptor content) {
			this.path = path;
			this.content = content;
		}

		@Override
		public boolean isChanged(ContentDescriptor content) {
			if (!ObjectUtils.isSameClass(this, content)) {
				return true;
			}
			EntryPathContentDescriptor o = (EntryPathContentDescriptor) content;
			if (!Objects.equals(path, o.path)) {
				return true;
			}
			return this.content.isChanged(o.content);
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			out.writeObject(path);
			out.writeObject(content);
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			path = (SakerPath) in.readObject();
			content = (ContentDescriptor) in.readObject();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((content == null) ? 0 : content.hashCode());
			result = prime * result + ((path == null) ? 0 : path.hashCode());
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
			EntryPathContentDescriptor other = (EntryPathContentDescriptor) obj;
			if (content == null) {
				if (other.content != null)
					return false;
			} else if (!content.equals(other.content))
				return false;
			if (path == null) {
				if (other.path != null)
					return false;
			} else if (!path.equals(other.path))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "[" + (path != null ? "path=" + path + ", " : "")
					+ (content != null ? "content=" + content : "") + "]";
		}
	}
}
