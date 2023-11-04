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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;

import saker.build.exception.InvalidPathFormatException;
import saker.build.file.path.SakerPath;
import saker.build.file.path.WildcardPath;
import saker.std.api.file.location.FileLocation;
import saker.zip.impl.create.options.ChainIncludeResourceMapping;
import saker.zip.impl.create.options.DeflatedCompressionIncludeResourceMapping;
import saker.zip.impl.create.options.IdentityIncludeResourceMapping;
import saker.zip.impl.create.options.MultiIncludeResourceMapping;
import saker.zip.impl.create.options.StoredCompressionIncludeResourceMapping;
import saker.zip.impl.create.options.TargetDirectoryIncludeResourceMapping;
import saker.zip.impl.create.options.WildcardFilterIncludeResourceMapping;

/**
 * Interface specifying a stateless strategy for mapping entry paths in a ZIP archive.
 * <p>
 * Instances of this interface is mostly used when
 * {@linkplain ZipCreationTaskBuilder#addIncludeArchive(FileLocation, IncludeResourceMapping) including entries from
 * other archives}.
 * <p>
 * The {@link #mapResourcePath(SakerPath, boolean)} method is used to map or re-map the archive paths of entries in an
 * implementation dependent way.
 * <p>
 * Clients may implement this interface.
 * <p>
 * Implementations should adhere the {@link #hashCode()} and {@link #equals(Object)} specification, and are recommended
 * to be {@link Externalizable}.
 */
public interface IncludeResourceMapping {
	/**
	 * Maps the archive path of an entry.
	 * <p>
	 * The method is called when an entry from another archive is being included in the created ZIP. The method should
	 * return a set of paths to which the entry should be written to. The result may be <code>null</code>, empty set,
	 * singleton set, or can contain multiple result paths.
	 * <p>
	 * If the result is empty or <code>null</code>, the entry for the argument path will <b>not</b> be part of the
	 * created archive.
	 * <p>
	 * If the result contains one or more paths, then the entry will be written to every archive paths in the result
	 * set. This can also be used to include an entry multiple times with the same contents.
	 * 
	 * @deprecated Deprecated since saker.zip 0.8.5. Use {@link #mapResource(ZipResourceEntry, boolean)} instead, which
	 *                 allows manipulation additonal properties of the resources. This function is no longer used by the
	 *                 ZIP creator implementation after (and including) version 0.8.5.
	 * @param archivepath
	 *            The archive path of the entry being included.
	 * @param directory
	 *            <code>true</code> if the entry is a directory.
	 * @return The path(s) for which the entry should be written in the created archive.
	 */
	@Deprecated
	public Set<SakerPath> mapResourcePath(SakerPath archivepath, boolean directory);

	/**
	 * Maps a ZIP resource entry to zero, one, or more other other entries.
	 * <p>
	 * The method is called when an entry from another archive is being included in the created ZIP. The method should
	 * return a collection of resource entries to which the entry should be written to. The result may be
	 * <code>null</code>, empty collection, singleton collection, or can contain multiple result entries.
	 * <p>
	 * If the result is empty or <code>null</code>, the entry for the argument resource will <b>not</b> be part of the
	 * created archive.
	 * <p>
	 * If the result contains one or more entries, then the entry will be written to the result archive multiple times
	 * specifeid by their {@linkplain ZipResourceEntry#getEntryPath() entry paths}. This can be used to include an entry
	 * multiple times with the same contents but different paths.
	 * <p>
	 * This method replaces the previous {@link #mapResourcePath(SakerPath, boolean)}. The default implementation calls
	 * this method and returns a collection that has the same entry paths as returned by the
	 * {@link #mapResource(ZipResourceEntry, boolean)} function, but all other {@link ZipResourceEntry} attributes are
	 * taken from the argument.
	 * 
	 * @param resourceentry
	 *            The archive resource entry that is being included.
	 * @param directory
	 *            <code>true</code> if the entry is a directory.
	 * @return The entries that should be written in the created archive.
	 */
	public default Collection<? extends ZipResourceEntry> mapResource(ZipResourceEntry resourceentry,
			boolean directory) {
		SakerPath entrypath = resourceentry.getEntryPath();
		Set<SakerPath> mapped = mapResourcePath(entrypath, directory);
		if (mapped == null) {
			return null;
		}
		List<ZipResourceEntry> result = new ArrayList<>();
		for (SakerPath path : mapped) {
			if (entrypath.equals(path)) {
				result.add(resourceentry);
			} else {
				result.add(resourceentry.withEntryPath(path));
			}
		}
		return result;
	}

	@Override
	public int hashCode();

	@Override
	public boolean equals(Object obj);

