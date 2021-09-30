/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;


/**
 * @author marynows
 *
 */
public final class Types {

	private Types() {
	}

	public static ParameterizedType newParameterizedType(final Type rawType, final Type... actualTypeArguments) {
		return new ParameterizedTypeImpl(rawType, null, actualTypeArguments);
	}

	private static final class ParameterizedTypeImpl implements ParameterizedType {
		private final Type rawType;
		private final Type ownerType;
		private final Type[] actualTypeArguments;

		private ParameterizedTypeImpl(final Type rawType, final Type ownerType, final Type... actualTypeArguments) {
			this.rawType = rawType;
			this.ownerType = ownerType;
			this.actualTypeArguments = actualTypeArguments;
		}

		@Override
		public Type[] getActualTypeArguments() {
			return Arrays.copyOf(this.actualTypeArguments, this.actualTypeArguments.length);
		}

		@Override
		public Type getRawType() {
			return this.rawType;
		}

		@Override
		public Type getOwnerType() {
			return this.ownerType;
		}

		@Override
		public String toString() {
			final StringBuilder builder = new StringBuilder();
			if (this.ownerType != null) {
				builder.append(toString(this.ownerType)).append(".");
			}
			builder.append(toString(this.rawType));
			if (this.actualTypeArguments.length > 0) {
				builder.append("<");
				for (final Type type : this.actualTypeArguments) {
					builder.append(toString(type)).append(", ");
				}
				builder.delete(builder.length() - 2, builder.length());
				builder.append(">");
			}
			return builder.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = (prime * result) + Arrays.hashCode(this.actualTypeArguments);
			result = (prime * result) + ((this.ownerType == null) ? 0 : this.ownerType.hashCode());
			result = (prime * result) + ((this.rawType == null) ? 0 : this.rawType.hashCode());
			return result;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof ParameterizedTypeImpl)) {
				return false;
			}
			final ParameterizedTypeImpl other = (ParameterizedTypeImpl) obj;
			if (!Arrays.equals(this.actualTypeArguments, other.actualTypeArguments)) {
				return false;
			}
			if (this.ownerType == null) {
				if (other.ownerType != null) {
					return false;
				}
			} else if (!this.ownerType.equals(other.ownerType)) {
				return false;
			}
			if (this.rawType == null) {
				if (other.rawType != null) {
					return false;
				}
			} else if (!this.rawType.equals(other.rawType)) {
				return false;
			}
			return true;
		}

		private String toString(final Type type) {
			if (type == null) {
				return "null";
			} else if (type instanceof Class<?>) {
				return ((Class<?>) type).getName();
			} else {
				return type.toString();
			}
		}
	}
}
