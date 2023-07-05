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
                        producer.run(ProducerImpl.this);
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
        public boolean emit(T value) {
            while(!pushValue(value)) {
                try {
                    ready.acquire();
                } catch (InterruptedException e) {
                    return false;
                }
                if(itrRef.get()==null)
                    return false;
            }
            return true;
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
            while(true) {
                if(next.get()!=null)
                    return true;
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
                producer.ready();
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
        boolean emit(T value);
    }
}