package org.hydroper.wasm;

import java.nio.charset.StandardCharsets;
import java.util.Vector;
import org.hydroper.utils.ByteArray;
import org.hydroper.wasm.enums.*;
import com.recoyx.sxc.util.IntVector;

/**
 * WASM output stream.
 *
 * This class is a Java port of the BinaryWriter class in Lobster implementation:
 * https://github.com/aardappel/lobster.
 * Reference and example of this API:
 * http://aardappel.github.io/lobster/implementation_wasm.html.
 */
public final class WASMOutput extends ByteArray
{
    private SectionKind currentSection = SectionKind.NONE;

    private SectionKind lastKnownSection = SectionKind.NONE;

    private int sectionSize = 0;

    private int sectionCount = 0;

    private int sectionData = 0;

    private int sectionIndexInFile = 0;

    private int sectionIndexInFileCode = 0;

    private int sectionIndexInFileData = 0;

    private int segmentPayloadStart = 0;

    private int numFunctionImports = 0;

    private int numGlobalImports = 0;

    private int numFunctionDecls = 0;

    private Vector<WASMFunction> functionSymbols = new Vector<>();

    private int functionBodyStart = 0;

    private int dataSectionSize = 0;

    private Vector<WASMDataSegment> dataSegments = new Vector<>();

    private Vector<WASMReloc> codeRelocs = new Vector<>();

    private Vector<WASMReloc> dataRelocs = new Vector<>();

    private void writeULEB(long value)
    {
        for (;;)
        {
            byte b = (byte) (value & 0x7F);
            writeByte(b);
            value >>= 7;
            if (value == 0)
                break;
            setPosition(position() - 1);
            writeByte((byte) (b | 0x80));
        }
    }

    private void writeSLEB(long value)
    {
        var negative = value < 0;
        for (;;)
        {
            byte b = (byte) (value & 0x7F);
            writeByte(b);
            var sign = value & 0x40;
            value >>= 7;
            if (negative)
                value |= 0x7f << (8 * 8 - 7);
            if ((value == 0 && sign == 0) || (value == -1 && sign == 0))
                break;
            setPosition(position() - 1);
            writeByte((byte) (b | 0x80));
        }
    }

    private static final int PATCHABLE_ULEB_SIZE = 5;

    private int patchableLEB()
    {
        var pos = length();
        for (int i = 0; i < PATCHABLE_ULEB_SIZE; ++i)
            writeByte((byte) 0x80);
        writeByte((byte) 0);
        return pos;
    }

    private void patchULEB(int pos, long value)
    {
        var oldPos = position();
        setPosition(pos);
        for (int i = 0; i < PATCHABLE_ULEB_SIZE; ++i)
        {
            writeByte((byte) (value & 0x7F));
            value >>= 7;
        }
        setPosition(oldPos);
    }

    private int writeWUTF(String str)
    {
        return writeWUTF(str, true);
    }

    private int writeWUTF(String str, boolean prependLength)
    {
        var ba = new ByteArray(str.getBytes(StandardCharsets.UTF_8));
        if (prependLength)
            writeULEB(ba.length());
        var pos = length();
        writeBytes(ba);
        return pos;
    }

    private boolean startsWithCount()
    {
        return currentSection != SectionKind.CUSTOM && currentSection != SectionKind.START;
    }

    private static final int R_WASM_FUNCTION_INDEX_LEB = 0;
    private static final int R_WASM_TABLE_INDEX_SLEB = 1;
    private static final int R_WASM_TABLE_INDEX_I32 = 2;
    private static final int R_WASM_MEMORY_ADDR_LEB = 3;
    private static final int R_WASM_MEMORY_ADDR_SLEB = 4;
    private static final int R_WASM_MEMORY_ADDR_I32 = 5;
    private static final int R_WASM_TYPE_INDEX_LEB = 6;
    private static final int R_WASM_GLOBAL_INDEX_LEB = 7;
    private static final int R_WASM_FUNCTION_OFFSET_I32 = 8;
    private static final int R_WASM_SECTION_OFFSET_I32 = 9;
    private static final int R_WASM_EVENT_INDEX_LEB = 10;

