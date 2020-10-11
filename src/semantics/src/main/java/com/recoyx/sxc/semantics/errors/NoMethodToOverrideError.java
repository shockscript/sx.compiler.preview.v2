package com.recoyx.sxc.semantics.errors;

import com.recoyx.sxc.semantics.Symbol;

public class NoMethodToOverrideError extends RuntimeException
{
    public String kind;
    public Symbol name;

    public NoMethodToOverrideError(String kind, Symbol name)
    {
        super();
        this.kind = kind;
        this.name = name;
    }
}