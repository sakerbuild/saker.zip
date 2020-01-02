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
