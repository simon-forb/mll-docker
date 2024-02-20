package mll;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.BinaryOperator;

public abstract class Op {
    protected int hash_;
    private DAG   dag_;
    private int   id_;
    private Op[]  inputs_;

    Op(DAG dag, Op... inputs) {
        dag_    = dag;
        id_     = dag.nextID();
        inputs_ = inputs;
        hash_   = Objects.hash(getClass().hashCode());
        for (var input : inputs) hash_ = Objects.hash(hash_, input.id());
    }

    /*
     * getters
     */

    public DAG dag() { return dag_; }
    public int id() { return id_; }
    public Op[] inputs() { return inputs_; }
    public Op input(int i) { return inputs_[i]; }
    public int numInputs() { return inputs_.length; }
    @Override public int hashCode() { return hash_; }

    @Override public boolean equals(Object obj) {
        boolean result = getClass() == obj.getClass();
        if (result) {
            var other = (Op) obj;
            result &= numInputs() == other.numInputs();
            for (int i = 0, e = numInputs(); i != e && result; ++i) result &= input(i) == other.input(i);
        }
        return result;
    }

    /*
     * factory methods to construct Ops
     */

    public Lit lit(double f) { return dag().lit(f); }
    public Lit lit0() { return lit(0.f); }
    public Lit lit1() { return lit(1.f); }
    public Lit lit2() { return lit(2.f); }

    public Op add(Op y) { return Add.c(this, y); }
    public Op sub(Op y) { return Sub.c(this, y); }
    public Op mul(Op y) { return Mul.c(this, y); }
    public Op pow(Op y) { return Pow.c(this, y); }
    public Op div(Op y) { return Div.c(this, y); }

    public static Op neg(Op x) { return Neg.c(x); }
    public static Op exp(Op x) { return Exp.c(x); }
    public static Op log(Op x) { return Log.c(x); }
    public static Op sin(Op x) { return Sin.c(x); }
    public static Op cos(Op x) { return Cos.c(x); }

    public Op neg() { return Neg.c(this); }
    public Op exp() { return Exp.c(this); }
    public Op log() { return Log.c(this); }
    public Op sin() { return Sin.c(this); }
    public Op cos() { return Cos.c(this); }

    /*
     * helpers for optimizing Ops during construction
     */

    protected static Double fold(Op x, Op y, BinaryOperator<Double> o) {
        if (x instanceof Lit l && y instanceof Lit m) return o.apply(l.get(), m.get());
        return null;
    }

    protected static boolean commute(Op x, Op y) {
        if (x instanceof Lit) return false; // keep Lit on lhs
        if (y instanceof Lit) return true; // ditto
        return x.id() > y.id(); // no Lits? -> smaller id to lhs
    }

    /// Re-associates @p x and @p y according to following rules.
    /// We use the following naming convention (Lit%erals are prefixed with `l`):
    /// @formatter:off
    /// ```
    ///     x    op     y
    /// (a op b) op (c op d)
    ///
    /// (1)     lx    op (lc op d) -> (lx op lc) op     d
    /// (2) (la op b) op (lc op d) -> (la op lc) op (b op d)
    /// (3)      x    op (lc op d) ->     lc     op (x op d)
    /// (4) (la op b) op      y    ->     la     op (b op y)
    /// ```
    /// @formatter:on
    static Op reassociate(Class<? extends BinOp> cls, Op x, Op y, BinaryOperator<Op> mk) {
        var ab = x.getClass() == cls ? (BinOp) x : null;
        var cd = y.getClass() == cls ? (BinOp) y : null;
        var a  = ab != null ? ab.lhs() : null;
        var b  = ab != null ? ab.rhs() : null;
        var c  = cd != null ? cd.lhs() : null;
        var d  = cd != null ? cd.rhs() : null;
        var lx = (x instanceof Lit) ? (Lit) x : null;
        var la = a != null && (a instanceof Lit) ? (Lit) a : null;
        var lc = c != null && (c instanceof Lit) ? (Lit) c : null;

        // @formatter:off
        if (lx != null && lc != null) return mk.apply(mk.apply(lx, lc),              d); // (1)
        if (la != null && lc != null) return mk.apply(mk.apply(la, lc), mk.apply(b, d)); // (2)
        if (              lc != null) return mk.apply(              lc, mk.apply(x, d)); // (3)
        if (la != null              ) return mk.apply(              la, mk.apply(b, y)); // (4)
        // @formatter:on

        return null;
    }

