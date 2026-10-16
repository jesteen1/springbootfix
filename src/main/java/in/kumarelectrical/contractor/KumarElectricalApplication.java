package in.kumarelectrical.contractor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@SpringBootApplication
public class KumarElectricalApplication {
    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(KumarElectricalApplication.class, args);
    }

    private static void loadDotEnv() {
        try {
            Path[] candidates = { Paths.get(".env"), Paths.get("/app/.env"), Paths.get("../.env") };
            for (Path path : candidates) {
                if (Files.exists(path)) {
                    List<String> lines = Files.readAllLines(path);
                    for (String line : lines) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#") && line.contains("=")) {
                            int eq = line.indexOf('=');
                            String key = line.substring(0, eq).trim();
                            String val = line.substring(eq + 1).trim();
                            if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                                if (val.length() >= 2) {
                                    val = val.substring(1, val.length() - 1);
                                }
                            }
                            if (System.getProperty(key) == null && System.getenv(key) == null) {
                                System.setProperty(key, val);
                            }
                        }
                    }
                    break;
                }
            }
        } catch (Exception ignored) {
        }
    }
}
