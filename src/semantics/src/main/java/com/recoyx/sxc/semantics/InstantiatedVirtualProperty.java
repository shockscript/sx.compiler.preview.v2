package com.recoyx.sxc.semantics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import com.recoyx.sxc.semantics.errors.*;
import com.recoyx.sxc.util.VectorUtils;

public final class InstantiatedVirtualProperty extends Symbol
{
    private Symbol _origin;
    private Symbol _type;
    private Symbol _getter;
    private Symbol _setter;

    public InstantiatedVirtualProperty(Symbol origin, Symbol declaratorType)
    {
        _origin = origin;
        _type = origin.staticType().replaceType(declaratorType);
    }

    @Override
    public SymbolKind kind()
    {
        return SymbolKind.VIRTUAL_PROPERTY;
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
        return _origin.writeOnly();
    }

    @Override
    public Symbol getter()
    {
        return _getter;
    }

    @Override
    public void setGetter(Symbol getter)
    {
        _getter = getter;
    }

    @Override
    public Symbol setter()
    {
        return _setter;
    }

    @Override
    public void setSetter(Symbol setter)
    {
        _setter = setter;
    }

    @Override
    public boolean isInstantiated()
    {
        return true;
    }
}