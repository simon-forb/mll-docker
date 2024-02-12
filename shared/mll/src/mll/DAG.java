package mll;

import java.util.HashMap;

public class DAG {
    public Var var(String name) { return (Var) unify(new Var(this, name)); }

    public Var x() { return var("x"); }
    public Var y() { return var("y"); }
    public Var z() { return var("z"); }

    public Lit lit(float f) {
        if (f == -0.f) f = 0.f; // ignore -0.f
        return (Lit) unify(new Lit(this, f));
    }

    public Lit lit0() { return lit(0.f); }
    public Lit lit1() { return lit(1.f); }
    public Lit lit2() { return lit(2.f); }

    int nextID() { return id_++; }

    Op unify(Op key) {
        if (ops_.containsKey(key)) return ops_.get(key);
        ops_.put(key, key);
        return key;
    }

    private int             id_  = 0;
    private HashMap<Op, Op> ops_ = new HashMap<Op, Op>();
}