    private void relocULEB(int type, int symbolIndex, int targetIndex, boolean isFunction)
    {
        codeRelocs.add(new WASMReloc(type, length() - sectionData, symbolIndex, targetIndex, isFunction));
        // A relocatable LEB typically can be 0, since all information about
        // this value is stored in the relocation itself. But putting
        // a meaningful value here will help with reading the output of
        // objdump.
        patchULEB(patchableLEB(), isFunction ? symbolIndex : targetIndex);
    }

    public WASMOutput()
    {
        this(true);
    }

    public WASMOutput(boolean prefix)
    {
        if (prefix)
        {
            writeWUTF("\0asm", false);
            writeInt(1);
        }
    }

    /**
     * Call <code>begin</code>/<code>endSection</code>  pairs for each segment type, in order.
     * In between, call the add functions corresponding to the section type.
     */
    public void beginSection(SectionKind sk)
    {
        beginSection(sk, "");
    }

    public void beginSection(SectionKind sk, String name)
    {
        assert currentSection == SectionKind.NONE;
        currentSection = sk;
        if (sk == SectionKind.CODE)
            sectionIndexInFileCode = sectionIndexInFile;
        if (sk == SectionKind.DATA)
            sectionIndexInFileData = sectionIndexInFile;
        writeByte((byte) sk.valueOf());
        sectionSize = patchableLEB();
        if (sk == SectionKind.CUSTOM)
        {
            writeWUTF(name);
        }
        else
        {
            // Known sections must be created in order and only once.
            assert sk.valueOf() > lastKnownSection.valueOf();
            lastKnownSection = sk;
        }
        sectionCount = 0;
        sectionData = length();
        if (startsWithCount())
            patchableLEB();
    }

    public void endSection(SectionKind sk)
    {
        assert currentSection == sk;
        // Most sections start with a "count" field.
        if (startsWithCount())
            patchULEB(sectionData, sectionCount);
        // Patch up the size of this section.
        patchULEB(sectionSize, length() - sectionSize - PATCHABLE_ULEB_SIZE);
        currentSection = SectionKind.NONE;
        ++sectionIndexInFile;
    }

    public int addType(IntVector params, IntVector returns)
    {
        assert(currentSection == SectionKind.TYPE);
        writeULEB((long) TypeKind.FUNC.valueOf());
        writeULEB(params.size());
        for (var p : params)
            writeULEB(p);
        writeULEB(returns.size());
        for (var r : returns)
            writeULEB(r);
        return sectionCount++;
    }

    public static final int EXTERNAL_FUNCTION = 0;

    public static final int EXTERNAL_TABLE = 1;

    public static final int EXTERNAL_MEMORY = 2;

    public static final int EXTERNAL_GLOBAL = 3;

    public int addImportLinkFunction(String name, int tidx)
    {
        writeWUTF("");  // Module, unused.
        writeWUTF(name);
        writeULEB(EXTERNAL_FUNCTION);
        writeULEB(tidx);
        functionSymbols.add(new WASMFunction(name, true, true));
        ++sectionCount;
        return numFunctionImports++;
    }

    public int addImportGlobal(String name, byte type, boolean isMutable)
    {
        writeWUTF("");  // Module, unused.
        writeWUTF(name);
        writeULEB(EXTERNAL_GLOBAL);
        writeByte(type);
        writeULEB(isMutable ? 1 : 0);
        ++sectionCount;
        return numGlobalImports++;
    }

    public int getNumFunctionImports()
    {
        return numFunctionImports;
    }

    public int getNumGlobalImports()
    {
        return numGlobalImports;
    }

    public void addFunction(int tidx)
    {
        assert currentSection == SectionKind.FUNCTION;
        writeULEB(tidx);
        ++numFunctionDecls;
        ++sectionCount;
    }

    public int getNumDefinedFunctions()
    {
        return numFunctionDecls;
    }

    public void addTable() {
        assert currentSection == SectionKind.TABLE;
        writeByte(TypeKind.ANYFUNC.valueOf());  // Currently only option.
        writeULEB(0);  // Flags: no maximum.
        writeULEB(0);  // Initial length.
        ++sectionCount;
    }

    public void addMemory(int initialPages)
    {
        assert currentSection == SectionKind.MEMORY;
        writeULEB(0);  // Flags: no maximum.
        writeULEB(initialPages);
        ++sectionCount;
    }

