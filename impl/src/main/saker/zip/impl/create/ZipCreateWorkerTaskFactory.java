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

import java.io.Externalizable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import saker.build.file.FileHandle;
import saker.build.file.SakerDirectory;
import saker.build.file.SakerFile;
import saker.build.file.content.ContentDescriptor;
import saker.build.file.path.SakerPath;
import saker.build.runtime.execution.ExecutionContext;
import saker.build.task.CommonTaskContentDescriptors;
import saker.build.task.Task;
import saker.build.task.TaskContext;
import saker.build.task.TaskExecutionUtilities;
import saker.build.task.TaskFactory;
import saker.build.task.utils.dependencies.EqualityTaskOutputChangeDetector;
import saker.std.api.file.location.ExecutionFileLocation;
import saker.std.api.file.location.FileLocationVisitor;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.io.SerialUtils;
import saker.zip.api.create.IncludeResourceMapping;
import saker.zip.api.create.ZipCreatorTaskOutput;
import saker.zip.api.create.ZipResourceTransformerFactory;
import saker.zip.impl.create.options.MultiIncludeResourceMapping;
import saker.zip.impl.create.options.ZipIncludeArchiveOption;
import saker.zip.impl.create.options.ZipResourceOption;

public class ZipCreateWorkerTaskFactory
		implements TaskFactory<ZipCreatorTaskOutput>, Task<ZipCreatorTaskOutput>, Externalizable {
	private static final long serialVersionUID = 1L;

	protected SakerPath outputPath;
	protected Date modificationTime;
	protected Set<ZipResourceOption> resourceOptions;
	protected Set<ZipIncludeArchiveOption> includeOptions;
	protected List<ZipResourceTransformerFactory> resourceTransformers;

	/**
	 * For {@link Externalizable}.
	 */
	public ZipCreateWorkerTaskFactory() {
	}

	public ZipCreateWorkerTaskFactory(SakerPath outputPath) {
		this.outputPath = outputPath;
	}

	public void setOutputPath(SakerPath outputPath) {
		this.outputPath = outputPath;
	}

	public void setModificationTime(Date modificationTime) {
		this.modificationTime = modificationTime;
	}

	public void setResourceOptions(Set<? extends ZipResourceOption> resourceOptions) {
		this.resourceOptions = ImmutableUtils.makeImmutableLinkedHashSet(resourceOptions);
	}

	public void setIncludeOptions(Set<? extends ZipIncludeArchiveOption> includeOptions) {
		this.includeOptions = ImmutableUtils.makeImmutableLinkedHashSet(includeOptions);
	}

	public void setResourceTransformers(List<? extends ZipResourceTransformerFactory> resourceTransformers) {
		this.resourceTransformers = ImmutableUtils.makeImmutableList(resourceTransformers);
	}

	@Override
	public ZipCreatorTaskOutput run(TaskContext taskcontext) throws Exception {
		TaskExecutionUtilities taskutils = taskcontext.getTaskUtilities();
		SakerDirectory outparentdir = taskutils.resolveDirectoryAtPathCreateIfAbsent(outputPath.getParent());
		if (outparentdir == null) {
			throw new IllegalArgumentException("Failed to create directory for output: " + outputPath);
		}
		ZipOutputSakerFile.Builder builder = ZipOutputSakerFile.builder();
		builder.setDefaultEntryModificationTime(
				modificationTime == null ? null : FileTime.fromMillis(modificationTime.getTime()));

		if (!ObjectUtils.isNullOrEmpty(resourceOptions)) {
			//XXX create bulk file resolution methods in task utilities
			for (ZipResourceOption resoption : resourceOptions) {
				resoption.getFileLocation().accept(new FileLocationVisitor() {
					@Override
					public void visit(ExecutionFileLocation loc) {
						SakerPath filepath = loc.getPath();
						SakerFile file = taskutils.resolveAtAbsolutePath(filepath);
						if (file == null) {
							taskcontext.reportInputFileDependency(null, filepath,
									CommonTaskContentDescriptors.NOT_PRESENT);
							//XXX abort instead
							throw ObjectUtils.sneakyThrow(
									new FileNotFoundException("File to include in archive not found: " + filepath));
						}
						ContentDescriptor contents = file.getContentDescriptor();
						taskcontext.reportInputFileDependency(null, filepath, contents);
						builder.add(resoption.getArchivePath(), file instanceof SakerDirectory ? null : file, contents);
					}
				});
			}
		}
		if (!ObjectUtils.isNullOrEmpty(includeOptions)) {
			Map<FileHandle, IncludeInfo> includes = new LinkedHashMap<>();
			for (ZipIncludeArchiveOption incoption : includeOptions) {
				incoption.getArchive().accept(new FileLocationVisitor() {
					@Override
					public void visit(ExecutionFileLocation loc) {
						SakerPath filepath = loc.getPath();
						SakerFile file = taskutils.resolveFileAtAbsolutePath(filepath);
						if (file == null) {
							taskcontext.reportInputFileDependency(null, filepath,
									CommonTaskContentDescriptors.IS_NOT_FILE);
							//XXX abort instead
							throw ObjectUtils
									.sneakyThrow(new FileNotFoundException("Include archive not found: " + filepath));
						}
						ContentDescriptor contents = file.getContentDescriptor();
						taskcontext.reportInputFileDependency(null, filepath, contents);

						IncludeResourceMapping mapping = incoption.getMapping();
						includes.compute(file, (f, info) -> {
							if (info == null) {
								return new IncludeInfo(contents, mapping);
							}
							if (info.resourceMapping instanceof MultiIncludeResourceMapping) {
								MultiIncludeResourceMapping multiprev = (MultiIncludeResourceMapping) info.resourceMapping;
								List<IncludeResourceMapping> prevmappings = ObjectUtils
										.newArrayList(multiprev.getMappings());
								prevmappings.add(mapping);
								info.resourceMapping = MultiIncludeResourceMapping.create(prevmappings);
							} else {
								info.resourceMapping = MultiIncludeResourceMapping.create(info.resourceMapping,
										mapping);
							}
							return info;
						});
					}
				});
			}
			for (Entry<FileHandle, IncludeInfo> entry : includes.entrySet()) {
				IncludeInfo info = entry.getValue();
				builder.addIncludeFromArchive(entry.getKey(), info.contentDescriptor, info.resourceMapping);
			}
		}
		if (!ObjectUtils.isNullOrEmpty(resourceTransformers)) {
			for (ZipResourceTransformerFactory transformer : resourceTransformers) {
				builder.addResourceTransformer(transformer);
			}
		}

		SakerFile file = builder.build(outputPath.getFileName());
		outparentdir.add(file);
		file.synchronize();
		taskutils.reportOutputFileDependency(null, file);

		SimpleZipCreatorTaskOutput result = new SimpleZipCreatorTaskOutput(file.getSakerPath());
		taskcontext.reportSelfTaskOutputChangeDetector(new EqualityTaskOutputChangeDetector(result));
		return result;
	}

	private static class IncludeInfo {
		protected ContentDescriptor contentDescriptor;
		protected IncludeResourceMapping resourceMapping;

		public IncludeInfo(ContentDescriptor contentDescriptor, IncludeResourceMapping resourceMapping) {
			this.contentDescriptor = contentDescriptor;
			this.resourceMapping = resourceMapping;
		}

		@Override
		public String toString() {
			return "IncludeInfo[" + (contentDescriptor != null ? "contentDescriptor=" + contentDescriptor + ", " : "")
					+ (resourceMapping != null ? "resourceMapping=" + resourceMapping : "") + "]";
		}

	}

	@Override
	public final Task<? extends ZipCreatorTaskOutput> createTask(ExecutionContext executioncontext) {
		return this;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(outputPath);
		out.writeObject(modificationTime);
		SerialUtils.writeExternalCollection(out, includeOptions);
		SerialUtils.writeExternalCollection(out, resourceOptions);
		SerialUtils.writeExternalCollection(out, resourceTransformers);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		outputPath = (SakerPath) in.readObject();
		modificationTime = (Date) in.readObject();
		includeOptions = SerialUtils.readExternalImmutableLinkedHashSet(in);
		resourceOptions = SerialUtils.readExternalImmutableLinkedHashSet(in);
		resourceTransformers = SerialUtils.readExternalImmutableList(in);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((includeOptions == null) ? 0 : includeOptions.hashCode());
		result = prime * result + ((modificationTime == null) ? 0 : modificationTime.hashCode());
		result = prime * result + ((outputPath == null) ? 0 : outputPath.hashCode());
		result = prime * result + ((resourceOptions == null) ? 0 : resourceOptions.hashCode());
		result = prime * result + ((resourceTransformers == null) ? 0 : resourceTransformers.hashCode());
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
		ZipCreateWorkerTaskFactory other = (ZipCreateWorkerTaskFactory) obj;
		if (includeOptions == null) {
			if (other.includeOptions != null)
				return false;
		} else if (!includeOptions.equals(other.includeOptions))
			return false;
		if (modificationTime == null) {
			if (other.modificationTime != null)
				return false;
		} else if (!modificationTime.equals(other.modificationTime))
			return false;
		if (outputPath == null) {
			if (other.outputPath != null)
				return false;
		} else if (!outputPath.equals(other.outputPath))
			return false;
		if (resourceOptions == null) {
			if (other.resourceOptions != null)
				return false;
		} else if (!resourceOptions.equals(other.resourceOptions))
			return false;
		if (resourceTransformers == null) {
			if (other.resourceTransformers != null)
				return false;
		} else if (!resourceTransformers.equals(other.resourceTransformers))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + (outputPath != null ? "outputPath=" + outputPath + ", " : "")
				+ (modificationTime != null ? "modificationTime=" + modificationTime + ", " : "")
				+ (resourceOptions != null ? "resourceOptions=" + resourceOptions + ", " : "")
				+ (includeOptions != null ? "includeOptions=" + includeOptions + ", " : "")
				+ (resourceTransformers != null ? "resourceTransformers=" + resourceTransformers : "") + "]";
	}

}