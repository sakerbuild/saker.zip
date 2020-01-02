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
package saker.zip.main.create;

import java.util.Collection;
import java.util.Date;

import saker.build.exception.InvalidPathFormatException;
import saker.build.file.path.SakerPath;
import saker.build.file.provider.SakerPathFiles;
import saker.build.runtime.execution.ExecutionContext;
import saker.build.scripting.model.info.TypeInformationKind;
import saker.build.task.ParameterizableTask;
import saker.build.task.TaskContext;
import saker.build.task.TaskFactory;
import saker.build.task.identifier.TaskIdentifier;
import saker.build.task.utils.SimpleStructuredObjectTaskResult;
import saker.build.task.utils.annot.DataContext;
import saker.build.task.utils.dependencies.EqualityTaskOutputChangeDetector;
import saker.nest.scriptinfo.reflection.annot.NestInformation;
import saker.nest.scriptinfo.reflection.annot.NestParameterInformation;
import saker.nest.scriptinfo.reflection.annot.NestTaskInformation;
import saker.nest.scriptinfo.reflection.annot.NestTypeUsage;
import saker.nest.utils.FrontendTaskFactory;
import saker.zip.api.create.ZipCreationTaskBuilder;
import saker.zip.main.create.option.ZipCreateDataContext;
import saker.zip.main.create.option.ZipIncludeTaskOption;
import saker.zip.main.create.option.ZipResourceTransformerTaskOption;
import saker.zip.main.create.option.ZipResourcesTaskOption;
import saker.zip.main.doc.TaskDocs;
import saker.zip.main.doc.TaskDocs.DocZipCreatorTaskOutput;

@NestTaskInformation(returnType = @NestTypeUsage(DocZipCreatorTaskOutput.class))
@NestInformation("Creates a ZIP archive with the specified contents.\n"
		+ "The task will create a ZIP archive based on the parameters passed to the task. "
		+ "The archive will be written to the specified location in the " + ZipCreateTaskFactory.TASK_NAME
		+ " subdirectory of the build directory.\n"
		+ "By default, the archive is constructed in a deterministic manner, meaning that it will not include timestamps of "
		+ "the current build time, and the entries are put in the archive in a deterministic order.")

@NestParameterInformation(value = "Output",
		type = @NestTypeUsage(SakerPath.class),
		info = @NestInformation(TaskDocs.ZIP_CREATE_OUTPUT))
@NestParameterInformation(value = "Resources",
		type = @NestTypeUsage(value = Collection.class, elementTypes = ZipResourcesTaskOption.class),
		info = @NestInformation(TaskDocs.ZIP_CREATE_RESOURCES))
@NestParameterInformation(value = "Includes",
		aliases = { "Include" },
		type = @NestTypeUsage(value = Collection.class, elementTypes = ZipIncludeTaskOption.class),
		info = @NestInformation(TaskDocs.ZIP_CREATE_INCLUDES))

@NestParameterInformation(value = "Transformers",
		aliases = { "Transformer" },
		type = @NestTypeUsage(value = Collection.class, elementTypes = ZipResourceTransformerTaskOption.class),
		info = @NestInformation(TaskDocs.ZIP_CREATE_TRANSFORMERS))

@NestParameterInformation(value = "ModificationTime",
		type = @NestTypeUsage(value = Date.class, kind = TypeInformationKind.LITERAL),
		info = @NestInformation(TaskDocs.ZIP_CREATE_MODIFICATION_TIME))
public class ZipCreateTaskFactory extends FrontendTaskFactory<Object> {
	private static final long serialVersionUID = 1L;

	public static final String TASK_NAME = "saker.zip.create";

	protected static class ZipCreateTaskImpl implements ParameterizableTask<Object> {
		private static final SakerPath DEFAULT_BUILD_SUBDIRECTORY_PATH = SakerPath.valueOf(TASK_NAME);
		private static final SakerPath DEFAULT_OUTPUT_PATH = SakerPath.valueOf("output.zip");

		@DataContext
		public ZipCreateDataContext zipDataContext;

		public ZipCreateTaskImpl() {
		}

		@Override
		public Object run(TaskContext taskcontext) throws Exception {
			ZipCreateDataContext data = this.zipDataContext.clone();
			SakerPath output = data.outputOption;
			if (output == null || output.equals(SakerPath.EMPTY)) {
				output = DEFAULT_OUTPUT_PATH;
			}
			if (!output.isForwardRelative()) {
				taskcontext.abortExecution(
						new InvalidPathFormatException("Zip output path must be forward relative: " + output));
				return null;
			}

			SakerPath builddirpath = SakerPathFiles.requireBuildDirectoryPath(taskcontext)
					.resolve(DEFAULT_BUILD_SUBDIRECTORY_PATH);

			SakerPath absoluteoutpath = builddirpath.resolve(output);

			ZipCreationTaskBuilder taskbuilder = ZipCreationTaskBuilder.newBuilder();
			taskbuilder.setOutputPath(absoluteoutpath);
			taskbuilder.setModificationTime(data.modificationTimeOption);

			data.addTo(taskcontext, taskbuilder);

			TaskFactory<?> workerfactory = taskbuilder.buildTaskFactory();
			TaskIdentifier taskid = taskbuilder.buildTaskIdentifier();

			taskcontext.startTask(taskid, workerfactory, null);

			SimpleStructuredObjectTaskResult result = new SimpleStructuredObjectTaskResult(taskid);
			taskcontext.reportSelfTaskOutputChangeDetector(new EqualityTaskOutputChangeDetector(result));
			return result;
		}
	}

	@Override
	public ParameterizableTask<? extends Object> createTask(ExecutionContext executioncontext) {
		return new ZipCreateTaskImpl();
	}

}