    /*
     * Compute free Vars and outputs
     */

    record Output(Op op, int index) {
        @Override public boolean equals(Object obj) {
            return (obj instanceof Output out) && op() == out.op() && index == out.index();
        }

        @Override public int hashCode() { return Objects.hash(op().id(), index()); }
    }

    Var[] freeVars() { return freeVars(null); }

    Var[] freeVars(HashMap<Op, HashSet<Output>> outputs) {
        var vars = new TreeSet<Var>((v, w) -> v.name().compareTo(w.name()));
        freeVars(new HashSet<Op>(), vars, outputs);
        return vars.toArray(new Var[vars.size()]);
    }

    void freeVars(HashSet<Op> done, TreeSet<Var> vars, HashMap<Op, HashSet<Output>> outputs) {
        if (done.add(this)) {
            if (this instanceof Var var) {
                vars.add(var);
            } else {
                for (int i = 0, e = numInputs(); i != e; ++i) {
                    var input = input(i);
                    if (outputs != null) {
                        if (!outputs.containsKey(input)) outputs.put(input, new HashSet<Output>());
                        outputs.get(input).add(new Output(this, i));
                    }
                    input.freeVars(done, vars, outputs);
                }
            }
        }
    }

    void emit(String file) throws IOException {
        try (var writer = new BufferedWriter(new FileWriter(file))) {
            writer.append(toString());
        }
    }

    /*
     * Eval
     */

    public final double eval(double... values) {
        var vars = freeVars();
        if (vars.length != values.length)
            throw new IllegalArgumentException("number of provided values does not match number of free variables");

        var env = new HashMap<Op, Double>();
        for (int i = 0, e = vars.length; i != e; ++i) env.put(vars[i], values[i]);
        return eval(env);
    }

    public final double eval(HashMap<Op, Double> env) {
        var res = env.get(this);
        if (res != null) return res;

        var inVals = new double[numInputs()];
        for (int i = 0, e = numInputs(); i != e; ++i) inVals[i] = input(i).eval(env);

        res = eval_(inVals);
        env.put(this, res);
        return res;
    }

    abstract double eval_(double[] inVals);

    /*
     * DOT output
     */

    public final String dot() throws IOException { return dot(new HashMap<Op, Double>()); }

    public final String dot(HashMap<Op, Double> env) throws IOException {
        var writer = new StringWriter();
        writer.append("digraph {\n");
        writer.append("\trankdir=\"TB\"\n");
        dot(new HashMap<Op, String>(), env, writer);
        writer.append("}\n");
        return writer.toString();
    }

    protected final String dot(HashMap<Op, String> map, HashMap<Op, Double> env, Writer writer) throws IOException {
        var res = map.get(this);
        if (res != null) return res;
        res = dot_(map, env, writer);
        map.put(this, res);
        return res;
    }

    protected String dot_(HashMap<Op, String> map, HashMap<Op, Double> env, Writer writer) throws IOException {
        var dst = String.format("_%d", id());
        var val = !(this instanceof Lit) && !(this instanceof Grad) && env.containsKey(this)
                ? "\\n" + env.get(this).toString()
                : "";
        var col = (this instanceof Var) ? color() : "";

        writer.append(String.format("\t%s[label=\"%s%s\",%s];\n", dst, opString(), val, col));
        for (int i = 0, e = numInputs(); i != e; ++i) {
            var in   = input(i);
            var src  = in.dot(map, env, writer);
            var attr = this instanceof Grad grad && i >= 1 ? grad.vars()[i - 1].color() : "";
            writer.append(String.format("\t%s -> %s[%s];\n", src, dst, attr));
        }
        return dst;
    }

