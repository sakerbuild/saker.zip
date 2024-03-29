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
package saker.zip.api.create;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import saker.build.exception.InvalidPathFormatException;
import saker.build.file.path.SakerPath;
import saker.build.file.provider.SakerPathFiles;
import saker.build.task.TaskFactory;
import saker.build.task.identifier.TaskIdentifier;
import saker.std.api.file.location.FileLocation;
import saker.zip.impl.create.ZipCreateWorkerTaskFactory;
import saker.zip.impl.create.ZipCreateWorkerTaskIdentifier;
import saker.zip.impl.create.options.ZipIncludeArchiveOption;
import saker.zip.impl.create.options.ZipResourceOption;

final class ZipCreationTaskBuilderImpl implements ZipCreationTaskBuilder {
	protected SakerPath outputPath;
	protected Date modificationTime;
	protected Set<ZipIncludeArchiveOption> includeOptions = new LinkedHashSet<>();
	protected Set<ZipResourceOption> resourceOptions = new LinkedHashSet<>();
	protected List<ZipResourceTransformerFactory> resourceTransformers = new ArrayList<>();

	protected ZipCreationTaskBuilderImpl() {
	}

	@Override
	public void setOutputPath(SakerPath outputPath) {
		Objects.requireNonNull(outputPath, "output path");
		SakerPathFiles.requireAbsolutePath(outputPath);
		if (outputPath.getFileName() == null) {
			throw new InvalidPathFormatException("Output path has not file name: " + outputPath);
		}
		this.outputPath = outputPath;
	}

	@Override
	public void setModificationTime(Date modificationTime) {
		this.modificationTime = modificationTime;
	}

	@Override
	public void addIncludeArchive(FileLocation archive, IncludeResourceMapping mapping) {
		Objects.requireNonNull(archive, "archive");
		if (mapping == null) {
			mapping = IncludeResourceMapping.identity();
		}
		this.includeOptions.add(new ZipIncludeArchiveOption(archive, mapping));
	}

	@Override
	public void addResource(FileLocation location, SakerPath archivepath) {
		Objects.requireNonNull(location, "location");
		Objects.requireNonNull(archivepath, "archive path");
		if (!archivepath.isForwardRelative()) {
			throw new InvalidPathFormatException("Resource archive path must be forward relative: " + archivepath);
		}
		if (archivepath.getFileName() == null) {
			throw new InvalidPathFormatException("Resource archive path has no file name: " + archivepath);
		}
		this.resourceOptions.add(ZipResourceOption.create(location, archivepath));
	}

	@Override
	public void addResource(FileLocation location, ZipResourceEntry resourceentry)
			throws NullPointerException, InvalidPathFormatException {
		Objects.requireNonNull(location, "location");
		Objects.requireNonNull(resourceentry, "resource entry");

		SakerPath archivepath = resourceentry.getEntryPath();
		if (!archivepath.isForwardRelative()) {
			throw new InvalidPathFormatException("Resource archive path must be forward relative: " + archivepath);
		}
		if (archivepath.getFileName() == null) {
			throw new InvalidPathFormatException("Resource archive path has no file name: " + archivepath);
		}

		this.resourceOptions.add(ZipResourceOption.create(location, resourceentry));
	}

	@Override
	public void addResourceTransformer(ZipResourceTransformerFactory transformer) {
		Objects.requireNonNull(transformer, "zip resource transformer");
		this.resourceTransformers.add(transformer);
	}

	@Override
	public TaskIdentifier buildTaskIdentifier() {
		if (outputPath == null) {
			throw new IllegalStateException("No output path specified.");
		}
		return new ZipCreateWorkerTaskIdentifier(outputPath);
	}

	@Override
	public TaskFactory<?> buildTaskFactory() {
		if (outputPath == null) {
			throw new IllegalStateException("No output path specified.");
		}
		ZipCreateWorkerTaskFactory result = new ZipCreateWorkerTaskFactory(outputPath);
		result.setModificationTime(modificationTime);
		result.setResourceOptions(resourceOptions);
		result.setIncludeOptions(includeOptions);
		result.setResourceTransformers(resourceTransformers);
		return result;
	}
}
