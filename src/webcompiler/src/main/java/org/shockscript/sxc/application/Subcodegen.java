package com.recoyx.sxc.application;

import com.recoyx.sxc.semantics.*;
import com.recoyx.sxc.parser.*;
import com.recoyx.sxc.util.*;
import com.recoyx.sxc.verifier.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

/**
 * Codegen performing type substitution.
 */
public final class Subcodegen
{
    public Codegen back;

    public Symbol replacingType;

    public SymbolPool pool;

    public CodegenOutput output;

    public Subcodegen(Codegen back, Symbol replacingType)
    {
        this.back = back;
        this.replacingType = replacingType;
        this.pool = back.pool;
        this.output = back.output;
    }
}