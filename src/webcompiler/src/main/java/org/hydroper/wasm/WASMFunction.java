package org.hydroper.wasm;

public final class WASMFunction
{
    public String name;

    public boolean isImported;

    public boolean isLocal;

    public WASMFunction(String name, boolean isImported, boolean isLocal)
    {
        this.name = name;
        this.isImported = isImported;
        this.isLocal = isLocal;
    }
}