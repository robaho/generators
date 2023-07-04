public class GeneratorTest {
    public static void main(String[] args) throws InterruptedException {
        test(100);
        test(50);
        System.out.println("should see message that producer is done");
        for(int i=0;i<60;i++) {
            System.gc();
            Thread.sleep(1000);
        }
        System.out.println("done waiting");
    }
    private static void test(int max) {
        int count=0;
        for(int i : new IntGenerator(100)) {
            System.out.println(i);
            if(count++==max)
                break;
        }
    }
}
