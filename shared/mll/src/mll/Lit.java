package mll;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Objects;

public class Lit extends Op {
    private double f_;

    Lit(DAG dag, double f) {
        super(dag);
        f_    = f;
        hash_ = Objects.hash(hash_, f);
    }

    public double get() { return f_; }
    public boolean is(double f) { return f == f_; }
    public static boolean is(Op e, double f) { return (e instanceof Lit l) && l.is(f); }
    @Override public String toString() { return Double.toString(get()); }
    @Override public boolean equals(Object obj) { return super.equals(obj) && get() == ((Lit) obj).get(); }
    @Override protected double eval_(double[] inVals) { return f_; }
    @Override protected String llvm_(HashMap<Op, String> map, Writer writer) throws IOException {
        return Double.toString(get());
    }
    @Override protected Op diff(int inputIdx) { return lit0(); }
}
