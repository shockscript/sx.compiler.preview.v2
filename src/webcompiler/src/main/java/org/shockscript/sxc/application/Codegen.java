package com.recoyx.sxc.application;

import com.recoyx.sxc.semantics.*;
import com.recoyx.sxc.parser.*;
import com.recoyx.sxc.parser.Ast.*;
import com.recoyx.sxc.util.*;
import com.recoyx.sxc.verifier.*;
import com.recoyx.sxc.application.outputstructures.*;
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
 * Transpiles ShockScript AST and semantics to WASM.
 */
public final class Codegen
{
    public SymbolPool pool;

    public CodegenOutput output;

    public Codegen(SymbolPool pool, Path outputPath)
    {
        this.pool = pool;
        this.output = new CodegenOutput(outputPath);
    }

    public void generate(Vector<ProgramNode> programs)
    {
        // Do the following:
        // - Apply Extern meta data at every package block.
        // - Apply definition names at every package block.
        for (var program : programs)
        {
            for (var pckg_node : program.packages)
            {
                this.applyPackageProperties(pckg_node.block.directives, pckg_node.semNSSymbol);
            }
        }
        for (var program : programs)
        {
            for (var pckg_node : program.packages)
            {
                this.generateDirectives(pckg_node.block.directives, pckg_node.semNSSymbol);
            }
        }
        ...
    }

    /**
     * Does the following:
     * - Apply Extern meta data.
     * - Apply definition names under package block.
     */
    public void applyPackageProperties(Vector<DirectiveNode> directives, Symbol rel)
    {
        OutType out_type = null;
        VarBindingNode binding = null;

        for (var drtv : directives)
        {
            if (drtv instanceof StatementNode)
            {
            }
            else if (drtv instanceof DefinitionNode defn)
            {
                String externName = "";
                int externSlot = -1;
                var meta_data = defn.getMetaData("Extern");
                if (meta_data != null)
                {
                    defn.deleteMetaData("Extern");
                    if (defn.semNSSymbol != null)
                    {
                        if (meta_data.entries != null && meta_data.entries.size() == 1 && meta_data.entries.get(0).literal instanceof StringLiteralNode str)
                        {
                            externName = str.value;
                        }
                        else if (meta_data.entries != null && meta_data.entries.size() == 1 && meta_data.entries.get(0).literal instanceof NumericLiteralNode nltr)
                        {
                            externSlot = nltr.value;
                        }
                        else
                        {
                            externName = defn.semNSSymbol.packageID().replaceAll("%.", "_");
                            externName = (externName == "" ? "" : externName + "_") + defn.semNSSymbol.name().toString().replaceAll("::", "_");
                        }
                        if (rel.isPackage())
                        {
                            output.externSymbols.put(defn.semNSSymbol, null);
                            output.globalNames.createName(defn.semNSSymbol, externName);
                        }
                        else
                        {
                            if (defn instanceof VarDefinitionNode var_node)
                            {
                                out_type = output.getType(rel);
                                binding = var_node.bindings.get(0);
                                if (binding.pattern instanceof NamePatternNode pattern && externSlot != -1)
                                {
                                    out_type.externSymbols.put(pattern.semNSVariable, null);
                                    out_type.varOffsets.createName(pattern.semNSVariable, externSlot);
                                }
                            }
                            else if (defn instanceof FunctionDefinitionNode fn_node)
                            {
                                externName = output.nameOfPackageProperty(out_type);
                                externName = externName == "" ? "" : externName.endsWith("_t") ? externName.substring(0, externName.length() - 2) + "_" : externName + "_";
                                output.externSymbols.put(defn.semNSSymbol, null);
                                output.globalNames.createName(defn.semNSSymbol, externName);
                            }
                        }
                    }
                }
                else if (defn instanceof FunctionDefinitionNode fn_node && defn.markNative())
                {
                    if (rel.isPackage())
                    {
                        externName = defn.semNSSymbol.packageID();
                        externName = externName == "" ? "" : externName.replaceAll("%.", "_") + "_";
                        externName = externName + defn.semNSSymbol.name().toString().replace("::", "_");
                        output.externSymbols.put(defn.semNSSymbol, null);
                        output.globalNames.createName(defn.semNSSymbol, externName);
                    }
                    else
                    {
                        externName = output.globalNames.nameOf(out_type);
                        externName = externName == "" ? "" : externName.endsWith("_t") ? externName.substring(0, externName.length() - 2) + "_" : externName + "_";
                        externName += defn.semNSSymbol.name().toString().replace("::", "_");
                        output.externSymbols.put(defn.semNSSymbol, null);
                        output.globalNames.createName(defn.semNSSymbol, externName);
                    }
                }
                else if (defn instanceof VarDefinitionNode var_node && rel.isPackage())
                {
                    binding = var_node.bindings.get(0);
                    if (binding.pattern instanceof NamePatternNode pattern)
                    {
                        var name = rel.packageID();
                        name = name == "" ? "" : name.replaceAll("%.", "_") + "_";
                        name = name + pattern.semNSVariable.name().toString().replace("::", "_");
                        output.globalNames.createName(pattern.semNSVariable, name);
                    }
                }

                if (defn instanceof ClassDefinitionNode class_node && externName != "")
                {
                    this.applyPackageProperties(class_node.block.directives, defn.semNSSymbol);
                }
                else if (defn instanceof EnumDefinitionNode enum_node && externName != "")
                {
                    this.applyPackageProperties(enum_node.block.directives, defn.semNSSymbol);
                }
            }
            else if (drtv instanceof IncludeDirectiveNode inc_node)
            {
                this.applyPackageProperties(inc_node.subdirectives, rel);
            }
        }
    }

    public void generateDirectives(Vector<DirectiveNode> directives, Symbol rel)
    {
        for (var directive : directives)
        {
            generateDirective(directive, rel);
        }
    }

    public void generateDirective(DirectiveNode directive, Symbol rel)
    {
    }
}