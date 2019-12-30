package saker.zip.main.create.option;

import java.util.Collection;

import saker.nest.scriptinfo.reflection.annot.NestFieldInformation;
import saker.nest.scriptinfo.reflection.annot.NestInformation;
import saker.nest.scriptinfo.reflection.annot.NestTypeUsage;
import saker.zip.main.doc.TaskDocs;

@NestInformation("Contains configurations that specify the resources that should be included in a created ZIP archive.")
@NestFieldInformation(value = "Resources",
		type = @NestTypeUsage(value = Collection.class, elementTypes = ZipResourcesTaskOption.class),
		info = @NestInformation(TaskDocs.ZIP_CREATE_RESOURCES))
@NestFieldInformation(value = "Includes",
		type = @NestTypeUsage(value = Collection.class, elementTypes = ZipIncludeTaskOption.class),
		info = @NestInformation(TaskDocs.ZIP_CREATE_INCLUDES))
public interface ZipContentsTaskOption {
	public default ZipContentsTaskOption clone() {
		return new SimpleZipContentsTaskOption(this);
	}

	public Collection<ZipResourcesTaskOption> getResources();

	public Collection<ZipIncludeTaskOption> getIncludes();

}