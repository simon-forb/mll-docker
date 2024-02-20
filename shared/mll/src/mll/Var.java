package mll;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Objects;

public class Var extends Op {
    private String name_;

    Var(DAG dag, String name) {
        super(dag);
        name_ = name;
        hash_ = Objects.hash(hash_, name);
    }

    public String name() { return name_; }
    public @Override String toString() { return name(); }
    @Override public boolean equals(Object obj) { return super.equals(obj) && name().equals(((Var) obj).name()); }
    @Override protected double eval_(double[] inVals) { throw new IllegalArgumentException(); }
    @Override protected String llvm_(HashMap<Op, String> map, Writer writer) throws IOException {
        return String.format("%%%s", name());
    }
    @Override protected Op diff(int inputIdx) { return null; }
}
