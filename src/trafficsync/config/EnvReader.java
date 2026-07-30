package trafficsync.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// This class reads our configuration files (like server.env) so we do not have to hardcode ports and IP addresses.
// It loads everything into a map when the program starts.
public class EnvReader {
    // We store the settings in a HashMap so we can quickly look up a value by its key.
    private final Map<String, String> envVars = new HashMap<>();

    // The constructor reads the file line by line. It skips empty lines and comments,
    // and splits the remaining lines by the equals sign to get the key and value.
    public EnvReader(String filePath) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int idx = line.indexOf('=');
                if (idx > 0) {
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim();
                    envVars.put(key, value);
                }
            }
        } catch (IOException e) {
            System.err.println("Warning: Failed to load config file: " + filePath + ". Using defaults if applicable.");
        }
    }

    // These methods allow other classes to ask for a setting.
    // If the setting is missing, they return a safe default value so the program does not crash.
    public String get(String key, String defaultValue) {
        return envVars.getOrDefault(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        String val = envVars.get(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
