package saker.zip.main.create.option;

import java.util.Collection;

import saker.build.thirdparty.saker.util.ObjectUtils;

public class SimpleZipContentsTaskOption implements ZipContentsTaskOption {
	private Collection<ZipResourcesTaskOption> resources;
	private Collection<ZipIncludeTaskOption> includes;

	public SimpleZipContentsTaskOption(ZipContentsTaskOption copy) {
		this.resources = ObjectUtils.cloneArrayList(copy.getResources(), ZipResourcesTaskOption::clone);
		this.includes = ObjectUtils.cloneArrayList(copy.getIncludes(), ZipIncludeTaskOption::clone);
	}

	@Override
	public Collection<ZipResourcesTaskOption> getResources() {
		return resources;
	}

	@Override
	public Collection<ZipIncludeTaskOption> getIncludes() {
		return includes;
	}

	@Override
	public ZipContentsTaskOption clone() {
		return this;
	}
}