    /**
     * You <b>must</b> emit an initialise expression after calling this, for example, <code>emitI32Const(0), emitEnd()</code>.
     */
    public void addGlobal(byte type, boolean isMutable)
    {
        assert currentSection == SectionKind.GLOBAL;
        writeByte(type);
        writeULEB(isMutable ? 1 : 0);
        ++sectionCount;
    }

    public void addExportFunction(String name, int fidx)
    {
        assert currentSection == SectionKind.EXPORT;
        writeWUTF(name);
        writeULEB(EXTERNAL_FUNCTION);
        writeULEB(fidx);
    }

    public void addExportGlobal(String name, int gidx)
    {
        assert currentSection == SectionKind.EXPORT;
        writeWUTF(name);
        writeULEB(EXTERNAL_GLOBAL);
        writeULEB(gidx);
    }

    public void addStart(int fidx)
    {
        assert currentSection == SectionKind.START;
        writeULEB(fidx);
    }

    /**
     * Simple 1:1 mapping of function IDs.
     */
    public void addElementAllFunctions()
    {
        // Add more flexible Element functions later.

        assert currentSection == SectionKind.ELEMENT;
        writeULEB(0);  // Table index, always 0 for now.
        emitI32Const(0);  // Offset.
        emitEnd();
        var totalFuns = numFunctionImports + numFunctionDecls;
        writeULEB(totalFuns);
        for (int i = 0; i < totalFuns; ++i)
            writeULEB(i);
        ++sectionCount;
    }

    /**
     * After calling this, use the emit functions to add to the function body,
     * and be sure to end with <code>emitEnd()</code>.
     */
    public void addCode(IntVector locals, String name, boolean local)
    {
        assert currentSection == SectionKind.CODE;
        assert functionBodyStart == 0;
        functionBodyStart = patchableLEB();
        var entries = new IntVector();
        for (var l : locals)
        {
            if (entries.length() == 0 || entries.last() != l)
            {
                entries.push(1);
                entries.push(l);
            }
            else
            {
                entries.set(entries.length() - 1, entries.last() + 1);
            }
        }
        writeULEB(entries.length());
        for (var e : entries)
            writeULEB(e);
        functionSymbols.add(new WASMFunction(name, false, local));
        ++sectionCount;
    }

    // --- Control flow ---

    public void emitUnreachable()
    {
        writeByte((byte) 0x00);
    }

    public void emitNop()
    {
        writeByte((byte) 0x01);
    }

    public void emitBlock(byte blockType)
    {
        writeByte((byte) 0x02);
        writeByte(blockType);
    }

    public void emitLoop(byte blockType)
    {
        writeByte((byte) 0x03);
        writeByte(blockType);
    }

    public void emitIf(byte blockType)
    {
        writeByte((byte) 0x04);
        writeByte(blockType);
    }

    public void emitElse()
    {
        writeByte((byte) 0x05);
    }

    public void emitEnd()
    {
        writeByte((byte) 0x0b);
    }

    public void emitBr(int relativeDepth)
    {
        writeByte((byte) 0x0c);
        writeULEB(relativeDepth);
    }

    public void emitBrIf(int relativeDepth)
    {
        writeByte((byte) 0x0d);
        writeULEB(relativeDepth);
    }

    public void emitBrTable(IntVector targets, int defaultTarget)
    {
        writeByte((byte) 0x0e);
        writeULEB(targets.length());
        for (var t : targets)
            writeULEB(t);
        writeULEB(defaultTarget);
    }

    public void emitReturn()
    {
        writeByte((byte) 0x0f);
    }

    // --- Call operators ---

    // fun_idx is 0..N-1 imports followed by N..M-1 defined functions.
    public void emitCall(int funIdx)
    {
        writeByte((byte) 0x10);
        relocULEB(R_WASM_FUNCTION_INDEX_LEB, funIdx, 0, true);
    }

    public void emitCallIndirect(int typeIndex)
    {
        writeByte((byte) 0x11);
        relocULEB(R_WASM_TYPE_INDEX_LEB, 0, typeIndex, false);
        writeULEB(0);
    }

    // --- Parametric operators ---

    public void emitDrop()
    {
        writeByte((byte) 0x1a);
    }

    public void emitSelect()
    {
        writeByte((byte) 0x1b);
    }

