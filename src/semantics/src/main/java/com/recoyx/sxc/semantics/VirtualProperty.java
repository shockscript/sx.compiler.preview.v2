package com.recoyx.sxc.semantics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import com.recoyx.sxc.semantics.errors.*;
import com.recoyx.sxc.util.VectorUtils;

public final class VirtualProperty extends Symbol
{
    private Symbol _name;
    private Symbol _type;
    private Symbol _getter;
    private Symbol _setter;
    private Symbol _definitionPackage;

    public VirtualProperty(Symbol name, Symbol type)
    {
        _name = name;
        _type = type;
    }

    @Override
    public SymbolKind kind()
    {
        return SymbolKind.VIRTUAL_PROPERTY;
    }

    @Override
    public Symbol name()
    {
        return _name;
    }

    @Override
    public Symbol definitionPackage()
    {
        return _definitionPackage;
    }

    @Override
    public void setDefinitionPackage(Symbol pckg)
    {
        _definitionPackage = pckg;
    }

    @Override
    public String packageID()
    {
        return definitionPackage() == null ? "" : definitionPackage().packageID();
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
        return _setter == null;
    }

    @Override
    public boolean writeOnly()
    {
        return _getter == null;
    }

    @Override
    public Symbol getter()
    {
        return _getter;
    }

    @Override
    public void setGetter(Symbol symbol)
    {
        _getter = symbol;
    }

    @Override
    public Symbol setter()
    {
        return _setter;
    }

    @Override
    public void setSetter(Symbol symbol)
    {
        _setter = symbol;
    }
}