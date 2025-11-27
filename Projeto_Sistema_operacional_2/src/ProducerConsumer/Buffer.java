package ProducerConsumer;
import java.time.LocalTime;
import java.util.concurrent.Semaphore;

public class Buffer {
    private final int[] items;
    private int in=0, out=0;
    private final Semaphore empty, full, mutex;

    public Buffer(int size){
        items = new int[size];
        empty = new Semaphore(size);
        full = new Semaphore(0);
        mutex = new Semaphore(1);
    }

    public void put(int item, String threadName) throws InterruptedException {
        empty.acquire();
        mutex.acquire();
        items[in] = item;
        in = (in+1) % items.length;
        System.out.println(LocalTime.now() + " ["+threadName+"] produziu item " + item);
        mutex.release();
        full.release();
    }

    public int take(String threadName) throws InterruptedException {
        full.acquire();
        mutex.acquire();
        int item = items[out];
        out = (out+1) % items.length;
        System.out.println(LocalTime.now() + " ["+threadName+"] consumiu item " + item);
        mutex.release();
        empty.release();
        return item;
    }
}

