package mll;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    record Output(Op op, int index) {
        @Override public boolean equals(Object obj) {
            return (obj instanceof Output out) && op() == out.op() && index == out.index();
        }

        @Override public int hashCode() { return Objects.hash(op().id(), index()); }
    }

    private HashSet<Output> outputs_ = new HashSet<Output>();

    Op(DAG dag, Op... inputs) {
        dag_    = dag;
        id_     = dag.nextID();
        inputs_ = inputs;
        hash_   = Objects.hash(getClass().hashCode());
        for (int i = 0, e = inputs.length; i != e; ++i) {
            var in = inputs[i];
            hash_ = Objects.hash(hash_, in.id());
            in.outputs_.add(new Output(this, i));
        }
    }

    /*
     * getters
     */

    public DAG dag() { return dag_; }
    public int id() { return id_; }
    public Op[] inputs() { return inputs_; }
    public Op input(int i) { return inputs_[i]; }
    public int numInputs() { return inputs_.length; }
    public HashSet<Output> outputs() { return outputs_; }
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

    public Lit lit(float f) { return dag().lit(f); }
    public Lit lit0() { return lit(0.f); }
    public Lit lit1() { return lit(1.f); }
    public Lit lit2() { return lit(2.f); }

    public Op add(Op y) { return Add.c(this, y); }
    public Op sub(Op y) { return Sub.c(this, y); }
    public Op mul(Op y) { return Mul.c(this, y); }
    public Op pow(Op y) { return Pow.c(this, y); }
    public Op div(Op y) { return Div.c(this, y); }

    public static Op minus(Op x) { return x.lit0().sub(x); }
    public Op minus() { return minus(this); }

    public static Op exp(Op x) { return Exp.c(x); }
    public static Op log(Op x) { return Log.c(x); }
    public static Op sin(Op x) { return Sin.c(x); }
    public static Op cos(Op x) { return Cos.c(x); }
    public Op exp() { return Exp.c(this); }
    public Op log() { return Log.c(this); }
    public Op sin() { return Sin.c(this); }
    public Op cos() { return Cos.c(this); }

    /*
     * helpers for optimizing Ops during construction
     */

    protected static Float fold(Op x, Op y, BinaryOperator<Float> o) {
        if (x instanceof Lit l && y instanceof Lit m) return o.apply(l.get(), m.get());
        return null;
    }

    protected static boolean commute(Op x, Op y) {
        if (x instanceof Lit) return false; // keep Lit on lhs
        if (y instanceof Lit) return true; // ditto
        return x.id() > y.id(); // no Lits? -> smaller id to lhs
    }

    /*
     * Compute free Vars
     */

    TreeSet<Var> freeVars() {
        var vars = new TreeSet<Var>((v, w) -> v.name().compareTo(w.name()));
        freeVars(new HashSet<Op>(), vars);
        return vars;
    }

    void freeVars(HashSet<Op> done, TreeSet<Var> vars) {
        if (done.add(this)) {
            if (this instanceof Var var) {
                vars.add(var);
            } else {
                for (var input : inputs()) input.freeVars(done, vars);
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

    public final float eval(float... values) {
        var vars = freeVars();
        if (vars.size() != values.length)
            throw new IllegalArgumentException("number of provided values does not match number of free variables");

        var env = new HashMap<Op, Float>();
        int i   = 0;
        for (var var : vars) env.put(var, values[i]);
        return eval(env);
    }

    public final float eval(HashMap<Op, Float> env) {
        var res = env.get(this);
        if (res != null) return res;

        var inVals = new float[numInputs()];
        for (int i = 0, e = numInputs(); i != e; ++i) inVals[i] = input(i).eval(env);

        res = eval_(inVals);
        env.put(this, res);
        return res;
    }

    abstract float eval_(float[] inVals);

    /**
     * Writes a DOT-String to disk (as DOT-file).
     */
    public final void dotToFile(String file) throws IOException {
    	BufferedWriter writer = new BufferedWriter(new FileWriter(file));
    	dot(writer);
    }
    
    /**
     * Returns a DOT-String.
     */
    public final String dotToString() throws IOException {
		StringWriter writer = new StringWriter();
    	dot(writer);
    	return writer.toString();
    }

    /*
     * DOT output
     */

    private final void dot(Writer writer) throws IOException {
        writer.append("digraph {\n");
        writer.append("\trankdir=\"TB\"\n");
        dot(new HashMap<Op, String>(), writer);
        writer.append("}\n");
    }
    
    protected final String dot(HashMap<Op, String> map, Writer writer) throws IOException {
        var res = map.get(this);
        if (res != null) return res;
        res = dot_(map, writer);
        map.put(this, res);
        return res;
    }

    protected String dot_(HashMap<Op, String> map, Writer writer) throws IOException {
        var dst = String.format("_%d", id());
        writer.append(String.format("\t%s[label=\"%s\"];\n", dst, opString()));
        for (var in : inputs()) {
            var src = in.dot(map, writer);
            writer.append(String.format("\t%s -> %s;\n", src, dst));
        }
        return dst;
    }

    public String opString() { return numInputs() == 0 ? toString() : getClass().getSimpleName().toLowerCase(); }

    public final String llvmToString() throws IOException {
		StringWriter writer = new StringWriter();
		llvm(writer);
		return writer.toString();
	}
    
    public final void llvmToFile(String filename) throws IOException {
    	
    	Files.createDirectories(Paths.get("llvm"));
    	
    	BufferedWriter writer = new BufferedWriter(
    			new FileWriter(Paths.get("llvm", filename + ".ll").toString())
    			);
    	llvm(writer);
    	writer.close();
    }

	/*
     * LLVM output
     */
    
    private final void llvm(Writer writer) throws IOException {
        // declare LLVM intrinsics we might use
        writer.append("declare float @llvm.pow.f32(float %Val, float %Power)\n");
        writer.append("declare float @llvm.log.f32(float %Val)\n");
        writer.append("declare float @llvm.exp.f32(float %Val)\n");
        writer.append("declare float @llvm.sin.f32(float %Val)\n");
        writer.append("declare float @llvm.cos.f32(float %Val)\n");

        var resT = (this instanceof Grad grad) ? grad.llvmType() : "float";

        // mll signature
        writer.append(String.format("define %s @mll(", resT));
        String sep = "";
        for (var var : freeVars()) {
            writer.append(String.format("%sfloat %%%s", sep, var));
            sep = ", ";
        }
        writer.append(") { \n");

        // body + ret
        var resV = llvm(new HashMap<Op, String>(), writer);
        writer.append(String.format("\tret %s %s\n", resT, resV));
        writer.append("}\n");
    }
    
    protected final String llvm(HashMap<Op, String> map, Writer writer) throws IOException {
        var res = map.get(this);
        if (res != null) return res;
        res = llvm_(map, writer);
        map.put(this, res);
        return res;
    }

    protected abstract String llvm_(HashMap<Op, String> map, Writer writer) throws IOException;

    /*
     * Backpropagation
     */

    public Grad backwards() {
        var vars       = freeVars();
        var map        = new HashMap<Op, Op>();
        var gradInputs = new Op[vars.size() + 1];
        gradInputs[0] = this;
        int i = 1;
        for (var var : vars) gradInputs[i++] = var.backwards(map, this);
        return new Grad(gradInputs);
    }

    protected Op backwards(HashMap<Op, Op> map, Op res) {
        if (this == res) return lit1();

        var adjoint = map.get(this);
        if (adjoint != null) return adjoint;

        adjoint = lit0();
        for (var output : new HashSet<Output>(outputs())) { // copy as we may introduce more outputs along the way
            var out        = output.op();
            var out_i      = output.index();
            var out_a      = out.backwards(map, res);
            var dout_dthis = out.diff(out_i);
            adjoint = adjoint.add(out_a.mul(dout_dthis));
        }

        map.put(this, adjoint);
        return adjoint;
    }

    abstract protected Op diff(int inputIdx);
}

class Lit extends Op {
    private float f_;

    Lit(DAG dag, float f) {
        super(dag);
        f_    = f;
        hash_ = Objects.hash(hash_, f);
    }

    public float get() { return f_; }
    public boolean is(float f) { return f == f_; }
    public static boolean is(Op e, float f) { return (e instanceof Lit l) && l.is(f); }
    @Override public String toString() { return Float.toString(get()); }
    @Override public boolean equals(Object obj) { return super.equals(obj) && get() == ((Lit) obj).get(); }
    @Override protected float eval_(float[] inVals) { return f_; }
    @Override protected String llvm_(HashMap<Op, String> map, Writer writer) throws IOException {
        return Float.toString(get());
    }
    @Override protected Op diff(int inputIdx) { return lit0(); }
}

class Var extends Op {
    private String name_;

    Var(DAG dag, String name) {
        super(dag);
        name_ = name;
        hash_ = Objects.hash(hash_, name);
    }

    public String name() { return name_; }
    public @Override String toString() { return name(); }
    @Override public boolean equals(Object obj) { return super.equals(obj) && name().equals(((Var) obj).name()); }
    @Override protected float eval_(float[] inVals) { throw new IllegalArgumentException(); }
    @Override protected String llvm_(HashMap<Op, String> map, Writer writer) throws IOException {
        return String.format("%%%s", name());
    }
    @Override protected Op diff(int inputIdx) { return null; }
}

abstract class UnOp extends Op {
    UnOp(Op arg) { super(arg.dag(), arg); }

    public Op arg() { return input(0); }
    @Override public String toString() { return String.format("(%s(%s))", opString(), arg()); }
}

class Exp extends UnOp {
    Exp(Op arg) { super(arg); }

    public static Op c(Op x) {
        var dag = x.dag();
        return dag.unify(new Exp(x));
    }

    @Override protected float eval_(float[] inVals) { return (float) Math.exp(inVals[0]); }

    @Override protected String llvm_(HashMap<Op, String> map, Writer writer) throws IOException {
        var a = arg().llvm(map, writer);
        var x = String.format("%%_%d", id());
        writer.append(String.format("\t%s = call float @llvm.exp.f32(float %s)\n", x, a));
        return x;
    }

    @Override protected Op diff(int inputIdx) { return this; }
}

class Log extends UnOp {
    Log(Op arg) { super(arg); }

    public static Op c(Op x) {
        var dag = x.dag();
        return dag.unify(new Log(x));
    }

    @Override protected float eval_(float[] inVals) { return (float) Math.log(inVals[0]); }

    @Override protected String llvm_(HashMap<Op, String> map, Writer writer) throws IOException {
        var a = arg().llvm(map, writer);
        var x = String.format("%%_%d", id());
        writer.append(String.format("\t%s = call float @llvm.log.f32(float %s)\n", x, a));
        return x;
    }

    @Override protected Op diff(int inputIdx) { return lit1().div(arg()); }
}

class Sin extends UnOp {
    Sin(Op arg) { super(arg); }

    public static Op c(Op x) {
        var dag = x.dag();
        return dag.unify(new Sin(x));
    }

    @Override protected float eval_(float[] inVals) { return (float) Math.sin(inVals[0]); }

    @Override protected String llvm_(HashMap<Op, String> map, Writer writer) throws IOException {
        var a = arg().llvm(map, writer);
        var x = String.format("%%_%d", id());
        writer.append(String.format("\t%s = call float @llvm.sin.f32(float %s)\n", x, a));
        return x;
    }

    @Override protected Op diff(int inputIdx) { return cos(arg()); }
}

class Cos extends UnOp {
    Cos(Op arg) { super(arg); }

    public static Op c(Op x) {
        var dag = x.dag();
        return dag.unify(new Cos(x));
    }

    @Override protected float eval_(float[] inVals) { return (float) Math.cos(inVals[0]); }

    @Override protected String llvm_(HashMap<Op, String> map, Writer writer) throws IOException {
        var a = arg().llvm(map, writer);
        var x = String.format("%%_%d", id());
        writer.append(String.format("\t%s = call float @llvm.cos.f32(float %s)\n", x, a));
        return x;
    }

    @Override protected Op diff(int inputIdx) { return minus(sin(arg())); }
}

abstract class BinOp extends Op {
    BinOp(Op lhs, Op rhs) { super(lhs.dag(), lhs, rhs); }

    public Op lhs() { return input(0); }
    public Op rhs() { return input(1); }
    @Override public String toString() { return String.format("(%s %s %s)", lhs(), opString(), rhs()); }
}

class Add extends BinOp {
    Add(Op lhs, Op rhs) { super(lhs, rhs); }

    public static Op c(Op x, Op y) {
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
        var minus = Sub.isaMinus(y);
        if (minus == x) return dag.lit0(); // x + -x = 0

        return dag.unify(new Add(x, y));
    }

    @Override public String opString() { return "+"; }
    @Override protected float eval_(float[] inVals) { return inVals[0] + inVals[1]; }

    @Override protected String llvm_(HashMap<Op, String> map, Writer writer) throws IOException {
        var l = lhs().llvm(map, writer);
        var r = rhs().llvm(map, writer);
        var x = String.format("%%_%d", id());
        writer.append(String.format("\t%s = fadd float %s, %s\n", x, l, r));
        return x;
    }

    @Override protected Op diff(int inputIdx) { return lit1(); }
}

class Sub extends BinOp {
    Sub(Op lhs, Op rhs) { super(lhs, rhs); }

    public static Op c(Op x, Op y) {
        var dag = x.dag();
        var res = fold(x, y, (a, b) -> a - b);
        if (res != null) return dag.lit(res);

        if (y instanceof Lit m) return x.add(dag.lit(-m.get())); // x - m = x + -m
        if (x == y) return dag.lit0();

        return dag.unify(new Sub(x, y));
    }

    /// @returns `y` in `0 - y` otherwise `null`.
    public static Op isaMinus(Op e) { return (e instanceof Sub sub) && Lit.is(sub.lhs(), 0.f) ? sub.rhs() : null; }

    @Override public String opString() { return "-"; }
    @Override protected float eval_(float[] inVals) { return inVals[0] - inVals[1]; }

    @Override protected String llvm_(HashMap<Op, String> map, Writer writer) throws IOException {
        var l = lhs().llvm(map, writer);
        var r = rhs().llvm(map, writer);
        var x = String.format("%%_%d", id());
        writer.append(String.format("\t%s = fsub float %s, %s\n", x, l, r));
        return x;
    }

    @Override protected Op diff(int inputIdx) { return inputIdx == 0 ? lit1() : lit(-1.f); }
}

class Mul extends BinOp {
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
            if (l.is(0.f)) return l;
            if (l.is(1.f)) return y;
        }
        if (x == y) return x.pow(dag.lit2());

        return dag.unify(new Mul(x, y));
    }

    @Override public String opString() { return "*"; }
    @Override protected float eval_(float[] inVals) { return inVals[0] * inVals[1]; }

    @Override protected String llvm_(HashMap<Op, String> map, Writer writer) throws IOException {
        var l = lhs().llvm(map, writer);
        var r = rhs().llvm(map, writer);
        var x = String.format("%%_%d", id());
        writer.append(String.format("\t%s = fmul float %s, %s\n", x, l, r));
        return x;
    }

    @Override protected Op diff(int inputIdx) {
        // (x * y)' = x' * y + x * y'
        var l = inputIdx == 0 ? rhs() : lit0(); // 1 * y + x * 0
        var r = inputIdx == 1 ? lhs() : lit0(); // 0 * y + x * 1
        return l.add(r);
    }
}

