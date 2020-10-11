package com.recoyx.sxc.semantics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import com.recoyx.sxc.semantics.errors.*;

public final class InstantiatedSxcFunction extends Symbol
{
    private Symbol _origin;
    private Symbol _signature;
    private Symbol _ownerVirtualProperty;
    private HashMap<Symbol, Symbol> _overriders;
    private boolean _isBoundMethod;

    public InstantiatedSxcFunction(Symbol origin, Symbol declaratorType, SymbolPool pool)
    {
        super();
        _origin = origin;
        _signature = origin.signature().replaceType(declaratorType);
        var owner = _origin.ownerVirtualProperty();
        if (owner != null)
        {
            _ownerVirtualProperty = pool.createInstantiatedVirtualProperty(owner, declaratorType);
        }
    }

    @Override
    public SymbolKind kind()
    {
        return SymbolKind.FUNCTION;
    }

    @Override
    public boolean isInstantiated()
    {
        return true;
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
    public Symbol signature()
    {
        return _signature;
    }

    @Override
    public Symbol definitionPackage()
    {
        return _origin.definitionPackage();
    }

    @Override
    public String packageID()
    {
        return _origin.packageID();
    }

    @Override
    public Activation activation()
    {
        return _origin.activation();
    }

    @Override
    public Symbol ownerVirtualProperty()
    {
        return _ownerVirtualProperty;
    }

    @Override
    public HashMap<Symbol, Symbol> overriders()
    {
        return _overriders;
    }

    @Override
    public void initOverriders()
    {
        _overriders = _overriders != null ? _overriders : new HashMap<Symbol, Symbol>();
    }

    @Override
    public boolean markNative()
    {
        return _origin.markNative();
    }

    @Override
    public boolean markOverride()
    {
        return _origin.markOverride();
    }

    @Override
    public boolean markFinal()
    {
        return _origin.markFinal();
    }

    @Override
    public boolean isBoundMethod()
    {
        return _origin.isBoundMethod() || _isBoundMethod;
    }

    @Override
    public void setIsBoundMethod(boolean value)
    {
        _isBoundMethod = value;
    }

    @Override
    public boolean markYielding()
    {
        return _origin.markYielding();
    }
}