public class Test {
    boolean foo(final boolean i, boolean b, final boolean b1) {
        return b;
    }

    public static void main(String[] args) {
        boolean b = false;
        for (int i = 0; i< 10;i++) {
            b = !b;
            final Test t = new Test();
            if (!t.foo(b, !b, b)) {
                System.out.println("");
            }
        }
    }
}
