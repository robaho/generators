public class PerformanceTest {

    private static void doTest() {
        final int COUNT = 1000000;
        Generator<Integer> generator = new Generator<>(callback -> {
            for(int i=0;i<COUNT;i++) {
                if(!callback.yield(i))
                    return;
            }
        });

        long start = System.currentTimeMillis();
        int count=0;
        for(int i : generator) {
            count++;
        }
        long diff = System.currentTimeMillis()-start;
        if(count!=COUNT) {
            throw new IllegalStateException("incorrect number of entries, count "+count+", expected "+COUNT);
        }
        System.out.println("time to generate "+COUNT+" values "+diff+" ms");
    }
    public static void main(String[] args) throws InterruptedException {
        for(int i=0;i<3;i++) {
            doTest();
            Thread.sleep(1000);
        }
    }
}