    // --- Variable access ---

    public void emitGetLocal(int local)
    {
        writeByte((byte) 0x20);
        writeULEB(local);
    }

    public void emitSetLocal(int local)
    {
        writeByte((byte) 0x21);
        writeULEB(local);
    }

    public void emitTeeLocal(int local)
    {
        writeByte((byte) 0x22);
        writeULEB(local);
    }

    public void emitGetGlobal(int global)
    {
        writeByte((byte) 0x23);
        writeULEB(global);
    }

    public void emitSetGlobal(int global)
    {
        writeByte((byte) 0x24);
        writeULEB(global);
    }

    // --- Memory access ---

    public void emitI32Load(int offset) { emitI32Load(offset, 2); }

    public void emitI32Load(int offset, int flags) { writeByte((byte) 0x28); writeULEB(flags); writeULEB(offset); }

    public void emitI64Load(int offset) { emitI64Load(offset, 3); }

    public void emitI64Load(int offset, int flags) { writeByte((byte) 0x29); writeULEB(flags); writeULEB(offset); }

    public void emitF32Load(int offset) { emitF32Load(offset, 2); }

    public void emitF32Load(int offset, int flags) { writeByte((byte) 0x2A); writeULEB(flags); writeULEB(offset); }

    public void emitF64Load(int offset) { emitF64Load(offset, 3); }

    public void emitF64Load(int offset, int flags) { writeByte((byte) 0x2B); writeULEB(flags); writeULEB(offset); }

    public void emitI32Load8S(int offset) { emitI32Load8S(offset, 0); }

    public void emitI32Load8S(int offset, int flags) { writeByte((byte) 0x2C); writeULEB(flags); writeULEB(offset); }

    public void emitI32Load8U(int offset) { emitI32Load8U(offset, 0); }

    public void emitI32Load8U(int offset, int flags) { writeByte((byte) 0x2D); writeULEB(flags); writeULEB(offset); }

    public void emitI32Load16S(int offset) { emitI32Load16S(offset, 1); }

    public void emitI32Load16S(int offset, int flags) { writeByte((byte) 0x2E); writeULEB(flags); writeULEB(offset); }

    public void emitI32Load16U(int offset) { emitI32Load16U(offset, 1); }

    public void emitI32Load16U(int offset, int flags) { writeByte((byte) 0x2F); writeULEB(flags); writeULEB(offset); }

    public void emitI64Load8S(int offset) { emitI64Load8S(offset, 0); }

    public void emitI64Load8S(int offset, int flags) { writeByte((byte) 0x30); writeULEB(flags); writeULEB(offset); }

    public void emitI64Load8U(int offset) { emitI64Load8U(offset, 0); }

    public void emitI64Load8U(int offset, int flags) { writeByte((byte) 0x31); writeULEB(flags); writeULEB(offset); }

    public void emitI64Load16S(int offset) { emitI64Load16S(offset, 1); }

    public void emitI64Load16S(int offset, int flags) { writeByte((byte) 0x32); writeULEB(flags); writeULEB(offset); }

    public void emitI64Load16U(int offset) { emitI64Load16U(offset, 1); }

    public void emitI64Load16U(int offset, int flags) { writeByte((byte) 0x33); writeULEB(flags); writeULEB(offset); }

    public void emitI64Load32S(int offset) { emitI64Load32S(offset, 2); }

    public void emitI64Load32S(int offset, int flags) { writeByte((byte) 0x34); writeULEB(flags); writeULEB(offset); }

    public void emitI64Load32U(int offset) { emitI64Load32U(offset, 2); }

    public void emitI64Load32U(int offset, int flags) { writeByte((byte) 0x35); writeULEB(flags); writeULEB(offset); }

    public void emitI32Store(int offset) { emitI32Store(offset, 2); }

    public void emitI32Store(int offset, int flags) { writeByte((byte) 0x36); writeULEB(flags); writeULEB(offset); }

    public void emitI64Store(int offset) { emitI64Store(offset, 3); }

    public void emitI64Store(int offset, int flags) { writeByte((byte) 0x37); writeULEB(flags); writeULEB(offset); }

    public void emitF32Store(int offset) { emitF32Store(offset, 2); }

    public void emitF32Store(int offset, int flags) { writeByte((byte) 0x38); writeULEB(flags); writeULEB(offset); }

