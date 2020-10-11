package com.recoyx.sxc.semantics.errors;

import com.recoyx.sxc.semantics.Symbol;

public class InheritingNameConflictError extends RuntimeException
{
    public Symbol name;
    public Symbol inheritedInterface;

    public InheritingNameConflictError(Symbol name, Symbol inheritedInterface)
    {
        super();
        this.name = name;
        this.inheritedInterface = inheritedInterface;
    }
}