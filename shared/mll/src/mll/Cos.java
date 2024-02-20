package mll;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;

public class Cos extends UnOp {
    Cos(Op arg) { super(arg); }

    public static Op c(Op x) {
        var dag = x.dag();
        if (x instanceof Lit lit) return dag.lit(Math.cos(lit.get()));
        return dag.unify(new Cos(x));
    }

    @Override protected double eval_(double[] inVals) { return (double) Math.cos(inVals[0]); }

    @Override protected String llvm_(HashMap<Op, String> map, Writer writer) throws IOException {
        var a = arg().llvm(map, writer);
        var x = String.format("%%_%d", id());
        writer.append(String.format("\t%s = call double @llvm.cos.f64(double %s)\n", x, a));
        return x;
    }

    @Override protected Op diff(int inputIdx) { return sin(arg()).neg(); }
}