    public void emitF64Store(int offset) { emitF64Store(offset, 3); }

    public void emitF64Store(int offset, int flags) { writeByte((byte) 0x39); writeULEB(flags); writeULEB(offset); }

    public void emitI32Store8(int offset) { emitI32Store8(offset, 0); }

    public void emitI32Store8(int offset, int flags) { writeByte((byte) 0x3A); writeULEB(flags); writeULEB(offset); }

    public void emitI32Store16(int offset) { emitI32Store16(offset, 1); }

    public void emitI32Store16(int offset, int flags) { writeByte((byte) 0x3B); writeULEB(flags); writeULEB(offset); }

    public void emitI64Store8(int offset) { emitI64Store8(offset, 0); }

    public void emitI64Store8(int offset, int flags) { writeByte((byte) 0x3C); writeULEB(flags); writeULEB(offset); }

    public void emitI64Store16(int offset) { emitI64Store16(offset, 1); }

    public void emitI64Store16(int offset, int flags) { writeByte((byte) 0x3D); writeULEB(flags); writeULEB(offset); }

    public void emitI64Store32(int offset) { emitI64Store32(offset, 2); }

    public void emitI64Store32(int offset, int flags) { writeByte((byte) 0x3E); writeULEB(flags); writeULEB(offset); }

    public void emitCurrentMemory() { writeByte((byte) 0x3F); writeULEB(0); }

    public void emitGrowMemory() { writeByte((byte) 0x40); writeULEB(0); }

    // --- Constants ---

    public void emitI32Const(int value) { writeByte((byte) 0x41); writeSLEB(value); }
    public void emitI64Const(long value) { writeByte((byte) 0x42); writeSLEB(value); }
    public void emitF32Const(float value) { writeByte((byte) 0x43); writeFloat(value); }
    public void emitF64Const(double value) { writeByte((byte) 0x44); writeDouble(value); }

    // Getting the address of data in a data segment, encoded as a i32.const + reloc.
    public void emitI32ConstDataRef(int segment, int addend)
    {
        writeByte((byte) 0x41);
        relocULEB(R_WASM_MEMORY_ADDR_SLEB, segment, addend, false);
    }

    // fun_idx is 0..N-1 imports followed by N..M-1 defined functions.
    public void emitI32ConstFunctionRef(int funIdx) {
        writeByte((byte) 0x41);
        relocULEB(R_WASM_TABLE_INDEX_SLEB, funIdx, 0, true);
    }

    // --- Comparison operators ---

    public void emitI32Eqz() { writeByte((byte) 0x45); }
    public void emitI32Eq() { writeByte((byte) 0x46); }
    public void emitI32Ne() { writeByte((byte) 0x47); }
    public void emitI32LtS() { writeByte((byte) 0x48); }
    public void emitI32LtU() { writeByte((byte) 0x49); }
    public void emitI32GtS() { writeByte((byte) 0x4A); }
    public void emitI32GtU() { writeByte((byte) 0x4B); }
    public void emitI32LeS() { writeByte((byte) 0x4C); }
    public void emitI32LeU() { writeByte((byte) 0x4D); }
    public void emitI32GeS() { writeByte((byte) 0x4E); }
    public void emitI32GeU() { writeByte((byte) 0x4F); }

    public void emitI64Eqz() { writeByte((byte) 0x50); }
    public void emitI64Eq() { writeByte((byte) 0x51); }
    public void emitI64Ne() { writeByte((byte) 0x52); }
    public void emitI64LtS() { writeByte((byte) 0x53); }
    public void emitI64LtU() { writeByte((byte) 0x54); }
    public void emitI64GtS() { writeByte((byte) 0x55); }
    public void emitI64GtU() { writeByte((byte) 0x56); }
    public void emitI64LeS() { writeByte((byte) 0x57); }
    public void emitI64LeU() { writeByte((byte) 0x58); }
    public void emitI64GeS() { writeByte((byte) 0x59); }
    public void emitI64GeU() { writeByte((byte) 0x5A); }

    public void emitF32Eq() { writeByte((byte) 0x5B); }
    public void emitF32Ne() { writeByte((byte) 0x5C); }
    public void emitF32Lt() { writeByte((byte) 0x5D); }
    public void emitF32Gt() { writeByte((byte) 0x5E); }
    public void emitF32Le() { writeByte((byte) 0x5F); }
    public void emitF32Ge() { writeByte((byte) 0x60); }

