package com.recoyx.sxc.semantics.errors;

public class AmbiguousReferenceError extends RuntimeException
{
    public String name;

    public AmbiguousReferenceError(String name)
    {
        super();
        this.name = name;
    }
}