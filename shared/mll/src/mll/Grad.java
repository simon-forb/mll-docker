package mll;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;

public class Grad extends Op {
    private double[] result_; // cached result after eval
    private Var[]    vars_;

    Grad(Op[] inputs, Var[] vars) {
        super(inputs[0].dag(), inputs);
        vars_ = vars;
    }

    public static Grad c(Op[] inputs, Var[] vars) { return (Grad) inputs[0].dag().unify(new Grad(inputs, vars)); }

    double[] result() { return result_; }
    Var[] vars() { return vars_; }

    public @Override String toString() {
        var res = "{";
        var sep = "";
        for (var input : inputs()) {
            res += sep + input;
            sep  = ", ";
        }
        return res + "}";
    }

    @Override protected double eval_(double[] inVals) {
        result_ = inVals;
        return result_[0];
    }

    @Override protected String llvm_(HashMap<Op, String> map, Writer writer) throws IOException {
        throw new IllegalArgumentException("You can only emit a store via llvm_store.");
    }

    @Override protected void llvm_store(HashMap<Op, String> map, Writer writer) throws IOException {
        int n          = numInputs();
        var llvmInputs = new String[n];
        for (int i = 0; i != n; ++i) llvmInputs[i] = input(i).llvm(map, writer);

        for (int i = 0, e = numInputs(); i != e; ++i) {
            var gep   = String.format("\t%%_output%d = getelementptr inbounds double, ptr %%_output, i64 %d\n", i, i);
            var store = String.format("\tstore double %s, ptr %%_output%d\n", llvmInputs[i], i);
            writer.append(gep + store);
        }
    }

    @Override protected Op diff(int inputIdx) {
        throw new IllegalArgumentException("A Grad Op already holds the derivatives.");
    }
}