    public void emitF64Eq() { writeByte((byte) 0x61); }
    public void emitF64Ne() { writeByte((byte) 0x62); }
    public void emitF64Lt() { writeByte((byte) 0x63); }
    public void emitF64Gt() { writeByte((byte) 0x64); }
    public void emitF64Le() { writeByte((byte) 0x65); }
    public void emitF64Ge() { writeByte((byte) 0x66); }

    // --- Numeric operators ---

    public void emitI32Clz() { writeByte((byte) 0x67); }
    public void emitI32Ctz() { writeByte((byte) 0x68); }
    public void emitI32PopCnt() { writeByte((byte) 0x69); }
    public void emitI32Add() { writeByte((byte) 0x6A); }
    public void emitI32Sub() { writeByte((byte) 0x6B); }
    public void emitI32Mul() { writeByte((byte) 0x6C); }
    public void emitI32DivS() { writeByte((byte) 0x6D); }
    public void emitI32DivU() { writeByte((byte) 0x6E); }
    public void emitI32RemS() { writeByte((byte) 0x6F); }
    public void emitI32RemU() { writeByte((byte) 0x70); }
    public void emitI32And() { writeByte((byte) 0x71); }
    public void emitI32Or() { writeByte((byte) 0x72); }
    public void emitI32Xor() { writeByte((byte) 0x73); }
    public void emitI32Shl() { writeByte((byte) 0x74); }
    public void emitI32ShrS() { writeByte((byte) 0x75); }
    public void emitI32ShrU() { writeByte((byte) 0x76); }
    public void emitI32RotL() { writeByte((byte) 0x77); }
    public void emitI32RotR() { writeByte((byte) 0x78); }

    public void emitI64Clz() { writeByte((byte) 0x79); }
    public void emitI64Ctz() { writeByte((byte) 0x7A); }
    public void emitI64PopCnt() { writeByte((byte) 0x7B); }
    public void emitI64Add() { writeByte((byte) 0x7C); }
    public void emitI64Sub() { writeByte((byte) 0x7D); }
    public void emitI64Mul() { writeByte((byte) 0x7E); }
    public void emitI64DivS() { writeByte((byte) 0x7F); }
    public void emitI64DivU() { writeByte((byte) 0x80); }
    public void emitI64RemS() { writeByte((byte) 0x81); }
    public void emitI64RemU() { writeByte((byte) 0x82); }
    public void emitI64And() { writeByte((byte) 0x83); }
    public void emitI64Or() { writeByte((byte) 0x84); }
    public void emitI64Xor() { writeByte((byte) 0x85); }
    public void emitI64Shl() { writeByte((byte) 0x86); }
    public void emitI64ShrS() { writeByte((byte) 0x87); }
    public void emitI64ShrU() { writeByte((byte) 0x88); }
    public void emitI64RotL() { writeByte((byte) 0x89); }
    public void emitI64RotR() { writeByte((byte) 0x8A); }

    public void emitF32Abs() { writeByte((byte) 0x8B); }
    public void emitF32Neg() { writeByte((byte) 0x8C); }
    public void emitF32Ceil() { writeByte((byte) 0x8D); }
    public void emitF32Floor() { writeByte((byte) 0x8E); }
    public void emitF32Trunc() { writeByte((byte) 0x8F); }
    public void emitF32Nearest() { writeByte((byte) 0x90); }
    public void emitF32Sqrt() { writeByte((byte) 0x91); }
    public void emitF32Add() { writeByte((byte) 0x92); }
    public void emitF32Sub() { writeByte((byte) 0x93); }
    public void emitF32Mul() { writeByte((byte) 0x94); }
    public void emitF32Div() { writeByte((byte) 0x95); }
    public void emitF32Min() { writeByte((byte) 0x96); }
    public void emitF32Max() { writeByte((byte) 0x97); }
    public void emitF32CopySign() { writeByte((byte) 0x98); }

