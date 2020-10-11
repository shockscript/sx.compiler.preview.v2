package com.recoyx.sxc.semantics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import com.recoyx.sxc.semantics.errors.*;

public final class SxcFunction extends Symbol
{
    private Symbol _name;

    private Symbol _signature;

    private Symbol _definitionPackage;

    private Activation _activation;

    private Symbol _ownerVirtualProperty;

    private int _flags = 0;

    private HashMap<Symbol, Symbol> _overriders;

    private Vector<MethodOverload> _methodOverloads;

    protected SxcFunction(Symbol name, Symbol signature)
    {
        _name = name;
        _signature = signature;
    }

    @Override
    public SymbolKind kind()
    {
        return SymbolKind.FUNCTION;
    }

    @Override
    public Symbol origin()
    {
        return null;
    }

    @Override
    public Symbol name()
    {
        return _name;
    }

    @Override
    public Symbol signature()
    {
        return _signature;
    }

    public void setSignature(Symbol signature)
    {
        _signature = signature;
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
        return _definitionPackage != null ? _definitionPackage.packageID() : "";
    }

    @Override
    public Activation activation()
    {
        return _activation;
    }

    @Override
    public void setActivation(Activation obj)
    {
        _activation = obj;
    }

    @Override
    public Symbol ownerVirtualProperty()
    {
        return _ownerVirtualProperty;
    }

    @Override
    public void setOwnerVirtualProperty(Symbol property)
    {
        _ownerVirtualProperty = property;
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
        return (_flags & 1) != 0;
    }

    @Override
    public void setMarkNative(boolean value)
    {
        _flags = value ? _flags | 1 : (_flags & 1) != 0 ? _flags ^ 1 : _flags;
    }

    @Override
    public boolean markOverride()
    {
        return (_flags & 2) != 0;
    }

    @Override
    public void setMarkOverride(boolean value)
    {
        _flags = value ? _flags | 2 : (_flags & 2) != 0 ? _flags ^ 2 : _flags;
    }

    @Override
    public boolean markFinal()
    {
        return (_flags & 4) != 0;
    }

    @Override
    public void setMarkFinal(boolean value)
    {
        _flags = value ? _flags | 4 : (_flags & 4) != 0 ? _flags ^ 4 : _flags;
    }

    @Override
    public boolean isBoundMethod()
    {
        return (_flags & 8) != 0;
    }

    @Override
    public void setIsBoundMethod(boolean value)
    {
        _flags = value ? _flags | 8 : (_flags & 8) != 0 ? _flags ^ 8 : _flags;
    }

    @Override
    public boolean markYielding()
    {
        return (_flags & 16) != 0;
    }

    @Override
    public void setMarkYielding(boolean value)
    {
        _flags = value ? _flags | 16 : (_flags & 16) != 0 ? _flags ^ 16 : _flags;
    }

    @Override
    public Vector<MethodOverload> methodOverloads()
    {
        return _methodOverloads;
    }

    @Override
    public void initMethodOverloads()
    {
        _methodOverloads = _methodOverloads == null ? new Vector<>() : _methodOverloads;
    }

    @Override
    public void override(Delegate delegate)
        throws IncompatibleOverrideSignatureError
            ,  NoMethodToOverrideError
            ,  OverridingFinalMethodError
    {
        var name = this.name();
        if (name == null)
        {
            return;
        }

        Symbol superFunction = null
            ,  virtualProperty = this.ownerVirtualProperty()
            ,  superProperty = null;

        var ns = name.namespace().kind();

        if (ns == SymbolKind.EXPLICIT_NAMESPACE)
        {
            superProperty = delegate.inherit() != null ? delegate.inherit().lookupName(name) : null;
        }
        else if (ns != SymbolKind.PRIVATE_NAMESPACE)
        {
            // find overriding method in one of {public, protected, internal} namespaces
            superProperty = delegate.inherit() != null ? delegate.inherit().lookupReservedNamespaceName(SymbolKind.PUBLIC_NAMESPACE, name.localName()) : null;
            superProperty = superProperty != null ? superProperty : delegate.inherit() != null ? delegate.inherit().lookupReservedNamespaceName(SymbolKind.PROTECTED_NAMESPACE, name.localName()) : null;
            superProperty = superProperty != null ? superProperty : delegate.inherit() != null ? delegate.inherit().lookupReservedNamespaceName(SymbolKind.INTERNAL_NAMESPACE, name.localName()) : null;
        }

        if (superProperty != null && virtualProperty != null)
        {
            superProperty = superProperty.kind() == SymbolKind.VIRTUAL_PROPERTY ? superProperty : null;
        }

        else if (superProperty != null)
        {
            superProperty = superProperty.kind() == SymbolKind.FUNCTION ? superProperty : null;
        }

        if (virtualProperty != null)
        {
            boolean isGetter = this == virtualProperty.getter();
            if (superProperty == null || (isGetter ? superProperty.getter() == null : superProperty.setter() == null))
            {
                throw new NoMethodToOverrideError(isGetter ? "getter" : "setter", name);
            }
            superFunction = isGetter ? superProperty.getter() : superProperty.setter();
            if (superProperty.staticType() != virtualProperty.staticType())
            {
                throw new IncompatibleOverrideSignatureError(name, superFunction.signature());
            }
            if (superFunction.markFinal())
            {
                throw new OverridingFinalMethodError();
            }
        }
        else
        {
            if (superProperty == null)
            {
                throw new NoMethodToOverrideError("method", name);
            }
            if (superProperty.signature() != this.signature())
            {
                throw new IncompatibleOverrideSignatureError(name, superFunction.signature());
            }
            superFunction = superProperty;
            if (superFunction.markFinal())
            {
                throw new OverridingFinalMethodError();
            }
        }
        superFunction.initOverriders();
        superFunction.overriders().put(delegate.type(), this);
        this.setMarkOverride(true);
    }
}