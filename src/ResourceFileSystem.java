import static java.util.Objects.requireNonNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class ResourceFileSystem {
	private static final Map<String, String> overrides = new HashMap<>();
	
	public static InputStream getInputStream(String path) {
		if(overrides.containsKey(requireNonNull(path))) {
			try {
				return new ByteArrayInputStream(overrides.get(path).getBytes("UTF-8"));
			} catch(UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}
		return ResourceFileSystem.class.getResourceAsStream(path);
	}
	
	public static Map<String, String> getOverrides() {
		return overrides;
	}
}