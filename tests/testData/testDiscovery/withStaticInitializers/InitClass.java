public class InitClass {
  public static void initInit() {
    B b = new B(1);
  }
}

class A {
  static final A CONST = new B(1);
}

class B extends A {
  public B(int i) { }
}