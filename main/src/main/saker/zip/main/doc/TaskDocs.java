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
package saker.zip.main.doc;

import saker.build.file.path.SakerPath;
import saker.build.scripting.model.info.TypeInformationKind;
import saker.nest.scriptinfo.reflection.annot.NestFieldInformation;
import saker.nest.scriptinfo.reflection.annot.NestInformation;
import saker.nest.scriptinfo.reflection.annot.NestTypeInformation;
import saker.nest.scriptinfo.reflection.annot.NestTypeUsage;
import saker.zip.main.create.ZipCreateTaskFactory;

public class TaskDocs {
	public static final String ZIP_CREATE_OUTPUT = "Specifies the output path of the created archive.\n"
			+ "The path must be forward relative, and the task will write the archive under the "
			+ ZipCreateTaskFactory.TASK_NAME + " subdirectory of the build directory.\n"
			+ "If not specified, the archive is created with the \"output.zip\" name.";
	public static final String ZIP_CREATE_RESOURCES = "Specifies one or more resources which should be added to the created archive.\n"
			+ "The option accepts simple paths, file locations, file collections or complex configuration.\n"
			+ "The path that a resource is added with to the archive is determined by the type of the configuration.";
	public static final String ZIP_CREATE_INCLUDES = "Specifies one or more archive entry inclusions which should be added to the output archive.\n"
			+ "The parameter is used to include entries from other ZIP archives into the output.\n"
			+ "The option accepts simple paths, file locations, file collections or complex configuration.";
	public static final String ZIP_CREATE_TRANSFORMERS = "Specifies one or more ZIP resource transformers that should be used when constructing the output.\n"
			+ "ZIP resource transformers are plugins into the archive creating operation. "
			+ "They can customize the archive output in an implementation dependent manner.\n"
			+ "In order to add transformers to the task, you need to implement your own Transformer with the "
			+ "ZIP creator task API, or use an existing implementations. Consult the use-case documentation "
			+ "of your transformer of interest to find out how to include it in the task.";
	public static final String ZIP_CREATE_MODIFICATION_TIME = "Specifies the default modification time of the entries that are placed in the output archive.\n"
			+ "The archive entries which are specified using the Resources parameter will have their modification time "
			+ "set to the date specified using this parameter. This is in order to have deterministic binary outputs of the archives.\n"
			+ "If not specified, the epoch date (1970-01-01T00:00:00Z) is used.\n"
			+ "Note that this parameter doesn't apply to entries included using the Include parameter. Those entries "
			+ "have the same modification time as in the source Archive.\n"
			+ "The format of this parameter should be any of the following:\n" + "yyyy-MM-dd HH:mm:ss.SSS\n"
			+ "yyyy-MM-dd HH:mm:ss\n" + "yyyy-MM-dd HH:mm\n" + "yyyy-MM-dd\n" + "yyyy.MM.dd\n"
			+ "Where the pattern letters are interpreted the same way as SimpleDateFormat: "
			+ "https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html";

	@NestInformation("Represents the task output of a ZIP archive creation.\n"
			+ "Provides access to the output Path of the created archive.")
	@NestFieldInformation(value = "Path",
			type = @NestTypeUsage(kind = TypeInformationKind.FILE_PATH, value = SakerPath.class),
			info = @NestInformation("The path to the created ZIP archive."))
	@NestTypeInformation(qualifiedName = "ZipCreatorTaskOutput")
	public static class DocZipCreatorTaskOutput {
	}
}
