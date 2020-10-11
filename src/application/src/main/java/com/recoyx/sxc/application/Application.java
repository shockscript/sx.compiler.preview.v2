package com.recoyx.sxc.application;

import com.recoyx.sxc.semantics.*;
import com.recoyx.sxc.parser.*;
import com.recoyx.sxc.util.*;
import com.recoyx.sxc.verifier.*;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.diogonunes.jcdp.bw.Printer;
import com.diogonunes.jcdp.bw.Printer.Types;
import com.diogonunes.jcdp.color.api.Ansi.Attribute;
import com.diogonunes.jcdp.color.api.Ansi.BColor;
import com.diogonunes.jcdp.color.api.Ansi.FColor;
import com.diogonunes.jcdp.color.ColoredPrinter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.List;
import java.util.Vector;

public class Application
{
    public Arguments arguments = null;
    public boolean syntaxInvalidated;
    public boolean semanticsInvalidated;
    public int numOKPrograms = 0;
    public Verifier verifier = new Verifier();
    public Vector<Ast.ProgramNode> programs = new Vector<>();

    private ColoredPrinter grayPrinter;

    public static void main(String[] arguments2)
    {
        new Application(arguments2);
    }

    public Application(String[] arguments2)
    {
        arguments = new Arguments();
        var application = JCommander.newBuilder()
            .addObject(arguments)
            .build();
        application.setProgramName("sxc");
        application.parse(arguments2);

        grayPrinter = new ColoredPrinter.Builder(0, false)
            .foreground(FColor.BLUE)
            .build();

        if (arguments.help)
        {
            application.usage();
            return;
        }

        Path executionPath = getExecutionPath();

        if (!arguments.builtins && (arguments.importPaths.size() == 0) && (arguments.framework == null || arguments.framework.equals("")))
        {
            // load standard library
            this.verifier.allowDuplicates = true;
            this.revealShockScriptNamespace();
            var sources = this.resolveSources(VectorUtils.fromArray(new Path[] {executionPath.resolve("../../lib/stl")}), new Vector<>());
            this.parseProgram(sources);

            if (!this.syntaxInvalidated)
            {
                this.verifyProgram();
            }
            this.hideShockScriptNamespace();
            this.verifier.allowDuplicates = false;
        }

        if (this.syntaxInvalidated || this.semanticsInvalidated)
        {
            System.exit(1);
            return;
        }

        if (arguments.builtins)
        {
            this.verifier.allowDuplicates = true;
            this.revealShockScriptNamespace();
        }

        var includePaths = new Vector<Path>();
        var excludePaths = new Vector<Path>();

        for (var src : arguments.includeSources)
        {
            includePaths.add(Paths.get(src));
        }

        for (var src : arguments.excludeSources)
        {
            excludePaths.add(Paths.get(src));
        }

        var sources = this.resolveSources(includePaths, excludePaths);
        if (sources.size() == 0)
        {
            System.out.println("No source specified.");
            return;
        }
        this.parseProgram(sources);
        if (!this.syntaxInvalidated)
        {
            this.verifyProgram();
        }
        if (syntaxInvalidated || semanticsInvalidated)
        {
            System.exit(1);
        }
    }

    public Path getExecutionPath()
    {
        try
        {
            return Paths.get(new java.io.File(Application.class.getProtectionDomain().getCodeSource().getLocation()
                .toURI()).getPath());
        }
        catch (URISyntaxException exc)
        {
            return null;
        }
    }

    public final class Source
    {
        public String source;
        public String url;

        public Source(String source, String url)
        {
            this.source = source;
            this.url = url;
        }
    }

    private Vector<Source> resolveSources(Vector<Path> sources, Vector<Path> excludeSources)
    {
        var r = new Vector<Source>();
        var sources2 = new Vector<Path>();

        for (var path : sources)
        {
            this.resolveSourceInclusions(path, sources2);
        }

        if (excludeSources != null)
        {
            for (var path : excludeSources)
            {
                this.resolveSourceExclusions(path, sources2);
            }
        }

        for (var path : sources2)
        {
            var text = "";
            try
            {
                text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            }
            catch (IOException exc)
            {
                System.err.println("Could not read file: " + path.toUri().toString());
                System.exit(0);
            }
            r.add(new Source(text, path.toUri().toString()));
        }

        return r;
    }

    private void resolveSourceInclusions(Path filePath, Vector<Path> intoCollection)
    {
        resolveSourceInclusions(filePath, intoCollection, false);
    }

    private void resolveSourceInclusions(Path filePath, Vector<Path> intoCollection, boolean recursing)
    {
        if (Files.exists(filePath))
        {
            if (Files.isDirectory(filePath))
            {
                try
                {
                    Files.list(filePath).forEach(subPath ->
                    {
                        this.resolveSourceInclusions(subPath, intoCollection, true);
                    });
                }
                catch (IOException exc)
                {
                }
            }
            else
            {
                if (filePath.toString().endsWith(".sx") || filePath.toString().endsWith(".shockscript"))
                {
                    intoCollection.add(Paths.get(filePath.normalize().toString()));
                }
                else if (!recursing)
                {
                    System.err.println("Unsupported file type.");
                    System.exit(0);
                }
            }
        }
        else
        {
            System.err.println("File does not exist: "+ filePath);
            System.exit(0);
        }
    }

