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
package saker.zip.api.create;

import java.io.Externalizable;

/**
 * Stateless factory class for creating a {@linkplain ZipResourceTransformer ZIP resource transformer}.
 * <p>
 * Clients should implement this interface.
 * <p>
 * Implementations should adhere the {@link #hashCode()} and {@link #equals(Object)} specification, and are recommended
 * to be {@link Externalizable}.
 */
public interface ZipResourceTransformerFactory {
	/**
	 * Creates a new transformer.
	 * <p>
	 * This is called when the ZIP archive creation is started. The transformer is only used for transforming a single
	 * ZIP archive.
	 * 
	 * @return The created ZIP transformer.
	 */
	public ZipResourceTransformer createTransformer();

	@Override
	public int hashCode();

	@Override
	public boolean equals(Object obj);
}
