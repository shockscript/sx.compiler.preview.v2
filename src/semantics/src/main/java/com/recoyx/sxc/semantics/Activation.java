package com.recoyx.sxc.semantics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import com.recoyx.sxc.semantics.errors.*;

public final class Activation
{
    private Symbol _frame;
    private HashMap<Symbol, Boolean> _scopeExtendedProperties;

    public Activation(Symbol frame)
    {
        _frame = frame;
    }

    public Symbol frame()
    {
        return _frame;
    }

    public boolean isScopeExtendedProperty(Symbol property)
    {
        return _scopeExtendedProperties != null ? _scopeExtendedProperties.get(property) : false;
    }

    public void setScopeExtendedProperty(Symbol property)
    {
        _scopeExtendedProperties = _scopeExtendedProperties == null ? new HashMap<>() : _scopeExtendedProperties;
        _scopeExtendedProperties.put(property, true);
    }
}