    public void emitF64Abs() { writeByte((byte) 0x99); }
    public void emitF64Neg() { writeByte((byte) 0x9A); }
    public void emitF64Ceil() { writeByte((byte) 0x9B); }
    public void emitF64Floor() { writeByte((byte) 0x9C); }
    public void emitF64Trunc() { writeByte((byte) 0x9D); }
    public void emitF64Nearest() { writeByte((byte) 0x9E); }
    public void emitF64Sqrt() { writeByte((byte) 0x9F); }
    public void emitF64Add() { writeByte((byte) 0xA0); }
    public void emitF64Sub() { writeByte((byte) 0xA1); }
    public void emitF64Mul() { writeByte((byte) 0xA2); }
    public void emitF64Div() { writeByte((byte) 0xA3); }
    public void emitF64Min() { writeByte((byte) 0xA4); }
    public void emitF64Max() { writeByte((byte) 0xA5); }
    public void emitF64CopySign() { writeByte((byte) 0xA6); }

    // --- Conversion operators ---

    public void emitI32WrapI64() { writeByte((byte) 0xA7); }
    public void emitI32TruncSF32() { writeByte((byte) 0xA8); }
    public void emitI32TruncUF32() { writeByte((byte) 0xA9); }
    public void emitI32TruncSF64() { writeByte((byte) 0xAA); }
    public void emitI32TruncUF64() { writeByte((byte) 0xAB); }
    public void emitI64ExtendSI32() { writeByte((byte) 0xAC); }
    public void emitI64ExtendUI32() { writeByte((byte) 0xAD); }
    public void emitI64TruncSF32() { writeByte((byte) 0xAE); }
    public void emitI64TruncUF32() { writeByte((byte) 0xAF); }
    public void emitI64TruncSF64() { writeByte((byte) 0xB0); }
    public void emitI64TruncUF64() { writeByte((byte) 0xB1); }
    public void emitF32ConvertSI32() { writeByte((byte) 0xB2); }
    public void emitF32ConvertUI32() { writeByte((byte) 0xB3); }
    public void emitF32ConvertSI64() { writeByte((byte) 0xB4); }
    public void emitF32ConvertUI64() { writeByte((byte) 0xB5); }
    public void emitF32DemoteF64() { writeByte((byte) 0xB6); }
    public void emitF64ConvertSI32() { writeByte((byte) 0xB7); }
    public void emitF64ConvertUI32() { writeByte((byte) 0xB8); }
    public void emitF64ConvertSI64() { writeByte((byte) 0xB9); }
    public void emitF64ConvertUI64() { writeByte((byte) 0xBA); }
    public void emitF64PromoteF32() { writeByte((byte) 0xBB); }

    // --- Reinterpretations ---

    public void emitI32ReinterpretF32() { writeByte((byte) 0xBC); }
    public void emitI64ReinterpretF64() { writeByte((byte) 0xBD); }
    public void emitF32ReinterpretI32() { writeByte((byte) 0xBE); }
    public void emitF64ReinterpretI64() { writeByte((byte) 0xBF); }

    // --- End function ---

    public void emitEndFunction() {
        assert currentSection == SectionKind.CODE;
        emitEnd();
        assert functionBodyStart != 0;
        patchULEB(functionBodyStart,
            length() - functionBodyStart - PATCHABLE_ULEB_SIZE);
        functionBodyStart = 0;
    }

    public void addData(String data, String symbol, int align)
    {
        addData(data, symbol, align, true);
    }

    public void addData(String data, String symbol, int align, boolean local)
    {
        assert currentSection == SectionKind.DATA;
        writeULEB(0);  // Linear memory index.
        // Init exp: must use 32-bit for wasm32 target.
        emitI32Const(dataSectionSize);
        emitEnd();
        segmentPayloadStart = writeWUTF(data);
        dataSectionSize += data.length();
        dataSegments.add(new WASMDataSegment(symbol, align, data.length(), local));
        ++sectionCount;
    }

    // "off" is relative to the data in the last <code>addData()</code> call.
    public void dataFunctionRef(int fid, int offset)
    {
        assert segmentPayloadStart != 0;
        dataRelocs.add(new WASMReloc(R_WASM_TABLE_INDEX_I32,
                                offset + (segmentPayloadStart - sectionData),
                                fid,
                                0,
                                true));
    }

