package mll;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;

public class Div extends BinOp {
    Div(Op lhs, Op rhs) { super(lhs, rhs); }

    public static Op c(Op x, Op y) {
        var dag = x.dag();
        var res = fold(x, y, (a, b) -> a / b);
        if (res != null) return dag.lit(res);

        if (Lit.is(x, 0.f)) return x; // 0 / y = 0
        if (x == y) return dag.lit1(); // x / x = 1

        return dag.unify(new Div(x, y));
    }

    @Override public String opString() { return "/"; }
    @Override protected double eval_(double[] inVals) { return inVals[0] / inVals[1]; }

    @Override protected String llvm_(HashMap<Op, String> map, Writer writer) throws IOException {
        var l = lhs().llvm(map, writer);
        var r = rhs().llvm(map, writer);
        var x = String.format("%%_%d", id());
        writer.append(String.format("\t%s = fdiv double %s, %s\n", x, l, r));
        return x;
    }

    @Override protected Op diff(int inputIdx) {
        // (x / y)' = (x' * y - x * y') / y^2
        var l = inputIdx == 0 ? rhs() : lit0();       // (1 * y - x * 0) / y^2
        var r = inputIdx == 1 ? lhs().neg() : lit0(); // (0 * y - x * 1) / y^2
        return (l.sub(r)).div(rhs().pow(lit2()));
    }
}
