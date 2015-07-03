/**
 * 
 */
package codemining.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.commons.lang.exception.ExceptionUtils;

/**
 * A singleton Settings loader. The settings loader, loads from
 * defaults.properties any settings.
 * 
 * The API of the loader is SettingsLoader.get<TYPE>Property(propertyName,
 * defaultValue)
 * 
 * Note that in the .properties files each property should be written
 * ClassName.propertyName
 * 
 * @author Miltiadis Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public final class SettingsLoader {
	public static final String PARAMETER_LOG_FILE = "runParameters.txt";

	/**
	 * The static logger instance.
	 */
	private static final Logger LOGGER = Logger.getLogger(SettingsLoader.class
			.getName());

	/**
	 * Private instance of properties.
	 */
	private final Properties properties;

	/**
	 * The unique static instance of the SettingsLoader.
	 */
	private static SettingsLoader instance;

	/**
	 * Get a boolean property.
	 * 
	 * @param propertyName
	 *            the name of the property file to be used
	 * @param defaultValue
	 *            the default value of the property
	 * @return he numeric property at the loaded file or the default value if
	 *         one is not found
	 */
	public static boolean getBooleanSetting(final String propertyName,
			final boolean defaultValue) {
		final SettingsLoader loaderInstance = getSettingsLoader();
		final boolean value = loaderInstance.getBooleanProperty(propertyName,
				defaultValue);
		LOGGER.config("Parameter " + propertyName + "for "
				+ Thread.currentThread().getStackTrace()[1].getClassName()
				+ " value " + value);
		return value;
	}

	/**
	 * Static getter for the singleton instance.
	 * 
	 * @param propertyName
	 *            the name of the property file to be used
	 * @param defaultValue
	 *            the default value of the property
	 * @return he numeric property at the loaded file or the default value if
	 *         one is not found
	 */
	public static double getNumericSetting(final String propertyName,
			final double defaultValue) {
		final SettingsLoader loaderInstance = getSettingsLoader();
		final double value = loaderInstance.getNumericProperty(propertyName,
				defaultValue);
		LOGGER.config("Parameter " + propertyName + "for "
				+ Thread.currentThread().getStackTrace()[1].getClassName()
				+ " value " + value);
		return value;
	}

	/**
	 * 
	 */
	public static SettingsLoader getSettingsLoader() {
		if (instance == null) {
			loadSettings();
		}
		return instance;
	}

	/**
	 * Static getter of a string property from the loaded file.
	 * 
	 * @param propertyName
	 *            the name of the property
	 * @param defaultValue
	 *            the default value of the property
	 * @return the string of the property at the loaded file or the default
	 *         value if one is not found
	 */
	public static String getStringSetting(final String propertyName,
			final String defaultValue) {
		final SettingsLoader loaderInstance = getSettingsLoader();
		final String value = loaderInstance.getStringProperty(propertyName,
				defaultValue);
		LOGGER.config("Parameter " + propertyName + "for "
				+ Thread.currentThread().getStackTrace()[1].getClassName()
				+ " value " + value);
		return value;
	}

	/**
	 * Load to the static store the settings file.
	 * 
	 * @throws IOException
	 *             when default file is not found
	 */
	public static void loadSettings() {
		instance = new SettingsLoader();
	}

	/**
	 * Load to the static store a settings file.
	 * 
	 * @param filename
	 *            the filename to load
	 * @throws IOException
	 *             when file is not found
	 */
	public static void loadSettings(final String filename) throws IOException {
		instance = new SettingsLoader(filename);
	}

	private PrintWriter parametrizationWriter = null;

	/**
	 * Constructor, loads default.properties file.
	 * 
	 * @throws IOException
	 *             when default properties file is not found
	 */
	private SettingsLoader() {
		properties = new Properties();
		try {
			parametrizationWriter = new PrintWriter(PARAMETER_LOG_FILE, "UTF-8");
			loadProperties("default.properties");
			addToParameterLog("Loaded default.properties at " + (new Date()));
		} catch (final FileNotFoundException e) {
			LOGGER.info("Configuration file not found. Loading defaults.");
			addToParameterLog("Loaded default properties at " + (new Date()));
		} catch (final IOException e) {
			LOGGER.warning("Error loading configuration file: "
					+ ExceptionUtils.getFullStackTrace(e));
		}
	}

	/**
	 * Constructor loads an arbitrary file.
	 * 
	 * @param file
	 *            the .properties filename to load
	 * @throws IOException
	 *             when default properties file is not found
	 */
	private SettingsLoader(final String file) throws IOException {
		properties = new Properties();
		try {
			loadProperties(file);
			parametrizationWriter = new PrintWriter(PARAMETER_LOG_FILE, "UTF-8");
		} catch (final FileNotFoundException e) {
			LOGGER.info("Configuration file not found. Loading defaults.");
		}
	}

	private void addToParameterLog(final String text) {
		parametrizationWriter.println(text);
		parametrizationWriter.flush();
	}

	/**
	 * Return a numeric property in the properties file.
	 * 
	 * @param propertyName
	 *            the name of the property
	 * @param defaultValue
	 *            the default value to be used for this property when the
	 *            numeric property is either not set or invalid
	 * @return the numeric property at the loaded file or the default value if
	 *         one is not found
	 */
	private boolean getBooleanProperty(final String propertyName,
			final boolean defaultValue) {
		final String fullPropertyName = getFullPropertyName(propertyName);
		try {
			final boolean value = Boolean.parseBoolean(properties.getProperty(
					fullPropertyName, Boolean.toString(defaultValue)));
			addToParameterLog(fullPropertyName + "=" + value);
			return value;
		} catch (final Exception e) {
			return defaultValue;
		}
	}

	/**
	 * Return the base property name.
	 * 
	 * @param basePropertyName
	 * @return
	 */
	private String getFullPropertyName(final String basePropertyName) {
		final StackTraceElement[] stackTraceElements = Thread.currentThread()
				.getStackTrace();
		// back in the stacktrace only 5 positions back, there we can find our
		// caller.
		final String classFullName = stackTraceElements[4].getClassName();
		Class<?> targetClass;
		try {
			targetClass = Class.forName(classFullName);
			return targetClass.getSimpleName() + "." + basePropertyName;
		} catch (final ClassNotFoundException e) {
			LOGGER.severe("Failed to find class " + classFullName);
		}
		final String[] splitName = classFullName.split("\\.");
		return splitName[splitName.length - 1] + "." + basePropertyName;
	}

	/**
	 * Return a numeric property in the properties file.
	 * 
	 * @param propertyName
	 *            the name of the property
	 * @param defaultValue
	 *            the default value to be used for this property when the
	 *            numeric property is either not set or invalid
	 * @return the numeric property at the loaded file or the default value if
	 *         one is not found
	 */
	private double getNumericProperty(final String propertyName,
			final double defaultValue) {
		final String fullPropertyName = getFullPropertyName(propertyName);
		try {
			final double value = Double.parseDouble(properties.getProperty(
					fullPropertyName, Double.toString(defaultValue)));
			addToParameterLog(fullPropertyName + "=" + value);
			return value;
		} catch (final Exception ex) {
			return defaultValue;
		}
	}

	/**
	 * Return a string property.
	 * 
	 * @param propertyName
	 *            the name of the property
	 * @param defaultValue
	 *            the default value of the property
	 * @return the string of the property at the loaded file or the default
	 *         value if one is not found
	 */
	private String getStringProperty(final String propertyName,
			final String defaultValue) {
		final String fullPropertyName = getFullPropertyName(propertyName);
		final String loadedProperty = properties.getProperty(fullPropertyName,
				defaultValue);
		addToParameterLog(fullPropertyName + "=" + loadedProperty);
		return loadedProperty;

	}

	/**
	 * Load the properties from a file.
	 * 
	 * @param filename
	 *            the .properties filename
	 * @throws IOException
	 *             when default properties file is not found
	 */
	private void loadProperties(final String filename) throws IOException {
		final FileInputStream input = new FileInputStream(filename);
		try {
			properties.load(input);
			LOGGER.info("Loaded properties file " + filename);
		} finally {
			input.close();
		}
	}
}
