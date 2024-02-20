package mll;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;

public class Pow extends BinOp {
    Pow(Op base, Op exponent) { super(base, exponent); }

    public static Op c(Op x, Op y) {
        var dag = x.dag();
        var res = fold(x, y, (a, b) -> (double) Math.pow((double) a, (double) b));
        if (res != null) return dag.lit(res);

        if (Lit.is(y, 0.f)) return dag.lit1(); // x^0 = 1
        if (Lit.is(y, 1.f)) return x; // x^1 = x

        return dag.unify(new Pow(x, y));
    }

    public Op base() { return lhs(); }
    public Op exponent() { return rhs(); }
    @Override public String opString() { return "^"; }

    @Override protected double eval_(double[] inVals) { return (double) Math.pow(inVals[0], inVals[1]); }

    @Override protected String llvm_(HashMap<Op, String> map, Writer writer) throws IOException {
        var l = lhs().llvm(map, writer);
        var r = rhs().llvm(map, writer);
        var x = String.format("%%_%d", id());
        writer.append(String.format("\t%s = call double @llvm.pow.f64(double %s, double %s)\n", x, l, r));
        return x;
    }

    @Override protected Op diff(int inputIdx) {
        if (rhs() instanceof Lit l) {
            if (inputIdx == 1) return lit0();
            return l.mul(lhs().pow(lit(l.get() - 1.f)));
        }
        return null; // TODO
    }
}
