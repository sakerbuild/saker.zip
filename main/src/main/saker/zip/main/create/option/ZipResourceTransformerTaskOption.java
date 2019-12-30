package saker.zip.main.create.option;

import saker.build.task.TaskContext;
import saker.nest.scriptinfo.reflection.annot.NestInformation;
import saker.zip.api.create.ZipCreationTaskBuilder;
import saker.zip.api.create.ZipResourceTransformerFactory;
import saker.zip.main.create.ZipCreateTaskFactory;

@NestInformation("Represents a ZIP resource transformer that should be used when constructing ZIP entries.\n"
		+ "The transformers should be used with the " + ZipCreateTaskFactory.TASK_NAME + "() or related tasks. "
		+ "To add transformers to a ZIP creation, consult the use-case documentation of your transformer of interest.")
public interface ZipResourceTransformerTaskOption {
	public ZipResourceTransformerTaskOption clone();

	public void addTo(TaskContext taskcontext, ZipCreationTaskBuilder taskbuilder);

	public static ZipResourceTransformerTaskOption valueOf(ZipResourceTransformerFactory transformer) {
		return new ZipResourceTransformerTaskOption() {
			@Override
			public ZipResourceTransformerTaskOption clone() {
				return this;
			}

			@Override
			public void addTo(TaskContext taskcontext, ZipCreationTaskBuilder taskbuilder) {
				taskbuilder.addResourceTransformer(transformer);
			}
		};
	}
}
