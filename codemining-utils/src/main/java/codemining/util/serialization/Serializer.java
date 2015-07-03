/**
 * 
 */
package codemining.util.serialization;

import codemining.util.SettingsLoader;

/**
 * An ISerializationStrategy factory/singleton.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public final class Serializer {
	private static ISerializationStrategy strategy;

	public static final String SERIALIZER_TYPE = SettingsLoader
			.getStringSetting("name", "Kryo");

	public static final String KRYO_SERIALIZER = "Kryo";
	public static final String JAVA_SERIALIZER = "Java";

	/**
	 * Return the singleton class of the Serialization strategy to be used.
	 * 
	 * @return
	 */
	public static synchronized ISerializationStrategy getSerializer() {
		if (strategy == null) {
			if (SERIALIZER_TYPE.equals(KRYO_SERIALIZER)) {
				strategy = new KryoSerialization();
			} else if (SERIALIZER_TYPE.equals(JAVA_SERIALIZER)) {
				strategy = new JavaSerialization();
			}
		}

		return strategy;
	}
}
