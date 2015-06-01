public class Test {
    public static void main(String[] args){
        foo(() -> {
            System.out.println("hello, ");
            System.out.println("world");
        });
    }

    static void foo(Runnable r){
        r.run();
    }
}