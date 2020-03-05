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
package saker.zip.main.create.option;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;

import saker.build.file.SakerDirectory;
import saker.build.file.SakerFile;
import saker.build.file.path.SakerPath;
import saker.build.file.path.WildcardPath;
import saker.build.file.provider.SakerPathFiles;
import saker.build.runtime.execution.SakerLog;
import saker.build.task.CommonTaskContentDescriptors;
import saker.build.task.TaskContext;
import saker.build.task.dependencies.FileCollectionStrategy;
import saker.build.task.utils.dependencies.WildcardFileCollectionStrategy;
import saker.std.api.file.location.ExecutionFileLocation;
import saker.std.api.file.location.FileLocation;
import saker.std.api.file.location.FileLocationVisitor;
import saker.std.api.util.SakerStandardUtils;
import saker.std.main.file.option.MultiFileLocationTaskOption;
import saker.std.main.file.utils.TaskOptionUtils;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.zip.api.create.ZipCreationTaskBuilder;

public class SimpleZipResourcesTaskOption implements ZipResourcesTaskOption {
	private Collection<MultiFileLocationTaskOption> directory;
	private Collection<WildcardPath> resources;
	private Collection<MultiFileLocationTaskOption> files;
	private SakerPath targetDirectory;

	public SimpleZipResourcesTaskOption(ZipResourcesTaskOption copy) {
		this.directory = ObjectUtils.cloneArrayList(copy.getDirectory(), MultiFileLocationTaskOption::clone);
		this.resources = ObjectUtils.cloneArrayList(copy.getResources());
		this.files = ObjectUtils.cloneArrayList(copy.getFiles(), MultiFileLocationTaskOption::clone);
		this.targetDirectory = copy.getTargetDirectory();
	}

	@Override
	public ZipResourcesTaskOption clone() {
		return this;
	}

	@Override
	public Collection<MultiFileLocationTaskOption> getDirectory() {
		return directory;
	}

	@Override
	public Collection<WildcardPath> getResources() {
		return resources;
	}

	@Override
	public Collection<MultiFileLocationTaskOption> getFiles() {
		return files;
	}

	@Override
	public SakerPath getTargetDirectory() {
		return targetDirectory;
	}

	@Override
	public void addTo(TaskContext taskcontext, ZipCreationTaskBuilder taskbuilder) {
		SakerPath targetdir = ObjectUtils.nullDefault(getTargetDirectory(), SakerPath.EMPTY);
		if (!targetdir.isForwardRelative()) {
			throw new IllegalArgumentException("TargetDirectory must be a forward relative path: " + targetdir);
		}

		Collection<WildcardPath> resources = getResources();
		Collection<MultiFileLocationTaskOption> files = getFiles();
		Collection<MultiFileLocationTaskOption> dir = getDirectory();
		if (files != null) {
			if (resources != null || dir != null) {
				throw new IllegalArgumentException("Conflicting options: Files with Resources and Directory.");
			}
			for (MultiFileLocationTaskOption flocopt : files) {
				if (flocopt == null) {
					continue;
				}
				for (FileLocation flocation : TaskOptionUtils.toFileLocations(flocopt, taskcontext,
						TaskTags.TASK_INPUT_FILE)) {
					taskbuilder.addResource(flocation, targetdir.resolve(SakerStandardUtils.getFileLocationFileName(flocation)));
				}
			}
		} else {
			if (!ObjectUtils.hasNonNull(resources)) {
				SakerLog.warning().taskScriptPosition(taskcontext)
						.println("No Resources specified for archive creation.");
				return;
			}
			if (!ObjectUtils.hasNonNull(dir)) {
				dir = ImmutableUtils.singletonSet(MultiFileLocationTaskOption
						.valueOf(ExecutionFileLocation.create(taskcontext.getTaskWorkingDirectoryPath())));
			}
			for (MultiFileLocationTaskOption diropt : dir) {
				if (diropt == null) {
					continue;
				}
				for (FileLocation floc : TaskOptionUtils.toFileLocations(diropt, taskcontext,
						TaskTags.TASK_INPUT_FILE)) {
					floc.accept(new FileLocationVisitor() {
						@Override
						public void visit(ExecutionFileLocation loc) {
							Collection<FileCollectionStrategy> collectionstrats = new HashSet<>();
							SakerPath dirpath = loc.getPath();
							for (WildcardPath wc : resources) {
								collectionstrats.add(WildcardFileCollectionStrategy.create(dirpath, wc));
							}
							NavigableMap<SakerPath, SakerFile> files = new TreeMap<>(taskcontext.getTaskUtilities()
									.collectFilesReportAdditionDependency(null, collectionstrats));
							//filter out directories
							NavigableSet<SakerPath> dirpaths = new TreeSet<>();
							for (Iterator<Entry<SakerPath, SakerFile>> it = files.entrySet().iterator(); it
									.hasNext();) {
								Entry<SakerPath, SakerFile> entry = it.next();
								SakerFile f = entry.getValue();
								if (f instanceof SakerDirectory) {
									dirpaths.add(entry.getKey());
									it.remove();
								}
							}
							NavigableSet<SakerPath> filepaths = files.navigableKeySet();
							if (!SakerPathFiles.isAllSubPath(filepaths, dirpath)) {
								throw new IllegalArgumentException("Resources wildcard matched files not in directory: "
										+ dirpath + " with " + filepaths);
							}

							if (!dirpaths.isEmpty()) {
								//report the directories as well otherwise it could case reinvocation of the task
								taskcontext.getTaskUtilities().reportInputFileDependency(null, ObjectUtils
										.singleValueMap(dirpaths, CommonTaskContentDescriptors.IS_DIRECTORY));
							}
							taskcontext.getTaskUtilities().reportInputFileDependency(null,
									ObjectUtils.singleValueMap(filepaths, CommonTaskContentDescriptors.PRESENT));
							for (SakerPath fp : filepaths) {
								taskbuilder.addResource(ExecutionFileLocation.create(fp),
										targetdir.resolve(dirpath.relativize(fp)));
							}
						}
					});
				}
			}
		}
	}

}
