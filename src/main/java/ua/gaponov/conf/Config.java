package ua.gaponov.conf;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class Config {

    public static final String DAHUA_IP;
    public static final String DAHUA_USERNAME;
    public static final String DAHUA_PASSWORD;
    public static final int COUNT_RECORD_IN_BATCH;
    public static final String URL;
    public static final String DB_USER;
    public static final String DB_PASSWORD;

    static {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(Paths.get("application.properties"))) {
            props.load(in);
        } catch (Exception e) {
            //nop
        }

        DAHUA_IP = props.getProperty("dahua.ip");
        DAHUA_USERNAME = props.getProperty("dahua.username");
        DAHUA_PASSWORD = props.getProperty("dahua.password");
        COUNT_RECORD_IN_BATCH = Integer.parseInt(props.getProperty("dahua.count.record.in.batch"));

        URL = props.getProperty("db.url");
        DB_USER = props.getProperty("db.user");
        DB_PASSWORD = props.getProperty("db.password");
    }

    private Config() {
        throw new IllegalStateException("Utility class");
    }
}