class Div extends BinOp {
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
    @Override protected float eval_(float[] inVals) { return inVals[0] / inVals[1]; }

    @Override protected String llvm_(HashMap<Op, String> map, Writer writer) throws IOException {
        var l = lhs().llvm(map, writer);
        var r = rhs().llvm(map, writer);
        var x = String.format("%%_%d", id());
        writer.append(String.format("\t%s = fdiv float %s, %s\n", x, l, r));
        return x;
    }

    @Override protected Op diff(int inputIdx) {
        // (x / y)' = (x' * y - x * y') / y^2
        var l = inputIdx == 0 ? rhs() : lit0();        // (1 * y - x * 0) / y^2
        var r = inputIdx == 1 ? minus(lhs()) : lit0(); // (0 * y - x * 1) / y^2
        return (l.sub(r)).div(rhs().pow(lit2()));
    }
}

class Pow extends BinOp {
    Pow(Op base, Op exponent) { super(base, exponent); }

    public static Op c(Op x, Op y) {
        var dag = x.dag();
        var res = fold(x, y, (a, b) -> (float) Math.pow((double) a, (double) b));
        if (res != null) return dag.lit(res);

        if (Lit.is(y, 0.f)) return dag.lit1(); // x^0 = 1
        if (Lit.is(y, 1.f)) return x; // x^1 = x

        return dag.unify(new Pow(x, y));
    }

