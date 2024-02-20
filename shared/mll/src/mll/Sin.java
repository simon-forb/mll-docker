package mll;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;

public class Sin extends UnOp {
    Sin(Op arg) { super(arg); }

    public static Op c(Op x) {
        var dag = x.dag();
        if (x instanceof Lit lit) return dag.lit(Math.sin(lit.get()));
        return dag.unify(new Sin(x));
    }

    @Override protected double eval_(double[] inVals) { return (double) Math.sin(inVals[0]); }

    @Override protected String llvm_(HashMap<Op, String> map, Writer writer) throws IOException {
        var a = arg().llvm(map, writer);
        var x = String.format("%%_%d", id());
        writer.append(String.format("\t%s = call double @llvm.sin.f64(double %s)\n", x, a));
        return x;
    }

    @Override protected Op diff(int inputIdx) { return cos(arg()); }
}
