package com.recoyx.sxc.application.outputstructures;


import com.recoyx.sxc.parser.Ast.*;
import com.recoyx.sxc.semantics.Symbol;
import java.util.HashMap;
import java.util.Vector;

public final class OutputType
{
    public HashMap<Symbol, Integer> varOffsets = new HashMap<>();

    public HashMap<Symbol, Boolean> externSymbols = new HashMap<>();

    public Vector<VarBindingNode> varBindings = null;

    public boolean isExternSymbol(Symbol symbol)
    {
        return externSymbols.get(symbol) != null;
    }
}