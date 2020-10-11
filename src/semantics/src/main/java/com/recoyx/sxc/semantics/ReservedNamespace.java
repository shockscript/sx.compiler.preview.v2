package com.recoyx.sxc.semantics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import com.recoyx.sxc.semantics.errors.*;

public final class ReservedNamespace extends Namespace
{
    private SymbolKind _kind;
    private Symbol _definitionPackage;

    public ReservedNamespace(String type, Symbol definitionPackage)
    {
        _kind = type.equals("public") ? SymbolKind.PUBLIC_NAMESPACE
            : type.equals("private") ? SymbolKind.PRIVATE_NAMESPACE
            : type.equals("protected") ? SymbolKind.PROTECTED_NAMESPACE : SymbolKind.INTERNAL_NAMESPACE;
        _definitionPackage = definitionPackage;
    }

    @Override
    public SymbolKind kind()
    {
        return _kind;
    }

    @Override
    public Symbol definitionPackage()
    {
        return _definitionPackage;
    }

    @Override
    public String toString()
    {
        return kind() == SymbolKind.PUBLIC_NAMESPACE ? "public"
            : kind() == SymbolKind.PRIVATE_NAMESPACE ? "private"
            : kind() == SymbolKind.PROTECTED_NAMESPACE ? "protected" : "internal";
    }
}