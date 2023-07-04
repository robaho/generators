import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.Semaphore;

public class IntGenerator implements Iterable<Integer> {
    private final int max;
    IntGenerator(int max) {
        this.max = max;
    }

    @Override
    public Iterator<Integer> iterator() {
        IteratorImpl itr = new IteratorImpl();
        new IntProducer(max,itr);
        return itr;
    }

    private static class IntProducer {
        private final WeakReference<IteratorImpl> itrRef;
        private final Semaphore ready = new Semaphore(0);

        IntProducer(int max,IteratorImpl itr) {
            itrRef = new WeakReference<IntGenerator.IteratorImpl>(itr);
            itr.setProducer(this);
            Thread.startVirtualThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        for(int i=0;i<max;i++) {
                            while(!pushValue(i)) {
                                ready.acquire();
                                if(itrRef.get()==null)
                                    return;
                            }
                        }
                        IteratorImpl itr = itrRef.get();
                        if(itr==null) return;
                        itr.markDone();
                    } catch(InterruptedException e) {
                        return;
                    } finally {
                        System.out.println("producer is done");
                    }
                }
        });
        }
        private boolean pushValue(int i) {
            IteratorImpl itr = itrRef.get();
            if(itr==null) return false;
            return itr.newValue(i);
        }
        private void ready() {
            this.ready.release();
        }
    }

    private static class IteratorImpl implements Iterator<Integer> {
        private volatile boolean done;
        private Integer next;
        private IntProducer producer;

        private void setProducer(IntProducer producer) {
            this.producer = producer;
        }

        @Override
        public synchronized boolean hasNext() {
            while(true) {
                if(next!=null)
                    return true;
                if(done) return false;
                try {
                    wait();
                } catch (InterruptedException e) {
                    return false;
                }
            }
        }

        @Override
        public synchronized Integer next() {
            while(true) {
                if(!hasNext()) throw new NoSuchElementException();
                Integer value = next;
                next = null;
                producer.ready();
                return value;
            }
        }

        private synchronized boolean newValue(Integer value) {
            if(next==null) {
                next=value;
                this.notify();
                return true;
            }
            return false;
        }
        private synchronized void markDone() {
            done=true;
            this.notifyAll();
        }
        protected void finalize() throws Throwable {
            System.out.println("iterator is finalized");
            producer.ready();
        }
    }

}