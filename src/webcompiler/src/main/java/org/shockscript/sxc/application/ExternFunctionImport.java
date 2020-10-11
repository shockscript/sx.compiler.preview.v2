package com.recoyx.sxc.application;

public final class ExternFunctionImport
{
    public String importModule;

    public String importFunction;

    public int functionIndex;

    public ExternFunctionImport(String importModule, String importFunction, int functionIndex)
    {
        this.importModule = importModule;
        this.importFunction = importFunction;
        this.functionIndex = functionIndex;
    }
}