    private void resolveSourceExclusions(Path filePath, Vector<Path> intoCollection)
    {
        var num = intoCollection.size();
        Path filePath2 = null;
        int i = -1;
        filePath = filePath.normalize();
        if (!Files.exists(filePath))
        {
            return;
        }
        if (Files.isDirectory(filePath))
        {
            // exclude source paths under directory
            for (i = 0; i != num; ++i)
            {
                filePath2 = intoCollection.get(i);
                if (filePath2.startsWith(filePath))
                {
                    intoCollection.remove(i--);
                }
            }
        }
        else
        {
            // exclude specific source path
            for (i = 0; i != num; ++i)
            {
                filePath2 = intoCollection.get(i);
                if (filePath.equals(filePath2))
                {
                    intoCollection.remove(i--);
                }
            }
        }
    }

    private void parseProgram(Vector<Source> sources)
    {
        for (var p : sources)
        {
            var script = new Script(p.source, p.url);
            var program = new Parser(script).parseProgram();
            if (program != null)
            {
                this.programs.add(program);
            }
            else
            {
                this.syntaxInvalidated = true;
                this.reportScriptProblems(script);
            }
        }
    }

    private void verifyProgram()
    {
        if (this.syntaxInvalidated)
        {
            return;
        }
        var unverified = VectorUtils.slice(this.programs, this.numOKPrograms);
        this.verifier.verifyPrograms(unverified);

        if (this.verifier.invalidated())
        {
            this.semanticsInvalidated = true;
        }
        else
        {
            this.numOKPrograms += unverified.size();
        }

        this.verifier.organiseProblems(unverified);

        for (var program : unverified)
        {
            this.reportScriptProblems(program.script);
        }
    }

    private void revealShockScriptNamespace()
    {
        var p = this.verifier.pool.topPackage;
        p.ownNames().defineName(this.verifier.pool.createName(p.internalNamespace(), "shockscript"), this.verifier.pool.shockscriptNamespace);
    }

    private void hideShockScriptNamespace()
    {
        var p = this.verifier.pool.topPackage;
        p.ownNames().deleteName(this.verifier.pool.createName(p.internalNamespace(), "shockscript"));
    }

    private void reportScriptProblems(Script script)
    {
        if (script.problems != null)
        {
            for (var problem : script.problems)
            {
                this.reportProblem(problem);
            }
        }

        if (script.subscripts != null)
        {
            for (var subscript : script.subscripts)
            {
                this.reportScriptProblems(subscript);
            }
        }
    }

    private void reportProblem(Problem problem)
    {
        System.out.println(problem.toString());
        grayPrinter.println(this.selectSourceRegion(problem.script(), problem.span()));
    }

    private String selectSourceRegion(Script script, Span span)
    {
        var line1Index = script.getLineStartCodeUnits(span.firstLine());

        var str1 = "";
        var str2 = "";
        var str3 = "";
        var str4 = "";

        str1 = "         ";
        str2 = new Integer(span.firstLine()).toString();
        str1 = str1.substring(0, str1.length() - str2.length()) + str2;
        str2 = script.source().substring(line1Index);

        {
            int j2 = 0;
            int j3 = -1;
            for (; j2 != str2.length(); ++j2)
            {
                var ch = str2.codePointAt(j2);
                if (ch == 0x0a || ch == 0x0d)
                {
                    j3 = j2;
                    break;
                }
            }
            if (j3 != -1)
            {
                str2 = str2.substring(0, j3);
            }
        }

        var i = script.source().offsetByCodePoints(0, span.start()) - line1Index;
        var j = script.source().offsetByCodePoints(0, span.end()) - line1Index;
        j = j > str2.length() ? str2.length() : j;

        str3 = str2.substring(i, j);
        str4 = str2.substring(j);
        str2 = str2.substring(0, i);

        var line = str1 + " | " + str2 + str3 + str4;
        var carret = "";

        j = str1.length() + 3 + str2.length();

        for (i = 0; i != j; ++i)
        {
            carret += " ";
        }

        for (i = 0; i != str3.length(); ++i)
        {
            carret += "^";
        }

        carret = i == 0 ? carret + "^" : carret;
        return line + "\n" + carret;
    }

    static public final class Arguments
    {
        @Parameter(names = {"--help", "-h"}, help = true)
        public boolean help = false;

        @Parameter(names = {"-o", "--output"}, order = 0)
        public String outputPath = null;

        @Parameter
        public List<String> includeSources = new Vector<>();

        @Parameter(names = {"--exclude-sources"}, order = 1)
        public List<String> excludeSources = new Vector<>();

        @Parameter(names = "--framework", order = 2)
        public String framework = null;

        @Parameter(names = "--import", order = 3)
        public List<String> importPaths = new Vector<>();

        @Parameter(names = "--builtins", order = 4)
        public boolean builtins = false;
    }
}