package com.recoyx.sxc.application.outputstructures;

import org.hydroper.wasm.WASMOutput;
import org.hydroper.wasm.enums.TypeKind;

public final class OutputFunction
{
    /**
     * If the OutFunction represents a virtual method,
     * this property indicates its dynamic-dispatch-free version.
     */
    public OutputFunction ddFree;

    public Vector<TypeKind> localTypes = new Vector<>();

    public WASMOutput code = new WASMOutput();
}