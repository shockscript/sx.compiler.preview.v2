package org.hydroper.wasm;

public final class WASMDataSegment
{
    public String name;

    public int align;

    public int size;

    public boolean isLocal;

    public WASMDataSegment(String name, int align, int size, boolean isLocal)
    {
        this.name = name;
        this.align = align;
        this.size = size;
        this.isLocal = isLocal;
    }
}