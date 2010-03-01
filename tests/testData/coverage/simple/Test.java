public class Test {
    public static void main(String[] args){
      int i = 0;
      if (i == 0) {
          System.out.println(0);
          System.out.println("another line");
      } else {
          System.out.println(i);
      }

      switch (i) {
        case 0 :
           System.out.println("do switch");
           break;
        default:
           System.out.println("");
      }
    }
}