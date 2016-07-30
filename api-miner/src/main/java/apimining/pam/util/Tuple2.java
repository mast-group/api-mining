package apimining.pam.util;

public class Tuple2<T1, T2> {
	public final T1 _1;
	public final T2 _2;

	public Tuple2(final T1 _1, final T2 _2) {
		this._1 = _1;
		this._2 = _2;
	}

	@Override
	public String toString() {
		return "(" + _1 + "," + _2 + ")";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_1 == null) ? 0 : _1.hashCode());
		result = prime * result + ((_2 == null) ? 0 : _2.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Tuple2))
			return false;
		final Tuple2<?, ?> other = (Tuple2<?, ?>) obj;
		return (_1 == null ? other._1 == null : _1.equals(other._1))
				&& (_2 == null ? other._2 == null : _2.equals(other._2));
	}

}
