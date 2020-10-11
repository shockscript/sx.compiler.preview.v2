package com.recoyx.sxc.semantics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import com.recoyx.sxc.semantics.errors.*;

public final class ScopeChain
{
    private final SymbolPool _pool;
    private final Vector<Symbol> _openNamespaceList = new Vector<>();
    private Symbol _currentFrame;

    public ScopeChain(SymbolPool pool)
    {
        _pool = pool;
    }

    public SymbolPool pool()
    {
        return _pool;
    }

    public Vector<Symbol> openNamespaceList()
    {
        return _openNamespaceList;
    }

    public Symbol currentFrame()
    {
        return _currentFrame;
    }

    public Symbol lookupName(Symbol name)
    {
        return currentFrame().lookupName(name);
    }

    public Symbol lookupMultiName(Vector<Symbol> namespaces, String localName)
        throws AmbiguousReferenceError
    {
        return currentFrame().lookupMultiName(namespaces, localName);
    }

    public void enterFrame(Symbol frame)
    {
        frame.setParentFrame(frame.parentFrame() == null ? _currentFrame : frame.parentFrame());
        _currentFrame = frame;
        var list = frame.openNamespaceList();
        if (list != null)
        {
            for (var q : list)
            {
                _openNamespaceList.add(q);
            }
        }
    }

    public void exitFrame()
    {
        var k = _currentFrame;
        _currentFrame = k.parentFrame();
        var list = k.openNamespaceList();
        if (list != null)
        {
            _openNamespaceList.setSize(_openNamespaceList.size() - list.size());
        }
    }
}