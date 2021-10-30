import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class Config {
    public enum Configuration {
        ACTION("Action"),
        INPUT_FILE("InputFile"),
        OUTPUT_FILE("OutputFile"),
        BUFFER_SIZE("BufferSize");

        private final String conf;

        Configuration(String conf) {
            this.conf = conf;
        }
    }

    private final HashMap<Configuration, String> config;
    private final String splitter = "=";

    Config() {
        config = new HashMap<>();
        config.put(Configuration.ACTION, "");
        config.put(Configuration.INPUT_FILE, "");
        config.put(Configuration.OUTPUT_FILE, "");
        config.put(Configuration.BUFFER_SIZE, "");
    }

    public Error parse(final String configFileName) {
        try (BufferedReader fileReader = new BufferedReader(new FileReader(configFileName))) {
            String line = fileReader.readLine();

            while (line != null) {
                String[] cng = line.split(splitter);
                boolean itTrueConfig = false;

                if (cng[0].equals(Configuration.ACTION.conf)) {
                    config.replace(Configuration.ACTION, cng[1]);
                    itTrueConfig = true;
                }
                if (cng[0].equals(Configuration.INPUT_FILE.conf)) {
                    config.replace(Configuration.INPUT_FILE, cng[1]);
                    itTrueConfig = true;
                }
                if (cng[0].equals(Configuration.OUTPUT_FILE.conf)) {
                    config.replace(Configuration.OUTPUT_FILE, cng[1]);
                    itTrueConfig = true;
                }
                if (cng[0].equals(Configuration.BUFFER_SIZE.conf)) {
                    config.replace(Configuration.BUFFER_SIZE, cng[1]);
                    itTrueConfig = true;
                }

                if (cng.length != 2 && !itTrueConfig) {
                    return Error.NOT_CORRECT_CONFIG_ARGUMENTS;
                }

                line = fileReader.readLine();
            }
        } catch (IOException exception) {
            return Error.COULD_NOT_OPEN_FILE;
        }

        return null;
    }

    public String getArg(Configuration key) {
        return config.get(key);
    }
}
