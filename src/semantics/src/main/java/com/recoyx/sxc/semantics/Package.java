package com.recoyx.sxc.semantics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import com.recoyx.sxc.semantics.errors.*;

public final class Package extends Symbol
{
    private final String _id;
    private final Names _ownNames = new Names();
    private Symbol _publicNamespace;
    private Symbol _internalNamespace;

    public Package(String id)
    {
        _id = id;
    }

    @Override
    public SymbolKind kind()
    {
        return SymbolKind.PACKAGE;
    }

    @Override
    public String packageID()
    {
        return _id;
    }

    @Override
    public Names ownNames()
    {
        return _ownNames;
    }

    @Override
    public Symbol publicNamespace()
    {
        return _publicNamespace;
    }

    @Override
    public void setPublicNamespace(Symbol ns)
    {
        _publicNamespace = ns;
    }

    @Override
    public Symbol internalNamespace()
    {
        return _internalNamespace;
    }

    @Override
    public void setInternalNamespace(Symbol ns)
    {
        _internalNamespace = ns;
    }

    @Override
    public Symbol lookupName(Symbol name)
        throws AmbiguousReferenceError
    {
        var s = _ownNames.lookupName(name);
        return s != null ? pool().createPackageProperty(this, s) : null;
    }

    @Override
    public Symbol lookupMultiName(Vector<Symbol> namespaces, String localName)
        throws AmbiguousReferenceError
    {
        var s = _ownNames.lookupMultiName(namespaces, localName);
        return s != null ? pool().createPackageProperty(this, s) : null;
    }
}