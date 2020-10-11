package com.recoyx.sxc.application.outputstructures;

import com.recoyx.sxc.semantics.Symbol;
import java.util.HashMap;

/**
 * Collects output names and prevents conflict between symbol names.
 */
public final class OutputNames
{
    public OutputNames parentCollection;

    private HashMap<Symbol, String> _names = new HashMap<>();

    private CodegenOutput topOutput;

    public OutputNames(CodegenOutput topOutput)
    {
        this.topOutput = topOutput;
    }

    public String createQualifiedName(Symbol symbol, Symbol qname)
    {
        var name = qname.toString().replaceAll("%$", "S");
        return createName(symbol, name);
    }

    public String createName(Symbol symbol, String name)
    {
        var orig_name = name;
        int counter = 0;
        while (this.hasName(name) || topOutput.globalNames.hasName(name))
        {
            name = orig_name + Integer.toString(counter++);
        }
        _names.put(symbol, name);
        return name;
    }

    public String nameOf(Symbol symbol)
    {
        var r = _names.get(symbol);
        return r != null ? r : parentCollection != null ? parentCollection.nameOf(symbol) : null;
    }

    public boolean hasName(String name)
    {
        if (_names.containsValue(name))
        {
            return true;
        }
        return parentCollection == null ? false : parentCollection.hasName(name);
    }
}