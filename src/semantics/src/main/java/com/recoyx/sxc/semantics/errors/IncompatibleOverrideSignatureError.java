package com.recoyx.sxc.semantics.errors;

import com.recoyx.sxc.semantics.Symbol;

public class IncompatibleOverrideSignatureError extends RuntimeException
{
    public Symbol name;
    public Symbol expectedSignature;

    public IncompatibleOverrideSignatureError(Symbol name, Symbol expectedSignature)
    {
        super();
        this.name = name;
        this.expectedSignature = expectedSignature;
    }
}