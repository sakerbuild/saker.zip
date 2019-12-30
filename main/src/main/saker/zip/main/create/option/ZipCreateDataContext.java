package saker.zip.main.create.option;

import java.util.Collection;
import java.util.Date;

import saker.build.file.path.SakerPath;
import saker.build.task.TaskContext;
import saker.build.task.utils.annot.SakerInput;
import saker.build.util.data.annotation.ConverterConfiguration;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.zip.api.create.ZipCreationTaskBuilder;

public final class ZipCreateDataContext implements ZipContentsTaskOption {
	@SakerInput(value = { "Output" })
	public SakerPath outputOption = null;

	@SakerInput(value = { "Resources" })
	public Collection<ZipResourcesTaskOption> resourcesOption;

	@SakerInput(value = { "Include", "Includes" })
	public Collection<ZipIncludeTaskOption> includesOption;

	@SakerInput(value = { "Transformer", "Transformers" })
	public Collection<ZipResourceTransformerTaskOption> transformersOption;

	@SakerInput("ModificationTime")
	@ConverterConfiguration({ DateDataConverter.class })
	public Date modificationTimeOption;

	public ZipCreateDataContext() {
	}

	public ZipCreateDataContext(ZipCreateDataContext copy) {
		this.resourcesOption = ObjectUtils.cloneArrayList(copy.getResources(), ZipResourcesTaskOption::clone);
		this.includesOption = ObjectUtils.cloneArrayList(copy.getIncludes(), ZipIncludeTaskOption::clone);
		this.outputOption = copy.outputOption;
		this.transformersOption = ObjectUtils.cloneArrayList(copy.transformersOption,
				ZipResourceTransformerTaskOption::clone);
		this.modificationTimeOption = copy.modificationTimeOption;
	}

	@Override
	public ZipCreateDataContext clone() {
		return new ZipCreateDataContext(this);
	}

	public void addTo(TaskContext taskcontext, ZipCreationTaskBuilder taskbuilder) {
		taskbuilder.setModificationTime(modificationTimeOption);
		Collection<ZipResourceTransformerTaskOption> transformers = this.transformersOption;

		addContentsTo(taskcontext, taskbuilder, this);

		if (!ObjectUtils.isNullOrEmpty(transformers)) {
			for (ZipResourceTransformerTaskOption transformeroption : transformers) {
				if (transformeroption == null) {
					continue;
				}
				transformeroption.addTo(taskcontext, taskbuilder);
			}
		}
	}

	public static void addContentsTo(TaskContext taskcontext, ZipCreationTaskBuilder taskbuilder,
			ZipContentsTaskOption contentoptions) {
		Collection<ZipIncludeTaskOption> includes = contentoptions.getIncludes();
		Collection<ZipResourcesTaskOption> resources = contentoptions.getResources();
		if (!ObjectUtils.isNullOrEmpty(includes)) {
			for (ZipIncludeTaskOption inc : includes) {
				if (inc == null) {
					continue;
				}
				inc.addTo(taskcontext, taskbuilder);
			}
		}
		if (!ObjectUtils.isNullOrEmpty(resources)) {
			for (ZipResourcesTaskOption res : resources) {
				if (res == null) {
					continue;
				}
				res.addTo(taskcontext, taskbuilder);
			}
		}
	}

	@Override
	public Collection<ZipResourcesTaskOption> getResources() {
		return resourcesOption;
	}

	@Override
	public Collection<ZipIncludeTaskOption> getIncludes() {
		return includesOption;
	}
}
