package com.recoyx.sxc.semantics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import com.recoyx.sxc.semantics.errors.*;

public final class Delegate
{
    private final Symbol _type;
    private Delegate _inherit;
    private Names _ownNames;
    private ProxyPropertyTrait _ownProxyPropertyTrait;
    private ProxyPropertyTrait _ownAttributeTrait;
    private Symbol _ownFilterProxy;
    private HashMap<Operator, Symbol> _ownOperators;

    public Delegate(Symbol type)
    {
        _type = type;
    }

    public Symbol type()
    {
        return _type;
    }

    public SymbolPool pool()
    {
        return _type.pool();
    }

    public Delegate inherit()
    {
        return _inherit;
    }

    public void setInherit(Delegate delegate)
    {
        _inherit = delegate;
    }

    public Names ownNames()
    {
        return _ownNames;
    }

    public void setOwnNames(Names names)
    {
        _ownNames = names;
    }

    public ProxyPropertyTrait ownProxyPropertyTrait()
    {
        return _ownProxyPropertyTrait;
    }

    public void setOwnProxyPropertyTrait(ProxyPropertyTrait trait)
    {
        _ownProxyPropertyTrait = trait;
    }

    public ProxyPropertyTrait ownAttributeTrait()
    {
        return _ownAttributeTrait;
    }

    public void setOwnAttributeTrait(ProxyPropertyTrait trait)
    {
        _ownAttributeTrait = trait;
    }

    public HashMap<Operator, Symbol> ownOperators()
    {
        return _ownOperators;
    }

    public void initOwnOperators()
    {
        _ownOperators = _ownOperators != null ? _ownOperators : new HashMap<Operator, Symbol>();
    }

    public Symbol ownFilterProxy()
    {
        return _ownFilterProxy;
    }

    public void setOwnFilterProxy(Symbol symbol)
    {
        _ownFilterProxy = symbol;
    }

    public Symbol lookupName(Symbol name)
    {
        Symbol symbol = null;
        if (type().kind() == SymbolKind.INSTANTIATED_TYPE)
        {
            symbol = inherit().lookupName(name);
            return symbol != null ? symbol.replaceType(type()) : null;
        }
        symbol = ownNames() != null ? ownNames().lookupName(name) : null;
        return (symbol != null ? symbol : inherit() != null ? inherit().lookupName(name) : null);
    }

    public Symbol lookupMultiName(Vector<Symbol> namespaces, String localName)
        throws AmbiguousReferenceError
    {
        Symbol symbol = null;
        if (type().kind() == SymbolKind.INSTANTIATED_TYPE)
        {
            symbol = inherit().lookupMultiName(namespaces, localName);
            return symbol != null ? symbol.replaceType(type()) : null;
        }
        symbol = ownNames() != null ? ownNames().lookupMultiName(namespaces, localName) : null;
        return (symbol != null ? symbol : inherit() != null ? inherit().lookupMultiName(namespaces, localName) : null);
    }

    public Symbol lookupReservedNamespaceName(SymbolKind namespaceKind, String localName)
    {
        Symbol symbol = null;
        if (type().kind() == SymbolKind.INSTANTIATED_TYPE)
        {
            symbol = inherit().lookupReservedNamespaceName(namespaceKind, localName);
            return symbol != null ? symbol.replaceType(type()) : null;
        }
        for (NamePair p : ownNames())
        {
            if (p.key.namespace().kind() == namespaceKind && p.key.localName() == localName)
            {
                return p.value;
            }
        }
        return (symbol != null ? symbol : inherit() != null ? inherit().lookupReservedNamespaceName(namespaceKind, localName) : null);
    }

    public ProxyPropertyTrait searchProxyPropertyTrait()
    {
        if (type().kind() == SymbolKind.INSTANTIATED_TYPE)
        {
            var r = inherit().searchProxyPropertyTrait();
            return r != null ? pool().createInstantiatedProxyPropertyTrait(r, type()) : null;
        }
        var r = ownProxyPropertyTrait();
        return r != null ? r : inherit() != null ? inherit().searchProxyPropertyTrait() : null;
    }

    public ProxyPropertyTrait searchAttributeTrait()
    {
        if (type().kind() == SymbolKind.INSTANTIATED_TYPE)
        {
            var r = inherit().searchAttributeTrait();
            return r != null ? pool().createInstantiatedProxyPropertyTrait(r, type()) : null;
        }
        var r = ownAttributeTrait();
        return r != null ? r : inherit() != null ? inherit().searchAttributeTrait() : null;
    }

    public Symbol searchFilterProxy()
    {
        if (type().kind() == SymbolKind.INSTANTIATED_TYPE)
        {
            var r = inherit().searchFilterProxy();
            return r != null ? pool().createInstantiatedFunction(r, type()) : null;
        }
        var r = ownFilterProxy();
        return r != null ? r : inherit() != null ? inherit().searchFilterProxy() : null;
    }

    public Symbol searchOperator(Operator operatorType)
    {
        if (type().kind() == SymbolKind.INSTANTIATED_TYPE)
        {
            var r = inherit().searchOperator(operatorType);
            return r != null ? pool().createInstantiatedFunction(r, type()) : null;
        }
        var r = ownOperators() != null ? ownOperators().get(operatorType) : null;
        return r != null ? r : inherit() != null ? inherit().searchOperator(operatorType) : null;
    }

    public Iterable<NamePair> names()
    {
        var delegate = this;
        return new Iterable<NamePair>()
        {
            public Iterator<NamePair> iterator()
            {
                return new NamesIterator(delegate);
            }
        };
    }

    protected final class NamesIterator implements Iterator<NamePair>
    {
        Delegate delegate = null;
        Iterator<NamePair> names = null;
        Vector<Symbol> instantiatedType = null;

        public NamesIterator(Delegate delegate)
        {
            this.delegate = delegate;
            names = delegate.ownNames() != null ? delegate.ownNames().iterator() : null;
            instantiatedType = null;
            if (delegate.type().kind() == SymbolKind.INSTANTIATED_TYPE)
            {
                instantiatedType = new Vector<Symbol>();
                instantiatedType.add(delegate.type());
            }
        }

        public boolean hasNext()
        {
            if (names != null)
            {
                if (names.hasNext())
                {
                    return true;
                }
            }
            while (delegate.inherit() != null)
            {
                delegate = delegate.inherit();
                if (delegate.type().kind() == SymbolKind.INSTANTIATED_TYPE)
                {
                    instantiatedType = instantiatedType != null ? new Vector<Symbol>() : null;
                    instantiatedType.add(delegate.type());
                }
                names = delegate.ownNames() != null ? delegate.ownNames().iterator() : null;
                if (names != null)
                {
                    if (names.hasNext())
                    {
                        return true;
                    }
                }
            }
            return false;
        }

        public NamePair next()
        {
            var p = names.next();
            if (instantiatedType != null)
            {
                for (int i = instantiatedType.size(); --i != -1;)
                {
                    Symbol type2 = instantiatedType.get(i);
                    p = new NamePair(p.key, p.value.replaceType(type2));
                }
            }
            return p;
        }
    }
}