    // Call this last, to finalize the buffer into a valid WASM module,
    // and to add linking/reloc sections based on the previous sections.
    public void finish() {
        assert currentSection == SectionKind.NONE;
        // If this assert fails, you likely have not matched the number of
        // `addFunction` calls in a function section with the number of `addCode`
        // calls in a code section.
        assert (numFunctionImports + numFunctionDecls) == functionSymbols.size();
        // Linking section.
        {
            beginSection(SectionKind.CUSTOM, "linking");
            writeULEB(2);  // Version.
            int WASM_SEGMENT_INFO = 5;
            int WASM_INIT_FUNCS = 6;
            int WASM_COMDAT_INFO = 7;
            int WASM_SYMBOL_TABLE = 8;
            // Segment Info.
            {
                writeByte((byte) WASM_SEGMENT_INFO);
                var sisize = patchableLEB();
                writeULEB(dataSegments.size());
                for (var ds : dataSegments)
                {
                    writeWUTF(ds.name);
                    writeULEB(ds.align);
                    writeULEB(0);  // Flags. FIXME: any valid values?
                }
                patchULEB(sisize, length() - sisize - PATCHABLE_ULEB_SIZE);
            }
            // Symbol Table.
            {
                writeByte((byte) WASM_SYMBOL_TABLE);
                int stsize = patchableLEB();

                int SYMTAB_FUNCTION = 0;
                int SYMTAB_DATA = 1;
                int SYMTAB_GLOBAL = 2;
                int SYMTAB_SECTION = 3;
                int SYMTAB_EVENT = 4;

                int WASM_SYM_BINDING_WEAK = 1;
                int WASM_SYM_BINDING_LOCAL = 2;
                int WASM_SYM_VISIBILITY_HIDDEN = 4;
                int WASM_SYM_UNDEFINED = 16;
                int WASM_SYM_EXPORTED = 32;

                writeULEB(dataSegments.size() + functionSymbols.size());
                int segi = 0;
                for (var ds : dataSegments) {
                    writeByte((byte) SYMTAB_DATA);
                    writeULEB(ds.isLocal ? WASM_SYM_BINDING_LOCAL : WASM_SYM_EXPORTED);
                    writeWUTF(ds.name);
                    writeULEB(segi++);
                    writeULEB(0);  // Offset in segment, always 0 (1 seg per sym).
                    writeULEB(ds.size);
                }
                int wasm_function = 0;
                for (var fs : functionSymbols)
                {
                    writeByte((byte) SYMTAB_FUNCTION);
                    writeULEB(fs.isImported
                        ? WASM_SYM_UNDEFINED
                        : (fs.isLocal  ? WASM_SYM_BINDING_LOCAL : WASM_SYM_EXPORTED));
                    writeULEB(wasm_function++);
                    if (!fs.isImported)
                        writeWUTF(fs.name);
                }
                patchULEB(stsize, length() - stsize - PATCHABLE_ULEB_SIZE);
            }
            endSection(SectionKind.CUSTOM);  // linking
        }
        // Reloc sections
        {
            class F
            {
                public void encodeReloc(WASMReloc r)
                {
                    writeByte((byte) r.type);
                    writeULEB(r.srcOffset);
                    writeULEB(r.symbolIndex + (r.isFunction ? dataSegments.size() : 0));
                    if (r.type == R_WASM_MEMORY_ADDR_LEB ||
                        r.type == R_WASM_MEMORY_ADDR_SLEB ||
                        r.type == R_WASM_MEMORY_ADDR_I32 ||
                        r.type == R_WASM_FUNCTION_OFFSET_I32 ||
                        r.type == R_WASM_SECTION_OFFSET_I32)
                    {
                        writeSLEB(r.targetIndex);
                    }
                }
            }

            var f = new F();

            beginSection(SectionKind.CUSTOM, "reloc.CODE");
            writeULEB(sectionIndexInFileCode);
            writeULEB(codeRelocs.size());
            for (var r : codeRelocs)
                f.encodeReloc(r);
            endSection(SectionKind.CUSTOM);  // reloc.CODE

            beginSection(SectionKind.CUSTOM, "reloc.DATA");
            writeULEB(sectionIndexInFileData);
            writeULEB(dataRelocs.size());
            for (var r : dataRelocs)
                f.encodeReloc(r);
            endSection(SectionKind.CUSTOM);  // reloc.DATA
        }
    }
}