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
package saker.zip.impl.create;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import saker.build.file.FileHandle;
import saker.build.file.path.SakerPath;
import saker.build.file.provider.LocalFileProvider;
import saker.build.thirdparty.saker.util.io.ByteArrayRegion;
import saker.build.thirdparty.saker.util.io.ByteSource;

class LocalFileHandle implements FileHandle {
	//TODO this class should be in saker.build instead

	private SakerPath path;

	@Override
	public void writeTo(OutputStream os) throws IOException, NullPointerException {
		LocalFileProvider.getInstance().writeToStream(LocalFileProvider.toRealPath(path), os);
	}

	@Override
	public ByteArrayRegion getBytes() throws IOException {
		return LocalFileProvider.getInstance().getAllBytes(path);
	}

	@Override
	public ByteSource openByteSource() throws IOException {
		return LocalFileProvider.getInstance().openInput(path);
	}

	@Override
	public InputStream openInputStream() throws IOException {
		return LocalFileProvider.getInstance().openInputStream(path);
	}

	@Override
	public String getName() {
		return path.getFileName();
	}

	public LocalFileHandle(SakerPath path) {
		this.path = path;
	}

}
