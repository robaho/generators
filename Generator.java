import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

public class Generator<T> implements Iterable<T> {
    private final Producer<T> producer;
    
    Generator(Producer<T> producer) {
        this.producer = producer;
    }

    @Override
    public Iterator<T> iterator() {
        IteratorImpl<T> itr = new IteratorImpl<>();
        new ProducerImpl<T>(producer,itr);
        return itr;
    }

    private static class ProducerImpl<T> implements Callback<T> {
        private final WeakReference<IteratorImpl<T>> itrRef;
        private final Semaphore ready = new Semaphore(0);

        ProducerImpl(Producer<T> producer,IteratorImpl<T> itr) {
            itrRef = new WeakReference<>(itr);
            itr.setProducer(this);
            Thread.startVirtualThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ready.acquire();
                        if(itrRef.get()==null)
                            return;
                        producer.run(ProducerImpl.this);
                    } catch (InterruptedException ignore) {
                        // finally will clean-up
                    } finally {
                        System.out.println("producer is done");
                        IteratorImpl itr = itrRef.get();
                        if(itr==null) return;
                        itr.markDone();
                    }
                }
            });
        }

        @Override
        public boolean yield(T value) {
            pushValue(value);
            try {
                ready.acquire();
            } catch (InterruptedException e) {
                return false;
            }
            return itrRef.get()==null ? false : true;
        }
        
        private boolean pushValue(T value) {
            IteratorImpl<T> itr = itrRef.get();
            if(itr==null) return false;
            return itr.newValue(value);
        }
        
        private void ready() {
            this.ready.release();
        }
    }

    private static class IteratorImpl<T> implements Iterator<T> {
        private final AtomicReference<T> next = new AtomicReference<>();
        private volatile boolean done;
        private ProducerImpl<T> producer;
        private volatile Thread reader;

        private void setProducer(ProducerImpl<T> producer) {
            this.producer = producer;
        }

        @Override
        public boolean hasNext() {
            reader = Thread.currentThread();
            boolean released=false;
            while(true) {
                if(next.get()!=null)
                    return true;
                if(!released) {
                    producer.ready.release();
                    released=true;
                }
                if(done) return false;
                LockSupport.park();
            }
        }

        @Override
        public T next() {
            reader = Thread.currentThread();
            while(true) {
                if(!hasNext()) throw new NoSuchElementException();
                T value = next.getAndSet(null);
                return value;
            }
        }

        private boolean newValue(T value) {
            boolean result = next.compareAndSet(null,value);
            if(true) {
                LockSupport.unpark(reader);
            }
            return result;
        }
        private void markDone() {
            done=true;
            LockSupport.unpark(reader);
        }
        protected void finalize() throws Throwable {
            System.out.println("iterator is finalized");
            producer.ready();
        }
    }

    public static interface Producer<T> {
        void run(Callback<T> callback);
    }

    public static interface Callback<T> {
        /** return false if the producer should terminate */
        boolean yield(T value);
    }
}