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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import saker.build.file.path.SakerPath;
import saker.build.file.path.WildcardPath;
import saker.build.runtime.execution.SakerLog;
import saker.build.task.TaskContext;
import saker.std.api.file.location.FileLocation;
import saker.std.main.file.option.MultiFileLocationTaskOption;
import saker.std.main.file.utils.TaskOptionUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.zip.api.create.IncludeResourceMapping;
import saker.zip.api.create.ZipCreationTaskBuilder;

public class SimpleZipIncludeTaskOption implements ZipIncludeTaskOption {
	private Collection<MultiFileLocationTaskOption> archive;
	private Collection<WildcardPath> resources;
	private SakerPath targetDirectory;

	public SimpleZipIncludeTaskOption(ZipIncludeTaskOption copy) {
		this.archive = ObjectUtils.cloneArrayList(copy.getArchive(), MultiFileLocationTaskOption::clone);
		this.resources = ObjectUtils.cloneLinkedHashSet(copy.getResources());
		this.targetDirectory = copy.getTargetDirectory();
	}

	@Override
	public Collection<MultiFileLocationTaskOption> getArchive() {
		return archive;
	}

	@Override
	public Collection<WildcardPath> getResources() {
		return resources;
	}

	@Override
	public SakerPath getTargetDirectory() {
		return targetDirectory;
	}

	@Override
	public ZipIncludeTaskOption clone() {
		return this;
	}

	@Override
	public void addTo(TaskContext taskcontext, ZipCreationTaskBuilder taskbuilder) {
		Collection<MultiFileLocationTaskOption> archives = getArchive();
		if (!ObjectUtils.hasNonNull(archives)) {
			SakerLog.warning().taskScriptPosition(taskcontext).println("No Archive specified for resource inclusion.");
			return;
		}

		IncludeResourceMapping wildcardmapping;
		Collection<WildcardPath> resources = getResources();
		if (resources == null) {
			//include all resources from the archive if none was specified
			wildcardmapping = null;
		} else {
			List<IncludeResourceMapping> wildcardfilters = new ArrayList<>();
			for (WildcardPath reswc : resources) {
				if (reswc == null) {
					continue;
				}
				wildcardfilters.add(IncludeResourceMapping.wildcardIncludeFilter(reswc));
			}
			wildcardmapping = IncludeResourceMapping.multi(wildcardfilters);
		}

		SakerPath targetdir = getTargetDirectory();
		IncludeResourceMapping targetdirmapping;
		if (targetdir == null) {
			targetdirmapping = null;
		} else {
			targetdirmapping = IncludeResourceMapping.targetDirectory(targetdir);
		}

		IncludeResourceMapping combinedmappingresult = IncludeResourceMapping.chain(wildcardmapping, targetdirmapping);
		for (MultiFileLocationTaskOption archivelocation : archives) {
			if (archivelocation == null) {
				continue;
			}
			for (FileLocation loc : TaskOptionUtils.toFileLocations(archivelocation, taskcontext,
					TaskTags.TASK_INPUT_FILE)) {
				taskbuilder.addIncludeArchive(loc, combinedmappingresult);
			}
		}
	}

}
