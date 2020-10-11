package com.recoyx.shockscript.analysis.util;

public class Vector<E> extends java.util.Vector<E>
{
    public Vector()
    {
        super();
    }

    public Vector(E[] array)
    {
        for (var element : array)
            this.add(element);
    }

    public E[] toArray()
    {
        var array = (E[]) new Object[this.size()];
        for (int i = 0; i != this.size(); ++i)
            array[i] = this.get(i);
        return array;
    }

    public String join(String sep)
    {
        var builder = new StringBuilder();
        boolean start = true;
        for (var element : this)
        {
            builder.append((start ? "" : sep) + element.toString());
            start = false;
        }
        return builder.toString();
    }

    public E shift()
    {
        var e = this.get(0);
        this.remove(0);
        return e;
    }

    public E pop()
    {
        var e = this.get(this.size() - 1);
        this.remove(this.size() - 1);
        return e;
    }

    public com.recoyx.shockscript.analysis.util.Vector<E> slice()
    {
        return this.slice(0);
    }

    public com.recoyx.shockscript.analysis.util.Vector<E> slice(int from)
    {
        return this.slice(from, Integer.MAX_VALUE);
    }

    public com.recoyx.shockscript.analysis.util.Vector<E> slice(int from, int to)
    {
        if (from > to)
        {
            var k = from;
            from = to;
            to = k;
        }
        to = to > this.size() ? this.size() : to;
        var r = new com.recoyx.shockscript.analysis.util.Vector<E>();
        for (int i = from; i != to; ++i)
            r.add(this.get(i));
        return r;
    }
}