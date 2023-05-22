package ua.gaponov.conf;

import org.apache.log4j.*;
import ua.gaponov.utils.FilesUtils;

import java.util.Properties;

/**
 * @author Andriy Gaponov
 */
public class LoggingConfiguration {
    public static final String LOG_PATTERN = "log4j.conversion.pattern";
    public static final String LOG_FILE = "log4j.appender.file";
    public static final String LOG_LEVEL = "log4j.logger.level";
    public static final String LOG_ENCODING = "log4j.logger.encoding";
    private static final String DEFAULT_FILE_NAME = "application.properties";

    private static final Properties PROPERTIES = new Properties();

    static {
        try {
            PROPERTIES.load(FilesUtils.getFileInputStream(DEFAULT_FILE_NAME));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // creates pattern layout
        PatternLayout layout = new PatternLayout();
        layout.setConversionPattern(PROPERTIES.getProperty(LOG_PATTERN));

        // creates console appender
        ConsoleAppender consoleAppender = new ConsoleAppender();
        consoleAppender.setLayout(layout);
        consoleAppender.setEncoding(PROPERTIES.getProperty(LOG_ENCODING));
        consoleAppender.activateOptions();

        // creates file appender
        DailyRollingFileAppender rollingFileAppender = new DailyRollingFileAppender();
        rollingFileAppender.setEncoding(PROPERTIES.getProperty(LOG_ENCODING));
        rollingFileAppender.setFile(PROPERTIES.getProperty(LOG_FILE));
        rollingFileAppender.setLayout(layout);
        rollingFileAppender.setDatePattern("'.'yyyy-MM-dd");
        rollingFileAppender.activateOptions();

        // configures the root logger
        Logger rootLogger = Logger.getRootLogger();
        rootLogger.setLevel(Level.toLevel(PROPERTIES.getProperty(LOG_LEVEL)));
        rootLogger.removeAllAppenders();
        rootLogger.addAppender(consoleAppender);
        rootLogger.addAppender(rollingFileAppender);
    }
}
