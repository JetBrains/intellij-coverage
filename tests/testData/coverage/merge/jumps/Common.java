public class Common {
        public void count(int p) {
                if (p > 0) {    // BUG
                        System.out.println(p);
                } else {
                        System.out.println(p);
                }
        }
}
