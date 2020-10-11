package com.recoyx.sxc.util;

import java.math.BigInteger;
import com.google.common.primitives.UnsignedInteger;

public final class NumericUtils
{
	static public Object zero(Class<?> classObj)
	{
		if (classObj == Integer.class)
			return Integer.valueOf(0);
		else if (classObj == UnsignedInteger.class)
			return UnsignedInteger.ZERO;
		else if (classObj == BigInteger.class)
			return BigInteger.ZERO;
		else
			return Double.valueOf(0);
	}

	static public Object one(Class<?> classObj)
	{
		if (classObj == Integer.class)
			return Integer.valueOf(1);
		else if (classObj == UnsignedInteger.class)
			return UnsignedInteger.ONE;
		else if (classObj == BigInteger.class)
			return BigInteger.ONE;
		else
			return Double.valueOf(1);
	}

	static public Object increment(Object num)
	{
		if (num instanceof Integer)
			return ((Integer) num) + 1;
		else if (num instanceof UnsignedInteger)
			return UnsignedInteger.valueOf(((UnsignedInteger) num).longValue() + 1);
		else if (num instanceof BigInteger)
			return ((BigInteger) num).add(BigInteger.ONE);
		else
			return ((Double) num) + 1;
	}

	static public Object per2(Object num)
	{
		if (num instanceof Integer)
			return ((Integer) num) * 2;
		else if (num instanceof UnsignedInteger)
			return UnsignedInteger.valueOf(((UnsignedInteger) num).longValue() * 2);
		else if (num instanceof BigInteger)
			return ((BigInteger) num).multiply(BigInteger.TWO);
		else
			return ((Double) num) * 2;
	}
}