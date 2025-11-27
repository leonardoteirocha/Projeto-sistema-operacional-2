package ProducerConsumer;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;

public class Producer implements Runnable {
    private final int id, items;
    private final ProducerConsumerMain.Range delay;
    private final Buffer buffer;
    private final AtomicInteger totalProduced;

    public Producer(int id, int items, Buffer buffer, ProducerConsumerMain.Range delay, AtomicInteger totalProduced){
        this.id=id;this.items=items;this.buffer=buffer;this.delay=delay;this.totalProduced=totalProduced;
    }

    public void run(){
        try{
            for(int i=1;i<=items;i++){
                buffer.put((id*100000)+i, Thread.currentThread().getName());
                totalProduced.incrementAndGet();
                Thread.sleep(delay.randomMs());
            }
            System.out.println(LocalTime.now() + " ["+Thread.currentThread().getName()+"] terminou produção.");
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }
}
