package com.recoyx.shockscript.analysis.util;

import java.math.BigInteger;
import com.google.common.primitives.UnsignedInteger;

public final class ArbitraryNumber
{
    private Object _value;

	static public Object zero(Class<?> classObj)
	{
		if (classObj == Integer.class)
			return new ArbitraryNumber(Integer.valueOf(0));
		else if (classObj == UnsignedInteger.class)
			return new ArbitraryNumber(UnsignedInteger.ZERO);
		else if (classObj == BigInteger.class)
			return new ArbitraryNumber(BigInteger.ZERO);
		else
			return new ArbitraryNumber(Double.valueOf(0));
	}

	static public Object one(Class<?> classObj)
	{
		if (classObj == Integer.class)
			return new ArbitraryNumber(Integer.valueOf(1));
		else if (classObj == UnsignedInteger.class)
			return new ArbitraryNumber(UnsignedInteger.ONE);
		else if (classObj == BigInteger.class)
			return new ArbitraryNumber(BigInteger.ONE);
		else
			return new ArbitraryNumber(Double.valueOf(1));
	}

    public ArbitraryNumber(Object value)
    {
        this._value = value;
    }

	public ArbitraryNumber increment()
	{
        var num = this._value;
		if (num instanceof Integer)
			return new ArbitraryNumber(
                ((Integer) num) + 1
            );
		else if (num instanceof UnsignedInteger)
			return new ArbitraryNumber(
                UnsignedInteger.valueOf(((UnsignedInteger) num).longValue() + 1)
            );
		else if (num instanceof BigInteger)
			return new ArbitraryNumber(
                ((BigInteger) num).add(BigInteger.ONE)
            );
		else
			return new ArbitraryNumber(
                ((Double) num) + 1
            );
	}

    /**
     * Returns <code>value * 2</code>.
     */
	public ArbitraryNumber twoProduct()
	{
        var num = this._value;
		if (num instanceof Integer)
			return new ArbitraryNumber(
                ((Integer) num) * 2
            );
		else if (num instanceof UnsignedInteger)
			return new ArbitraryNumber(
                UnsignedInteger.valueOf(((UnsignedInteger) num).longValue() * 2)
            );
		else if (num instanceof BigInteger)
			return new ArbitraryNumber(
                ((BigInteger) num).multiply(BigInteger.TWO)
            );
		else
			return new ArbitraryNumber(
                ((Double) num) * 2
            );
	}
}