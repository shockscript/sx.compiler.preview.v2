package com.recoyx.sxc.semantics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import com.recoyx.sxc.semantics.errors.*;
import com.recoyx.sxc.util.VectorUtils;

public final class VariableProperty extends Symbol
{
    private Symbol _name;
    private boolean _readOnly;
    private Symbol _type;
    private Symbol _initialValue;
    private boolean _reassigned;

    public VariableProperty(Symbol name, boolean readOnly, Symbol type)
    {
        _name = name;
        _readOnly = readOnly;
        _type = type;
    }

    @Override
    public SymbolKind kind()
    {
        return SymbolKind.VARIABLE_PROPERTY;
    }

    @Override
    public Symbol name()
    {
        return _name;
    }

    @Override
    public Symbol staticType()
    {
        return _type;
    }

    @Override
    public void setStaticType(Symbol type)
    {
        _type = type;
    }

    @Override
    public boolean readOnly()
    {
        return _readOnly;
    }

    @Override
    public void setReadOnly(boolean value)
    {
        _readOnly = value;
    }

    @Override
    public boolean writeOnly()
    {
        return false;
    }

    @Override
    public Symbol initialValue()
    {
        return _initialValue;
    }

    @Override
    public void setInitialValue(Symbol value)
    {
        _initialValue = value;
    }

    @Override
    public boolean reassigned()
    {
        return _reassigned;
    }

    @Override
    public void setReassigned(boolean value)
    {
        _reassigned = value;
    }
}