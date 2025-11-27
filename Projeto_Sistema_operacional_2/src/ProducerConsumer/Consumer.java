package ProducerConsumer;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;

public class Consumer implements Runnable {
    private final int id;
    private final ProducerConsumerMain.Range delay;
    private final Buffer buffer;
    private final AtomicInteger totalConsumed;
    private final int expectedTotal;

    public Consumer(int id, Buffer buffer, ProducerConsumerMain.Range delay, AtomicInteger totalConsumed, int expectedTotal){
        this.id=id;this.buffer=buffer;this.delay=delay;this.totalConsumed=totalConsumed;this.expectedTotal=expectedTotal;
    }

    public void run(){
        try{
            while(!Thread.currentThread().isInterrupted()){
                int item = buffer.take(Thread.currentThread().getName());
                totalConsumed.incrementAndGet();
                Thread.sleep(delay.randomMs());
                if (totalConsumed.get() >= expectedTotal) break;
            }
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
        } finally {
            System.out.println(LocalTime.now() + " ["+Thread.currentThread().getName()+"] terminou consumo.");
        }
    }
}
