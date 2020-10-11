package com.recoyx.shockscript.analysis;

public final class SourceLocation
{
    private Script _script;
    private Span _span;

    public SourceLocation(Script script, Span span)
    {
        this._script = script;
        this._span = span;
    }

    public Script script()
    {
        return this._script;
    }

    public Span span()
    {
        return this._span;
    }
}