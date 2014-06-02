package itemsetmining.util;

import itemsetmining.itemset.Itemset;
import itemsetmining.transaction.Transaction;

import org.apache.spark.serializer.KryoRegistrator;

import com.esotericsoftware.kryo.Kryo;

/** Register custom classes for Spark Kryo serialization */
public class ClassRegistrator implements KryoRegistrator {

	@Override
	public void registerClasses(final Kryo kryo) {
		kryo.register(Itemset.class);
		kryo.register(Transaction.class);
	}

}
