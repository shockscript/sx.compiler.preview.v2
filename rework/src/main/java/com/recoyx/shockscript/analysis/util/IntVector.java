package com.recoyx.shockscript.analysis.util;

import java.util.Iterator;
import java.util.PrimitiveIterator;

public final class IntVector implements Iterable<Integer>
{
    private int[] _array = new int[5];
    private int _length = 0;

    public IntVector()
    {
    }

    public int first()
    {
        return _length == 0 ? 0 : _array[0];
    }

    public int last()
    {
        return _length == 0 ? 0 : _array[_length - 1];
    }

    public int size()
    {
        return _length;
    }

    public int length()
    {
        return _length;
    }

    public void setLength(int length)
    {
        if (length <= _length)
        {
            _length = length;
            return;
        }
        int k2 = _length;
        _length = length;
        int i = _array.length;
        while (i < length)
        {
            i += i;
        }
        int[] k = _array;
        _array = new int[i];
        for (int j = 0; j != k2; ++j)
        {
            _array[j] = k[j];
        }
    }

    public int pop()
    {
        if (length() == 0)
        {
            return 0;
        }
        var k = get(length() - 1);
        --_length;
        return k;
    }

    public int get(int index)
    {
        return index >= _length ? 0 : _array[index];
    }

    public void set(int index, int value)
    {
        if (index < _length)
        {
            _array[index] = value;
        }
    }

    public int push(int value)
    {
        if (_length == _array.length)
        {
            _grow();
        }
        _array[_length++] = value;
        return _length;
    }

    public IntVector chainPush(int value)
    {
        push(value);
        return this;
    }

    private void _grow()
    {
        int[] k = _array;
        int k2 = k.length;
        _array = new int[_array.length * 2];
        for (int j = 0; j != k2; ++j)
        {
            _array[j] = k[j];
        }
    }

    public Iterator<Integer> iterator()
    {
        return new PrimitiveIterator.OfInt()
        {
            int index = 0;

            @Override
            public boolean hasNext()
            {
                return index < _length;
            }

            @Override
            public Integer next()
            {
                return _array[index];
            }

            @Override
            public int nextInt()
            {
                return _array[index];
            }
        };
    }
}