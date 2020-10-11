package com.recoyx.sxc.semantics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import com.recoyx.sxc.semantics.errors.*;

public final class Names implements Iterable<NamePair>
{
    private Vector<Symbol> _keys = new Vector<>();
    private Vector<Symbol> _values = new Vector<>();

    public Names()
    {
    }

    public void defineName(Symbol key, Symbol value)
    {
        if (!_keys.contains(key))
        {
            _keys.add(key);
            _values.add(value);
        }
    }

    public boolean deleteName(Symbol key)
    {
        int i = _keys.indexOf(key);
        if (i == -1)
        {
            return false;
        }
        _keys.remove(i);
        _values.remove(i);
        return true;
    }

    public boolean hasName(Symbol key)
    {
        return _keys.contains(key);
    }

    public int length()
    {
        return _values.size();
    }

    public Symbol resolveIndex(int index)
    {
        return index < _values.size() ? _values.get(index) : null;
    }

    public int indexOf(Symbol symbol)
    {
        return symbol.kind() == SymbolKind.NAME ? _keys.indexOf(symbol) : _values.indexOf(symbol);
    }

    public Symbol lookupName(Symbol name)
    {
        int i = _keys.indexOf(name);
        return i == -1 ? null : _values.get(i);
    }

    public Symbol lookupMultiName(Vector<Symbol> namespaces, String localName)
        throws AmbiguousReferenceError
    {
        int i = 0,
            r = -1,
            l = _keys.size();
        boolean found = false;
        Symbol leading_ns = null;

        if (namespaces == null)
        {
            for (; i != l; ++i)
            {
                if (_keys.get(i).localName().equals(localName))
                {
                    return _values.get(i);
                }
            }

            return null;
        }

        for (var j = namespaces.size(); --j != -1;)
        {
            var ns = namespaces.get(j);
            for (i = 0; i != l; ++i)
            {
                Symbol name = _keys.get(i);
                if (name.localName().equals(localName))
                {
                    found = true;
                    if (name.namespace() == ns && leading_ns != ns)
                    {
                        if (r == -1)
                        {
                            r = i;
                            leading_ns = ns;
                        }
                        else
                        {
                            throw new AmbiguousReferenceError(localName);
                        }
                    }
                    continue;
                }
            }
            if (!found)
            {
                break;
            }
        }
        return r == -1 ? null : _values.get(r);
    }

    public Iterator<NamePair> iterator()
    {
        return new NamesIterator();
    }

    protected final class NamesIterator implements Iterator<NamePair>
    {
        int i;
        int l;

        public NamesIterator()
        {
            i = 0;
            l = _values.size();
        }

        public boolean hasNext()
        {
            return i != l;
        }

        public NamePair next()
        {
            return new NamePair(_keys.get(i), _values.get(i++));
        }
    }
}