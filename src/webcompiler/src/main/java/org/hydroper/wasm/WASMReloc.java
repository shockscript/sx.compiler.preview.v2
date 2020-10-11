package org.hydroper.wasm;

public final class WASMReloc
{
    public int type;

    /**
     * Indicates where the relocation occurs.
     */
    public int srcOffset;

    /**
     * Indicates a symbol index if this is a function.
     */
    public int symbolIndex;

    /**
     * Index inside thing we're referring to, e.g., addend.
     */
    public int targetIndex;

    public boolean isFunction;

    public WASMReloc(int type, int srcOffset, int symbolIndex, int targetIndex, boolean isFunction)
    {
        this.type = type;
        this.srcOffset = srcOffset;
        this.symbolIndex = symbolIndex;
        this.targetIndex = targetIndex;
        this.isFunction = isFunction;
    }
}