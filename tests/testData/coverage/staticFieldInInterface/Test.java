public class Test implements I{

    public String foo(int i) {
        if (i == 9) {
            System.out.println("9");
            System.out.println("another line");
        } else if (i == 0) {
            System.out.println("foo");
        }

        switch (i) {
            case 0:
                System.out.println("0");
                break;
            default:
                System.out.println("default");
        }
        System.out.println("hoo");
        new Runnable(){
            public void run() {
                System.out.println("do smth");
            }
        }.run();
        new Object() {
            void update(String s) {
                System.out.println(s);
            }
        }.update("sss");
        new TT().bar();
        return null;
    }

    public static void main(String[] args) {
        new Test().foo(9);
    }

}

class TT {
    static Integer II = new Integer(2);

    void bar() {
        System.out.println(II);
    }
}

interface I {
  Integer ZERO = new Integer (0);
}
