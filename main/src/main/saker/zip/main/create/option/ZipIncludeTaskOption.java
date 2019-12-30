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
import saker.zip.api.create.IncludeResourceMapping;
import saker.zip.api.create.ZipCreationTaskBuilder;

@NestInformation("Represents an archive entry inclusion for ZIP archive creation.\n"
		+ "Accepts simple paths, file locations, file collections or may be configured directly.\n"
		+ "The options is used to include the specified entries from an Archive and add them to the output ZIP archive.\n"
		+ "Use the Resources field to specify the entries to include, and the TargetDirectory to relocate the entries in the output.")
@NestFieldInformation(value = "Archive",
		type = @NestTypeUsage(value = Collection.class, elementTypes = MultiFileLocationTaskOption.class),
		info = @NestInformation("Specifies one or more source ZIP archives from where the entries should be included.\n"
				+ "The archives can be specified using simple paths, wildcards, file locations or file collections.\n"
				+ "For each given archive, the specified Resources will be included and added to the result archive under the "
				+ "given TargetDirectory."))
@NestFieldInformation(value = "Resources",
		type = @NestTypeUsage(value = Collection.class, elementTypes = WildcardPath.class),
		info = @NestInformation("Specifies one or more wildcard patterns which are used to select the input entries that should be "
				+ "included in the output.\n"
				+ "Each entry in the source Archive will be checked if any of the specified resource wildcards matches its name. If so, "
				+ "then it will be included in the output ZIP with the archive path that is the concatenation of TargetDirectory and the "
				+ "path in the source Archive.\n"
				+ "If none of the wildcards match an input entry name, it won't be added to the output.\n"
				+ "If no wildcard patterns are specified, all entries will be included from the source Archive."))
@NestFieldInformation(value = "TargetDirectory",
		type = @NestTypeUsage(SakerPath.class),
		info = @NestInformation("Specifies the target directory under which the entries specified should be placed.\n"
				+ "Any input entry that is matched by this configuration will have their archive paths prepended by the value of this field.\n"
				+ "The specified path must be forward relative. By default, no target directory is used."))
public interface ZipIncludeTaskOption {
	public default ZipIncludeTaskOption clone() {
		return new SimpleZipIncludeTaskOption(this);
	}

	public default Collection<MultiFileLocationTaskOption> getArchive() {
		return null;
	}

	public default Collection<WildcardPath> getResources() {
		return null;
	}

	public default SakerPath getTargetDirectory() {
		return null;
	}

	public void addTo(TaskContext taskcontext, ZipCreationTaskBuilder taskbuilder);

	public static ZipIncludeTaskOption valueOf(String archivepath) {
		return valueOf(SakerPath.valueOf(archivepath));
	}

	public static ZipIncludeTaskOption valueOf(SakerPath archivepath) {
		return new ZipIncludeTaskOption() {
			@Override
			public ZipIncludeTaskOption clone() {
				return this;
			}

			@Override
			public void addTo(TaskContext taskcontext, ZipCreationTaskBuilder taskbuilder) {
				taskbuilder.addIncludeArchive(
						ExecutionFileLocation.create(taskcontext.getTaskWorkingDirectoryPath().tryResolve(archivepath)),
						IncludeResourceMapping.identity());
			}
		};
	}

	public static ZipIncludeTaskOption valueOf(FileLocation filelocation) {
		return new ZipIncludeTaskOption() {
			@Override
			public ZipIncludeTaskOption clone() {
				return this;
			}

			@Override
			public void addTo(TaskContext taskcontext, ZipCreationTaskBuilder taskbuilder) {
				taskbuilder.addIncludeArchive(filelocation, IncludeResourceMapping.identity());
			}
		};
	}

	public static ZipIncludeTaskOption valueOf(FileCollection filecollection) {
		return new ZipIncludeTaskOption() {
			@Override
			public ZipIncludeTaskOption clone() {
				return this;
			}

			@Override
			public void addTo(TaskContext taskcontext, ZipCreationTaskBuilder taskbuilder) {
				for (FileLocation filelocation : filecollection) {
					taskbuilder.addIncludeArchive(filelocation, IncludeResourceMapping.identity());
				}
			}
		};
	}
}
