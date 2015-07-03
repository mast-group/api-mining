/**
 *
 */
package codemining.util.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.logging.Logger;

import org.objenesis.strategy.SerializingInstantiatorStrategy;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * A utility class for serialization.
 *
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 *
 */
public final class KryoSerialization implements ISerializationStrategy {

	private static final Logger LOGGER = Logger
			.getLogger(KryoSerialization.class.getName());

	@Override
	public Object deserializeFrom(final byte[] data)
			throws SerializationException {
		final ByteArrayInputStream stream = new ByteArrayInputStream(data);
		final Kryo kryo = new Kryo();
		final Input input = new Input(stream);

		kryo.setInstantiatorStrategy(new SerializingInstantiatorStrategy());

		final Object obj = kryo.readClassAndObject(input);
		input.close();
		return obj;
	}

	@Override
	public Object deserializeFrom(final String filename)
			throws SerializationException {
		LOGGER.info("Deserializing object from " + filename);
		try {
			final Kryo kryo = new Kryo();
			final Input input = new Input(new FileInputStream(filename));

			kryo.setInstantiatorStrategy(new SerializingInstantiatorStrategy());

			final Object obj = kryo.readClassAndObject(input);
			input.close();
			return obj;
		} catch (final FileNotFoundException e) {
			throw new ISerializationStrategy.SerializationException(e);
		}
	}

	@Override
	public byte[] serialize(final Object obj) throws SerializationException {
		LOGGER.info("Serializing object of type " + obj.getClass().getName()
				+ " to byte[]");
		final Kryo kryo = new Kryo();
		final ByteArrayOutputStream st = new ByteArrayOutputStream();
		final Output output = new Output(st);
		kryo.writeClassAndObject(output, obj);
		output.close();
		return st.toByteArray();
	}

	@Override
	public void serialize(final Object obj, final String filename)
			throws SerializationException {
		LOGGER.info("Serializing object of type " + obj.getClass().getName()
				+ " to " + filename);
		try {
			final Kryo kryo = new Kryo();
			final Output output = new Output(new FileOutputStream(filename));
			kryo.writeClassAndObject(output, obj);
			output.close();
		} catch (final FileNotFoundException e) {
			throw new ISerializationStrategy.SerializationException(e);
		}

	}

}