    public double hue() { return (double) (id_ % 16) / 16.0; }

    public String color() {
        var res = "";
        res += String.format("color=\"%f .5 .75\",", hue());
        return res + String.format("style=filled,fillcolor=\"%f .5 .75\"", hue());
    }

    public String opString() { return numInputs() == 0 ? toString() : getClass().getSimpleName().toLowerCase(); }

    /*
     * LLVM output
     */

    public final void llvm(String file) throws IOException {
        try (var writer = new BufferedWriter(new FileWriter(file))) {
            // declare LLVM intrinsics we might use
            writer.append("declare double @llvm.pow.f64(double %Val, double %Power)\n");
            writer.append("declare double @llvm.log.f64(double %Val)\n");
            writer.append("declare double @llvm.exp.f64(double %Val)\n");
            writer.append("declare double @llvm.sin.f64(double %Val)\n");
            writer.append("declare double @llvm.cos.f64(double %Val)\n");
            writer.newLine();

            // mll signature
            writer.append("define void @mll(ptr noundef noalias %_input, ptr noundef noalias %_output) {\n");

            // load vars
            var map = new HashMap<Op, String>();
            int i   = 0;
            for (var var : freeVars()) {
                var name = String.format("%%%s", var);
                writer.append(String.format("\t%%_in%d = getelementptr inbounds double, ptr %%_input, i64 %d\n", i, i));
                writer.append(String.format("\t%s = load double, ptr %%_in%d\n", name, i));
                map.put(var, name);
                ++i;
            }

            // emit final store and recursively the body to compute it
            llvm_store(map, writer);

            // ret void
            writer.append("\tret void\n");
            writer.append("}\n");
        }
    }

    protected final String llvm(HashMap<Op, String> map, Writer writer) throws IOException {
        var res = map.get(this);
        if (res != null) return res;
        res = llvm_(map, writer);
        map.put(this, res);
        return res;
    }

    protected void llvm_store(HashMap<Op, String> map, Writer writer) throws IOException {
        var res = llvm(map, writer);
        writer.append(String.format("\tstore double %s, ptr %%_output\n", res));
    }

    protected abstract String llvm_(HashMap<Op, String> map, Writer writer) throws IOException;

    /*
     * Backpropagation
     */

    public Grad backwards() {
        var outputs    = new HashMap<Op, HashSet<Output>>();
        var vars       = freeVars(outputs);
        var map        = new HashMap<Op, Op>();
        var gradInputs = new Op[vars.length + 1];
        gradInputs[0] = this;
        for (int i = 0, e = vars.length; i != e; ++i) gradInputs[i + 1] = vars[i].backwards(map, outputs, this);
        return Grad.c(gradInputs, vars);
    }

    protected Op backwards(HashMap<Op, Op> map, HashMap<Op, HashSet<Output>> outputs, Op res) {
        if (this == res) return lit1();

        var adjoint = map.get(this);
        if (adjoint != null) return adjoint;

        adjoint = lit0();
        for (var output : outputs.get(this)) {
            var out        = output.op();
            var out_i      = output.index();
            var out_a      = out.backwards(map, outputs, res);
            var dout_dthis = out.diff(out_i);
            adjoint = adjoint.add(out_a.mul(dout_dthis));
        }

        map.put(this, adjoint);
        return adjoint;
    }

    abstract protected Op diff(int inputIdx);
}

abstract class UnOp extends Op {
    UnOp(Op arg) { super(arg.dag(), arg); }

    public Op arg() { return input(0); }
    @Override public String toString() { return String.format("(%s(%s))", opString(), arg()); }
}

abstract class BinOp extends Op {
    BinOp(Op lhs, Op rhs) { super(lhs.dag(), lhs, rhs); }

    public Op lhs() { return input(0); }
    public Op rhs() { return input(1); }
    @Override public String toString() { return String.format("(%s %s %s)", lhs(), opString(), rhs()); }
}