	/**
	 * Gets the identity resource mapping.
	 * <p>
	 * The identity resource mapping will not transform the paths, and returns them as is.
	 * 
	 * @return The identity resource mapping.
	 */
	public static IncludeResourceMapping identity() {
		return IdentityIncludeResourceMapping.INSTANCE;
	}

	/**
	 * Gets a resource mapping that maps the archive paths under a given target directory.
	 * <p>
	 * The specified target directory will be prepended to all archive paths.
	 * 
	 * @param targetdirectory
	 *            The target directory path.
	 * @return The resource mapping.
	 * @throws NullPointerException
	 *             If the argument is <code>null</code>.
	 * @throws InvalidPathFormatException
	 *             If the argument path is not {@linkplain SakerPath#isForwardRelative() forward relative}.
	 */
	public static IncludeResourceMapping targetDirectory(SakerPath targetdirectory)
			throws NullPointerException, InvalidPathFormatException {
		return new TargetDirectoryIncludeResourceMapping(targetdirectory);
	}

	/**
	 * Gets a resource mapping that calls the argument mappings after each other.
	 * <p>
	 * For a mapping request, the result mapping will call the <code>first</code> mapping, and then the
	 * <code>second</code> mapping for each path that the <code>first</code> mapping returned.
	 * <p>
	 * If any of the arguments are <code>null</code>, the other one is returned.
	 * 
	 * @param first
	 *            The first mapping.
	 * @param second
	 *            The second mapping.
	 * @return The mapping that calls the argument mappings in order.
	 */
	public static IncludeResourceMapping chain(IncludeResourceMapping first, IncludeResourceMapping second) {
		if (first == null) {
			return second;
		}
		if (second == null) {
			return first;
		}
		return new ChainIncludeResourceMapping(first, second);
	}

	/**
	 * Gets a resource mapping that aggregates multiple mappings.
	 * <p>
	 * The returned mapping will call all argument mappings, and concatenate the result paths.
	 * <p>
	 * If the argument is <code>null</code> or empty, the result will exclude all entries.
	 * 
	 * @param mappings
	 *            The mappings.
	 * @return The created mapping.
	 */
	public static IncludeResourceMapping multi(IncludeResourceMapping... mappings) {
		return MultiIncludeResourceMapping.create(mappings);
	}

	/**
	 * Gets a resource mapping that aggregates multiple mappings.
	 * <p>
	 * The returned mapping will call all argument mappings, and merge the result paths.
	 * <p>
	 * If the argument is <code>null</code> or empty, the result will exclude all entries.
	 * 
	 * @param mappings
	 *            The mappings.
	 * @return The created mapping.
	 */
	public static IncludeResourceMapping multi(Iterable<? extends IncludeResourceMapping> mappings) {
		return MultiIncludeResourceMapping.create(mappings);
	}

	/**
	 * Gets a resource mapping that excludes the archive paths that are not included by the specified wildcard.
	 * <p>
	 * The result mapping will not transform the archive paths in any way. It will only check if the path is included by
	 * the given wildcard, and if so, works the same way as the {@linkplain #identity() identity mapping}. If it is not
	 * included, the archive entry will not be part of the created archive.
	 * 
	 * @param wildcard
	 *            The wildcard that specifies the inclusion pattern.
	 * @return The created resource mapping.
	 * @throws NullPointerException
	 *             If the argument is <code>null</code>.
	 */
	public static IncludeResourceMapping wildcardIncludeFilter(WildcardPath wildcard) throws NullPointerException {
		return new WildcardFilterIncludeResourceMapping(wildcard);
	}

	/**
	 * Gets a resource mapping that changes the compression method to {@link ZipEntry#STORED stored} (uncompressed).
	 * 
	 * @return The resource mapping.
	 * @since saker.zip 0.8.5
	 */
	public static IncludeResourceMapping storedCompressionMethod() {
		return StoredCompressionIncludeResourceMapping.INSTANCE;
	}

	/**
	 * Gets a resource mapping that changes the compression method to {@link ZipEntry#DEFLATED deflated}.
	 * 
	 * @return The resource mapping.
	 * @since saker.zip 0.8.5
	 */
	public static IncludeResourceMapping deflatedCompressionMethod() {
		return DeflatedCompressionIncludeResourceMapping.get(-1);
	}

	/**
	 * Gets a resource mapping that changes the compression method to {@link ZipEntry#DEFLATED deflated} with the
	 * specified compression level.
	 * 
	 * @param level
	 *            The compression level.
	 * @return The resource mapping.
	 * @since saker.zip 0.8.5
	 */
	public static IncludeResourceMapping deflatedCompressionMethod(int level) {
		return DeflatedCompressionIncludeResourceMapping.get(-1);
	}
}
