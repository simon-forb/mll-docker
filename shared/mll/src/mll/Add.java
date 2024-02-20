package mll;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;

public class Add extends BinOp {
    Add(Op lhs, Op rhs) { super(lhs, rhs); }

    public static Op c(Op x, Op y) {
        // BEGIN_SOLUTION
        var dag = x.dag();
        var res = fold(x, y, (a, b) -> a + b);
        if (res != null) return dag.lit(res);
        if (commute(x, y)) {
            var t = x; // swap
            x = y;
            y = t;
        }

        if (Lit.is(x, 0.f)) return y; // 0 + y = y
        if (x == y) return dag.lit2().mul(x); // x + x = 2 * x

        if (x instanceof Neg neg) {
            if (y == neg.arg()) return dag.lit0(); // -y + y = 0
            return y.sub(neg.arg()); // -x + y = y - x
        }

        if (y instanceof Neg neg) {
            if (x == neg.arg()) return dag.lit0(); // x + -x = 0
            return x.sub(neg.arg()); // x + -y = x - y
        }

        var reassoc = reassociate(Add.class, x, y, (a, b) -> Add.c(a, b));
        if (reassoc != null) return reassoc;

        // END_SOLUTION
        return dag.unify(new Add(x, y));
    }

    @Override public String opString() { return "+"; }
    @Override protected double eval_(double[] inVals) { return inVals[0] + inVals[1]; }

    @Override protected String llvm_(HashMap<Op, String> map, Writer writer) throws IOException {
        var l = lhs().llvm(map, writer);
        var r = rhs().llvm(map, writer);
        var x = String.format("%%_%d", id());
        writer.append(String.format("\t%s = fadd double %s, %s\n", x, l, r));
        return x;
    }

    @Override protected Op diff(int inputIdx) { return lit1(); }
}
