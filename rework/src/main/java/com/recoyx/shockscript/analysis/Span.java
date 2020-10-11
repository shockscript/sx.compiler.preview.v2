package com.recoyx.shockscript.analysis;

public final class Span implements Comparable<Span>
{
    static public Span inline(int line, int start, int end)
    {
        return new Span(line, start, line, end);
    }

    static public Span pointer(int line, int index)
    {
        return new Span(line, index, line, index);
    }

    private int _firstLine;
    private int _start;
    private int _lastLine;
    private int _end;

    public Span(int firstLine, int start, int lastLine, int end)
    {
        _firstLine = firstLine;
        _start = start;
        _lastLine = lastLine;
        _end = end;
    }

    public int firstLine()
    {
        return _firstLine;
    }

    public int lastLine()
    {
        return _lastLine;
    }

    public int start()
    {
        return _start;
    }

    public int end()
    {
        return _end;
    }

    public int compareTo(Span span)
    {
        return _firstLine < span._firstLine ? -1 :
               _start < span._start ? -1 :
               _start > span._start ? 1 : 0;
    }
}