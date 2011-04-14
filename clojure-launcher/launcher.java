package clojure;

import clojure.lang.Compiler;
import clojure.lang.IPersistentMap;
import clojure.lang.RT;
import clojure.lang.Symbol;
import clojure.lang.Var;

public class launcher{

    final static private Symbol CLOJURE_MAIN = Symbol.intern("clojure.main");
    final static private Var REQUIRE = RT.var("clojure.core", "require");
    final static private Var MAIN = RT.var("clojure.main", "main");

    public static void main(String[] args) {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            IPersistentMap bindings = RT.map(Compiler.LOADER, loader);
            boolean pushed = true;
            try {
                Var.pushThreadBindings(bindings);
            } catch (Exception ex) {
                pushed = false;
                throw ex;
            }
            try {
                REQUIRE.invoke(CLOJURE_MAIN);
                MAIN.applyTo(RT.seq(args));
            } finally {
                if (pushed)
                    Var.popThreadBindings();
            }
        } catch (Exception ex) {
            }
    }
}
