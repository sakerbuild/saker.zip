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