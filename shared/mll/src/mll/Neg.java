package mll;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;

public class Neg extends UnOp {
    Neg(Op arg) { super(arg); }

    public static Op c(Op x) {
        var dag = x.dag();
        if (x instanceof Lit l) return dag.lit(-l.get());
        if (x instanceof Neg neg) return neg.arg(); // --x = x
        return dag.unify(new Neg(x));
    }

    @Override public String toString() { return String.format("-(%s)", arg()); }
    @Override protected double eval_(double[] inVals) { return -inVals[0]; }

    @Override protected String llvm_(HashMap<Op, String> map, Writer writer) throws IOException {
        var a = arg().llvm(map, writer);
        var x = String.format("%%_%d", id());
        writer.append(String.format("\t%s = fsub double 0.0, %s\n", x, a));
        return x;
    }

    @Override protected Op diff(int inputIdx) { return lit(-1); }
}
