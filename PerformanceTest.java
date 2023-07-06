public class PerformanceTest {

    private static void test() {
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
            throw new IllegalStateException("incorrect number of entries");
        }
        System.out.println("time to generate "+COUNT+" values "+diff+" ms");
    }
    public static void main(String[] args) throws InterruptedException {
        test();
        // show that using a virtual thread for the reader improves the performance by 10x due to
        // reduced synchronization costs
        for(int i=0;i<3;i++) {
            Thread thread = Thread.startVirtualThread(() -> test());
            thread.join();
        }
    }
}
