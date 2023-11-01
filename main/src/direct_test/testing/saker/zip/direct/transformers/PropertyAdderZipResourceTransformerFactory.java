package testing.saker.zip.direct.transformers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeMap;

import saker.build.file.path.SakerPath;
import saker.build.file.path.WildcardPath;
import saker.zip.api.create.ZipResourceEntry;
import saker.zip.api.create.ZipResourceTransformationContext;
import saker.zip.api.create.ZipResourceTransformer;
import saker.zip.api.create.ZipResourceTransformerFactory;

public final class PropertyAdderZipResourceTransformerFactory implements ZipResourceTransformerFactory {
	private final WildcardPath paths;
	private final String propertyName;
	private final String propertyValue;

	public PropertyAdderZipResourceTransformerFactory(WildcardPath paths, String propertyName, String propertyValue) {
		this.paths = paths;
		this.propertyName = propertyName;
		this.propertyValue = propertyValue;
	}

	@Override
	public ZipResourceTransformer createTransformer() {
		return new ZipResourceTransformer() {
			@Override
			@SuppressWarnings("deprecation")
			public boolean process(ZipResourceTransformationContext context, SakerPath resourcepath,
					InputStream resourceinput) throws IOException {
				throw new UnsupportedOperationException();
			}

			@Override
			public ZipResourceEntry process(ZipResourceTransformationContext context, ZipResourceEntry resourceentry,
					InputStream resourceinput) throws IOException {
				if (resourceinput == null || !paths.includes(resourceentry.getEntryPath())) {
					//directory, or not matched
					return resourceentry;
				}
				Properties props = new Properties();
				props.load(resourceinput);
				if (Objects.equals(propertyValue, props.get(propertyName))) {
					//property already equals
					return resourceentry;
				}
				//use deterministic order map instead of properties, as that is a hashtable
				TreeMap<Object, Object> propvals = new TreeMap<>(props);
				propvals.put(propertyName, propertyValue);
				try (OutputStream os = context.appendFile(resourceentry)) {
					//properties store adds a date comment, which we don't need for testing
					for (Entry<Object, Object> entry : propvals.entrySet()) {
						os.write(entry.getKey().toString().getBytes(StandardCharsets.UTF_8));
						os.write('=');
						os.write(entry.getValue().toString().getBytes(StandardCharsets.UTF_8));
						os.write('\n');
					}
				}
				return null;
			}
		};
	}

	public WildcardPath getPaths() {
		return paths;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public String getPropertyValue() {
		return propertyValue;
	}
}