package com.recoyx.shockscript.analysis.semantics;

import com.recoyx.shockscript.analysis.semantics.frames.Frame;
import com.recoyx.shockscript.analysis.semantics.types.Type;

public class Symbol
{
    public boolean isObjectValue()
    {
        return this instanceof ObjectValue;
    }

    public boolean isFrame()
    {
        return this instanceof Frame;
    }

    public boolean isType()
    {
        return this instanceof Type;
    }

    public 

    public String localName()
    {
        return "";
    }

    public Symbol name()
    {
        return null;
    }

    public Symbol property()
    {
        return null;
    }
}