/**
 * 
 */
package codemining.util.serialization.tui;

import codemining.util.serialization.ISerializationStrategy.SerializationException;
import codemining.util.serialization.JavaSerialization;
import codemining.util.serialization.Serializer;

/**
 * Convert from a Java serializable to the default format
 * 
 * @author jfowkes
 * 
 */
public class SerializableConversion {

	/**
	 * @param args
	 * @throws SerializationException
	 */
	public static void main(String[] args) throws SerializationException {
		if (args.length < 2) {
			System.err
					.println("Usage <inputSerializedFIle> <outputSerializedFile>");
			return;
		}
		final JavaSerialization javaSerializer = new JavaSerialization();
		final Object javaObject = javaSerializer.deserializeFrom(args[0]);

		Serializer.getSerializer().serialize(javaObject, args[1]);

	}

}
