package mll;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;

public class Sub extends BinOp {
    Sub(Op lhs, Op rhs) { super(lhs, rhs); }

    public static Op c(Op x, Op y) {
        var dag = x.dag();
        var res = fold(x, y, (a, b) -> a - b);
        if (res != null) return dag.lit(res);

        if (x == y) return dag.lit0(); // x - x = 0
        if (x instanceof Lit lit && lit.is(0)) return y.neg(); // 0 - y = -y
        if (y instanceof Lit lit) return x.add(dag.lit(-lit.get())); // x - lit = x + -lit
        if (y instanceof Neg neg) return x.add(neg.arg()); // x - -y = x + y

        return dag.unify(new Sub(x, y));
    }

    @Override public String opString() { return "-"; }
    @Override protected double eval_(double[] inVals) { return inVals[0] - inVals[1]; }

    @Override protected String llvm_(HashMap<Op, String> map, Writer writer) throws IOException {
        var l = lhs().llvm(map, writer);
        var r = rhs().llvm(map, writer);
        var x = String.format("%%_%d", id());
        writer.append(String.format("\t%s = fsub double %s, %s\n", x, l, r));
        return x;
    }

    @Override protected Op diff(int inputIdx) { return inputIdx == 0 ? lit1() : lit(-1.f); }
}
