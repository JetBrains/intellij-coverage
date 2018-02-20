public class Test extends junit.framework.TestCase  {

  public void test1() {
    ClassA.method1();
    ClassA.method2();
  }

  public void test2() {
    ClassB.method1();
    ClassB.method2();
  }

  public void test3() {
    ClassA.methodR();
    ClassB.methodR();
  }

  public void testConstructor() {
    new ClassA().someMethod();
    new ClassB().someMethod();
  }
}