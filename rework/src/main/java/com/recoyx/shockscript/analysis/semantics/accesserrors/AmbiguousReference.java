package com.recoyx.shockscript.analysis.semantics.accesserrors;

import com.recoyx.shockscript.analysis.semantics.Symbol;

public final class AmbiguousReference extends Symbol
{
    private String _name;

    public AmbiguousReference(String name)
    {
        this._name = name;
    }

    @Override
    public String localName()
    {
        return this._name;
    }
}