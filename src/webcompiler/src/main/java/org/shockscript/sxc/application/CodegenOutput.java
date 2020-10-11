package com.recoyx.sxc.application;

import com.recoyx.sxc.semantics.*;
import com.recoyx.sxc.parser.*;
import com.recoyx.sxc.parser.Ast.*;
import com.recoyx.sxc.util.*;
import com.recoyx.sxc.verifier.*;
import com.recoyx.sxc.application.outputstructures.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

/**
 * Contains transpiling link data, such as associations of output names to types.
 */
public final class CodegenOutput
{
    /**
     * Output directory.
     */
    public Path outputPath;

    /**
     * Function output data.
     */
    public HashMap<Symbol, OutFunction> functions = new HashMap<>();

    /**
     * Type output data.
     */
    public HashMap<Symbol, OutType> types = new HashMap<>();

    /**
     * Scope chain output data.
     */
    public HashMap<Symbol, OutNames> frames = new HashMap<>();

    /**
     * Indicates which package-qualified symbols are
     * extern to ShockScript.
     */
    public HashMap<Symbol, Boolean> externSymbols = new HashMap<>();

    /**
     * Links package-qualified symbols to names.
     */
    public OutNames globalNames = null;

    /**
     * The StringBuilder object used for generating code.
     */
    public StringBuilder currentBuilder = null;

    private Vector<StringBuilder> builderStack = new Vector<>();

    /**
     * Content of the generated main function. 
     */
    public StringBuilder initCodeBuilder = new StringBuilder();

    /**
     * Output names for the initialising code.
     */
    public OutNames initCodeNames = null;

    public CodegenOutput(Path outputPath)
    {
        this.outputPath = outputPath;
        globalNames = new OutNames(this);
        initCodeNames = new OutNames(this);
        enterBuilder(new StringBuilder());
    }

    public OutType getType(Symbol type)
    {
        OutType r = types.get(type);
        if (r == null)
        {
            types.put(type, r = new OutType());
        }
        return r;
    }

    /**
     * Adds a StringBuilder to top of the builder stack.
     */
    public void enterBuilder(StringBuilder builder)
    {
        this.builderStack.add(builder);
        this.currentBuilder = builder;
    }

    /**
     * Pops top StringBuilder from the builder stack.
     */
    public void exitBuilder()
    {
        VectorUtils.pop(this.builderStack);
        this.currentBuilder = this.builderStack.size() == 0 ? null : this.builderStack.lastElement();
    }

    public boolean isExternSymbol(Symbol symbol)
    {
        return externSymbols.has(symbol);
    }
}