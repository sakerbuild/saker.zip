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

import saker.build.file.path.SakerPath;
import saker.build.file.path.WildcardPath;
import saker.build.task.TaskContext;
import saker.nest.scriptinfo.reflection.annot.NestFieldInformation;
import saker.nest.scriptinfo.reflection.annot.NestInformation;
import saker.nest.scriptinfo.reflection.annot.NestTypeUsage;
import saker.std.api.file.location.ExecutionFileLocation;
import saker.std.api.file.location.FileCollection;
import saker.std.api.file.location.FileLocation;
import saker.std.main.file.option.MultiFileLocationTaskOption;
import saker.zip.api.create.ZipCreationTaskBuilder;

@NestInformation("Represents an resource configuration for ZIP archive creation.\n"
		+ "Accepts simple paths, file locations, file collections or may be configured directly.\n"
		+ "The target path of a resource is determined based on the way the option was configured.\n"
		+ "- If a simple file path or file location is specified, the resource is added to the archive with the file name of the path. "
		+ "E.g. if path/to/resource.txt is specified, it will have take resource.txt name in the archive.\n"
		+ "- In case of file collections, each file is added individually as a simple file.\n"
		+ "- In other cases, the TargetDirectory field and the relative path of each resource from the associated Directory is used "
		+ "to determine the archive path.")
@NestFieldInformation(value = "Directory",
		type = @NestTypeUsage(value = Collection.class, elementTypes = MultiFileLocationTaskOption.class),
		info = @NestInformation("Specifies one or more directory paths which are used as a base to find Resources to add to the archive.\n"
				+ "Each wildcard specified in the Resources field will be matched relative to the Directory paths specified.\n"
				+ "The archive path will be the concatenation of the TargetDirectory field, and the relative path from the "
				+ "Directory to each found Resource.\n"
				+ "If no directory is specified, the current working path of the task will be used.\n"
				+ "This option cannot be used with Files."))
@NestFieldInformation(value = "Resources",
		type = @NestTypeUsage(value = Collection.class, elementTypes = WildcardPath.class),
		info = @NestInformation("Wildcard patterns used to find resources relative to the specified Directories.\n"
				+ "The specified wildcard paths in this field will be used to match the input files under the directories specified "
				+ "in Directory. Each matched file will be included in the archive with the path that is the concatenation of TargetDirectory "
				+ "and the relative path from the associated Directory.\n"
				+ "The wildcards can be used to match both files and directories to add to the archive.\n"
				+ "This option cannot be used with Files."))

@NestFieldInformation(value = "Files",
		type = @NestTypeUsage(value = Collection.class, elementTypes = MultiFileLocationTaskOption.class),
		info = @NestInformation("Specifies one or more files that should be added to the archive.\n"
				+ "The files can be specified using simple paths, wildcards, file locations or file collections.\n"
				+ "The final archive path of each file will be the concatenation of the TargetDirectory and the file name of the "
				+ "specified files.\n" + "This option cannot be used with Directory and Resources."))
@NestFieldInformation(value = "TargetDirectory",
		type = @NestTypeUsage(SakerPath.class),
		info = @NestInformation("Specifies the target directory under which the entries specified in the configuration should be placed.\n"
				+ "Any input file that is matched by this configuration will have their archive paths prepended by the value of this field.\n"
				+ "The specified path must be forward relative. By default, no target directory is used."))
public interface ZipResourcesTaskOption {
	public default ZipResourcesTaskOption clone() {
		return new SimpleZipResourcesTaskOption(this);
	}

	public default Collection<MultiFileLocationTaskOption> getDirectory() {
		return null;
	}

	public default Collection<WildcardPath> getResources() {
		return null;
	}

	public default Collection<MultiFileLocationTaskOption> getFiles() {
		return null;
	}

	public default SakerPath getTargetDirectory() {
		return null;
	}

	public void addTo(TaskContext taskcontext, ZipCreationTaskBuilder taskbuilder);

	public static ZipResourcesTaskOption valueOf(FileCollection filecollection) {
		return new ZipResourcesTaskOption() {
			@Override
			public ZipResourcesTaskOption clone() {
				return this;
			}

			@Override
			public void addTo(TaskContext taskcontext, ZipCreationTaskBuilder taskbuilder) {
				for (FileLocation filelocation : filecollection) {
					FileLocationZipResourcesTaskOption.addFileLocationTo(taskcontext, taskbuilder, filelocation);
				}
			}
		};
	}

	public static ZipResourcesTaskOption valueOf(FileLocation filelocation) {
		return new FileLocationZipResourcesTaskOption(filelocation);
	}

	public static ZipResourcesTaskOption valueOf(SakerPath filepath) {
		return new ZipResourcesTaskOption() {
			@Override
			public ZipResourcesTaskOption clone() {
				return this;
			}

			@Override
			public void addTo(TaskContext taskcontext, ZipCreationTaskBuilder taskbuilder) {
				SakerPath abspath = taskcontext.getTaskWorkingDirectoryPath().tryResolve(filepath);
				taskbuilder.addResource(ExecutionFileLocation.create(abspath),
						SakerPath.valueOf(abspath.getFileName()));
			}
		};
	}

	public static ZipResourcesTaskOption valueOf(String filepath) {
		return valueOf(SakerPath.valueOf(filepath));
	}
}
