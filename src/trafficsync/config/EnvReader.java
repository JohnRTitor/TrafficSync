package trafficsync.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnvReader {
    private final Map<String, String> envVars = new HashMap<>();

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
    
    public Map<String, String> getAll() {
        return new HashMap<>(envVars);
    }
}
