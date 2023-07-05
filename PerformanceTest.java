public class PerformanceTest {
    public static void main(String[] args) {
        final int COUNT = 1000000;
        Generator<Integer> generator = new Generator<>(callback -> {
            for(int i=0;i<COUNT;i++) {
                if(!callback.emit(i))
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
}
