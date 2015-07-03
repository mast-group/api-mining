package codemining.util.serialization;

/**
 * A single serialization strategy.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public interface ISerializationStrategy {

	class SerializationException extends Exception {
		private static final long serialVersionUID = 7492466587431989538L;

		public SerializationException(final Throwable e) {
			super(e);
		}
	}

	/**
	 * Deserialize from data.
	 * 
	 * @param data
	 * @return
	 * @throws SerializationException
	 */
	Object deserializeFrom(final byte[] data) throws SerializationException;

	/**
	 * Deserialize an object from a file.
	 * 
	 * @param filename
	 * @return
	 */
	Object deserializeFrom(final String filename) throws SerializationException;

	/**
	 * Serialize to bytes.
	 * 
	 * @param obj
	 * @return
	 * @throws SerializationException
	 */
	byte[] serialize(final Object obj) throws SerializationException;

	/**
	 * Serialize an object to a file.
	 * 
	 * @param obj
	 * @param filename
	 */
	void serialize(final Object obj, final String filename)
			throws SerializationException;

}