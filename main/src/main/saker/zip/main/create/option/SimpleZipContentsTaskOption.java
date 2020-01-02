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