    public Op base() { return lhs(); }
    public Op exponent() { return rhs(); }
    @Override public String opString() { return "^"; }

    @Override protected float eval_(float[] inVals) { return (float) Math.pow(inVals[0], inVals[1]); }

    @Override protected String llvm_(HashMap<Op, String> map, Writer writer) throws IOException {
        var l = lhs().llvm(map, writer);
        var r = rhs().llvm(map, writer);
        var x = String.format("%%_%d", id());
        writer.append(String.format("\t%s = call float @llvm.pow.f32(float %s, float %s)\n", x, l, r));
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

class Grad extends Op {
    private float[] result_; // cached result after eval

    Grad(Op... inputs) { super(inputs[0].dag(), inputs); }

    float[] result() { return result_; }

    public @Override String toString() {
        var res = "{";
        var sep = "";
        for (var input : inputs()) {
            res += sep + input;
            sep  = ", ";
        }
        return res + "}";
    }

    @Override protected float eval_(float[] inVals) {
        result_ = inVals;
        return result_[0];
    }

    String llvmType() {
        var res = "";
        res += "{";
        var sep = "";
        for (int i = 0, e = numInputs(); i != e; ++i) {
            res += sep + "float";
            sep  = ", ";
        }
        res += "}";
        return res;
    }

    @Override protected String llvm_(HashMap<Op, String> map, Writer writer) throws IOException {
        var type = llvmType();
        var curr = "undef";
        for (int i = 0, e = numInputs(); i != e; ++i) {
            var next = String.format("%%res%d", i);
            var lli  = input(i).llvm(map, writer);
            writer.append(String.format("\t%s = insertvalue %s %s, float %s, %d\n", next, type, curr, lli, i));
            curr = next;
        }
        return curr;
    }

    @Override protected Op diff(int inputIdx) { throw new IllegalArgumentException(); }
}
