package com.recoyx.sxc.semantics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import com.recoyx.sxc.semantics.errors.*;
import com.recoyx.sxc.util.VectorUtils;

public final class InstantiatedVariableProperty extends Symbol
{
    private Symbol _origin;
    private Symbol _type;

    public InstantiatedVariableProperty(Symbol origin, Symbol type)
    {
        _origin = origin;
        _type = type;
    }

    @Override
    public SymbolKind kind()
    {
        return SymbolKind.VARIABLE_PROPERTY;
    }

    @Override
    public Symbol origin()
    {
        return _origin;
    }

    @Override
    public Symbol name()
    {
        return _origin.name();
    }

    @Override
    public Symbol staticType()
    {
        return _type;
    }

    @Override
    public boolean readOnly()
    {
        return _origin.readOnly();
    }

    @Override
    public boolean writeOnly()
    {
        return false;
    }

    @Override
    public Symbol initialValue()
    {
        return _origin.initialValue();
    }

    @Override
    public boolean reassigned()
    {
        return _origin.reassigned();
    }

    @Override
    public void setReassigned(boolean value)
    {
        _origin.setReassigned(value);
    }
}