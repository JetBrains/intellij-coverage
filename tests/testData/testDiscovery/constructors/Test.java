public class Test extends junit.framework.TestCase  {

  public void test1() {
    try {
      throw new AssertionFailedError();
    } catch (AssertionFailedError exception) {

    }
  }

  public void test2() {
    try {
      throw new AssertionFailedError("Ololo");
    } catch (AssertionFailedError exception) {

    }
  }

  public void test3() {
    AssertionFailedError.defaultString("Ololo");
  }
}