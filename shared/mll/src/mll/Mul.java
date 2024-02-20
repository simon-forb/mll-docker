package mll;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;

public class Mul extends BinOp {
    Mul(Op lhs, Op rhs) { super(lhs, rhs); }

    public static Op c(Op x, Op y) {
        var dag = x.dag();
        var res = fold(x, y, (a, b) -> a * b);
        if (res != null) return dag.lit(res);
        if (commute(x, y)) {
            var t = x; // swap
            x = y;
            y = t;
        }

        if (x instanceof Lit l) {
            if (l.is(0)) return l;
            if (l.is(1)) return y;
            if (l.is(-1)) return y.neg();
        }
        if (x == y) return x.pow(dag.lit2());

        var reassoc = reassociate(Mul.class, x, y, (a, b) -> Mul.c(a, b));
        if (reassoc != null) return reassoc;

        return dag.unify(new Mul(x, y));
    }

    @Override public String opString() { return "*"; }
    @Override protected double eval_(double[] inVals) { return inVals[0] * inVals[1]; }

    @Override protected String llvm_(HashMap<Op, String> map, Writer writer) throws IOException {
        var l = lhs().llvm(map, writer);
        var r = rhs().llvm(map, writer);
        var x = String.format("%%_%d", id());
        writer.append(String.format("\t%s = fmul double %s, %s\n", x, l, r));
        return x;
    }

    @Override protected Op diff(int inputIdx) {
        // (x * y)' = x' * y + x * y'
        var l = inputIdx == 0 ? rhs() : lit0(); // 1 * y + x * 0
        var r = inputIdx == 1 ? lhs() : lit0(); // 0 * y + x * 1
        return l.add(r);
    }
}
