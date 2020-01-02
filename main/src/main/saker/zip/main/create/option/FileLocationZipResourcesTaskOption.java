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

import saker.build.file.path.SakerPath;
import saker.build.task.TaskContext;
import saker.std.api.file.location.ExecutionFileLocation;
import saker.std.api.file.location.FileLocation;
import saker.std.api.file.location.FileLocationVisitor;
import saker.std.api.file.location.LocalFileLocation;
import saker.zip.api.create.ZipCreationTaskBuilder;

final class FileLocationZipResourcesTaskOption implements ZipResourcesTaskOption {
	private final FileLocation fileLocation;

	FileLocationZipResourcesTaskOption(FileLocation filelocation) {
		this.fileLocation = filelocation;
	}

	@Override
	public ZipResourcesTaskOption clone() {
		return this;
	}

	@Override
	public void addTo(TaskContext taskcontext, ZipCreationTaskBuilder taskbuilder) {
		FileLocation filelocation = fileLocation;
		addFileLocationTo(taskcontext, taskbuilder, filelocation);
	}

	public static void addFileLocationTo(TaskContext taskcontext, ZipCreationTaskBuilder taskbuilder,
			FileLocation filelocation) {
		filelocation.accept(new FileLocationVisitor() {
			@Override
			public void visit(ExecutionFileLocation loc) {
				SakerPath filepath = loc.getPath();
				taskbuilder.addResource(loc, SakerPath.valueOf(filepath.getFileName()));
			}

			@Override
			public void visit(LocalFileLocation loc) {
				SakerPath filepath = loc.getLocalPath();
				taskbuilder.addResource(loc, SakerPath.valueOf(filepath.getFileName()));
			}
		});
	}
}