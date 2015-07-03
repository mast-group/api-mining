/**
 * 
 */
package codemining.util.serialization;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.logging.Logger;

import org.apache.commons.lang.SerializationUtils;

/**
 * A serialization strategy using Java's built-in mechanism.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class JavaSerialization implements ISerializationStrategy {

	private static final Logger LOGGER = Logger
			.getLogger(JavaSerialization.class.getName());

	@Override
	public Object deserializeFrom(byte[] data) throws SerializationException {
		return SerializationUtils.deserialize(data);
	}

	@Override
	public Object deserializeFrom(final String filename)
			throws SerializationException {
		LOGGER.info("Deserializing object from " + filename);
		try {
			final FileInputStream fisM = new FileInputStream(filename);
			final ObjectInputStream oisM = new ObjectInputStream(fisM);
			final Object obj = oisM.readObject();
			oisM.close();
			return obj;
		} catch (ClassNotFoundException e) {
			throw new ISerializationStrategy.SerializationException(e);
		} catch (IOException e) {
			throw new ISerializationStrategy.SerializationException(e);
		}
	}

	@Override
	public byte[] serialize(Object obj) throws SerializationException {
		return SerializationUtils.serialize((Serializable) obj);
	}

	@Override
	public void serialize(final Object obj, final String filename)
			throws SerializationException {
		LOGGER.info("Serializing object of type " + obj.getClass().getName()
				+ " to " + filename);
		try {
			final FileOutputStream fos = new FileOutputStream(filename);
			final ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(obj);
			oos.close();
		} catch (IOException e) {
			throw new ISerializationStrategy.SerializationException(e);
		}
	}

}
