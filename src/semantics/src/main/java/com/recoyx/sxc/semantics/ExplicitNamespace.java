package com.recoyx.sxc.semantics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import com.recoyx.sxc.semantics.errors.*;

public final class ExplicitNamespace extends Namespace
{
    private String _prefix;
    private String _uri;

    public ExplicitNamespace(String prefix, String uri)
    {
        _prefix = prefix;
        _uri = uri;
    }

    @Override
    public SymbolKind kind()
    {
        return SymbolKind.EXPLICIT_NAMESPACE;
    }

    @Override
    public String prefix()
    {
        return _prefix;
    }

    @Override
    public String uri()
    {
        return _uri;
    }

    @Override
    public String toString()
    {
        return _prefix;
    }
}