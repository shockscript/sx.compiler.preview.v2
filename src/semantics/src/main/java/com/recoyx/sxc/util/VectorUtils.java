package com.recoyx.sxc.util;

import java.util.Vector;

public final class VectorUtils
{
    static public <T> Vector<T> fromArray(T[] array)
    {
        var vector = new Vector<T>();
        for (var element : array)
        {
            vector.add(element);
        }
        return vector;
    }

    static public <T> T[] toArray(Vector<T> vector)
    {
        var array = (T[]) new Object[vector.size()];
        for (int i = 0; i != vector.size(); ++i)
        {
            array[i] = vector.get(i);
        }
        return array;
    }

    static public <T> String join(Vector<T> vector, String sep)
    {
        var builder = new StringBuilder();
        boolean start = true;
        for (var element : vector)
        {
            builder.append((start ? "" : sep) + element.toString());
            start = false;
        }
        return builder.toString();
    }

    static public <T> String join(T[] array, String sep)
    {
        return join(fromArray(array), sep);
    }

    static public <T> T shift(Vector<T> vector)
    {
        var e = vector.get(0);
        vector.remove(0);
        return e;
    }

    static public <T> T pop(Vector<T> vector)
    {
        var e = vector.get(vector.size() - 1);
        vector.remove(vector.size() - 1);
        return e;
    }

    static public <T> T first(Vector<T> vector)
    {
        return vector.size() == 0 ? null : vector.get(0);
    }

    static public <T> T last(Vector<T> vector)
    {
        return vector.size() == 0 ? null : vector.get(vector.size() - 1);
    }

    static public <T> Vector<T> slice(Vector<T> vector, int from)
    {
        return slice(vector, from, Integer.MAX_VALUE);
    }

    static public <T> Vector<T> slice(Vector<T> vector, int from, int to)
    {
        if (from > to)
        {
            var k = from;
            from = to;
            to = k;
        }
        to = to > vector.size() ? vector.size() : to;
        var r = new Vector<T>();
        for (int i = from; i != to; ++i)
        {
            r.add(vector.get(i));
        }
        return r;
    }
}