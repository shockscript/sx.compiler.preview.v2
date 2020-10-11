package com.recoyx.sxc.semantics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import com.recoyx.sxc.semantics.errors.*;

public final class SxcName extends Symbol
{
    private Symbol _namespace;
    private String _localName;

    public SxcName(Symbol namespace, String localName)
    {
        _namespace = namespace;
        _localName = localName;
    }

    @Override
    public SymbolKind kind()
    {
        return SymbolKind.NAME;
    }

    @Override
    public Symbol namespace()
    {
        return _namespace;
    }

    @Override
    public String localName()
    {
        return _localName;
    }

    @Override
    public String toString()
    {
        if (_namespace.kind() == SymbolKind.EXPLICIT_NAMESPACE)
        {
            return _namespace.toString() + "::" + _localName;
        }
        return _localName;
    }
}