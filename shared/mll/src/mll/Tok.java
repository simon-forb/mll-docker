package mll;

class Pos {
    Pos(int row, int col) {
        this.row = row;
        this.col = col;
    }

    @Override public Pos clone() { return new Pos(row, col); }

    @Override public String toString() { return String.format("%d:%d", row, col); }

    @Override public boolean equals(Object obj) { return (obj instanceof Pos pos) && row == pos.row && col == pos.col; }

    public int row;
    public int col;
}

class Loc {
    Loc(String file, Pos begin, Pos finis) {
        this.file  = file;
        this.begin = begin;
        this.finis = finis;
    }

    Loc copy() { return new Loc(file, begin.clone(), finis.clone()); }

    @Override public String toString() {
        if (begin.equals(finis)) return String.format("%s:%s", file, begin);
        return String.format("%s:%s-%s", file, begin, finis);
    }

    String file;
    Pos    begin;
    Pos    finis;
}

public class Tok {
    enum Tag {
        K_let, K_log, K_exp, K_sin, K_cos, O_add, O_sub, O_mul, O_pow, O_div, D_paren_l, D_paren_r, M_id, M_lit, M_eof,
        T_semicolon, T_eq;

        public Prec binPrec() {
            return switch (this) {
                case O_pow -> Prec.Pow;
                case O_mul, O_div -> Prec.Mul;
                case O_add, O_sub -> Prec.Add;
                default -> Prec.Err;
            };
        }

        @Override public String toString() {
            return switch (this) {
                case K_let -> "let";
                case K_exp -> "exp";
                case K_sin -> "sin";
                case K_cos -> "cos";
                case K_log -> "log";
                case O_add -> "+";
                case O_sub -> "-";
                case O_mul -> "*";
                case O_pow -> "^";
                case O_div -> "/";
                case D_paren_l -> "(";
                case D_paren_r -> ")";
                case T_eq -> "=";
                case T_semicolon -> ";";
                case M_id -> "<identifier>";
                case M_lit -> "<literal>";
                case M_eof -> "<end of file>";
            };
        }
    }

    enum Prec {
        Err, Bot, Add, Mul, Pow, Pre,
    }

    public Tok(Loc loc, Tag tag) {
        tag_ = tag;
        loc_ = loc;
    }

    public Tok(Loc loc, String id) {
        loc_ = loc;
        tag_ = Tag.M_id;
        id_  = id;
    }

    public Tok(Loc loc, float f) {
        loc_ = loc;
        tag_ = Tag.M_lit;
        f_   = f;
    }

    public Tag tag() { return tag_; }
    public Loc loc() { return loc_; }

    public String id() {
        assert tag_ == Tag.M_id;
        return id_;
    }

    public float lit() {
        assert tag_ == Tag.M_lit;
        return f_;
    }

    @Override public String toString() {
        return switch (tag()) {
            case M_id -> id();
            case M_lit -> Float.toString(f_);
            default -> tag().toString();
        };
    }

    private Tag    tag_;
    private Loc    loc_;
    private float  f_;
    private String id_;
}
