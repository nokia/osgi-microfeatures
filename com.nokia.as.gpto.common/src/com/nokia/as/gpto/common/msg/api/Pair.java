package com.nokia.as.gpto.common.msg.api;

public class Pair<X, Y> {
	private final X x;
	private final Y y;

	public Pair(X x, Y y) {
		this.x = x;
		this.y = y;
	}

	public static <X, Y> Pair<X, Y> of(X left, Y right) {
		return new Pair<>(left, right);
	}

	public X getLeft() {
		return x;
	}

	public Y getRight() {
		return y;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((x == null) ? 0 : x.hashCode());
		result = prime * result + ((y == null) ? 0 : y.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Pair<?, ?> other = (Pair<?, ?>) obj;
		if (x == null) {
			if (other.x != null)
				return false;
		} else if (!x.equals(other.x))
			return false;
		if (y == null) {
			if (other.y != null)
				return false;
		} else if (!y.equals(other.y))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return new StringBuffer("<").append(x.toString())
				.append(", ")
				.append(y.toString())
				.append(">")
				.toString();
	}
}
