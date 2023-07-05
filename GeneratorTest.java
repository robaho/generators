
public class GeneratorTest {

    public static void main(String[] args) {
        test(100);
        test(50);
    }

    static volatile int generations;

    private static void test(int max) {
        generations = -1;

        Generator<Integer> generator = new Generator<>(callback -> {
            generations = 0;
            for (int i = 0; i < 1000; i++) {
                generations++;
                if (!callback.yield(i))
                    return;
            }
        });
        if (generations != -1) {
            throw new IllegalStateException("producer should not have run, generations " + generations);
        }

        int count = 0;
        for (var i : generator) {
            count++;
            if(count==max)
                break;
        }
        if (generations != count) {
            throw new IllegalStateException("wrong count, generations "+generations+", count "+count);
        }
        if (max != count) {
            throw new IllegalStateException("wrong count, max "+max+", count "+count);
        }
    }
}
