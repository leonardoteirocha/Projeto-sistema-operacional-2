package barber;

import java.time.LocalTime;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class BarberShop {
    final Semaphore customers; // count waiting customers
    final Semaphore barber; // barber ready
    final Semaphore mutex; // protect chairs
    final int chairs;
    int waiting = 0;
    final int totalClients;
    final BarberMain.Range interArrival, service;
    AtomicInteger served = new AtomicInteger(0);
    AtomicInteger rejected = new AtomicInteger(0);
    volatile long barberBusyTime = 0;
    long barberStartBusy = 0;

    public BarberShop(int chairs, int totalClients, BarberMain.Range interArrival, BarberMain.Range service) {
        this.chairs = chairs; this.totalClients = totalClients;
        this.interArrival = interArrival; this.service = service;
        customers = new Semaphore(0);
        barber = new Semaphore(0);
        mutex = new Semaphore(1);
    }

    public void start() throws InterruptedException {
        Thread barberThread = new Thread(() -> {
            try {
                while(true){
                    customers.acquire(); // wait for customer
                    mutex.acquire();
                    waiting--;
                    barberStartBusy = System.currentTimeMillis();
                    barber.release(); // signal customer to be served
                    mutex.release();

                    // simulate haircut
                    int serv = new Random().nextInt(service.max-service.min+1)+service.min;
                    System.out.println(LocalTime.now()+" [Barbeiro] atendendo por "+serv+" ms");
                    Thread.sleep(serv);
                    barberBusyTime += (System.currentTimeMillis() - barberStartBusy);
                    served.incrementAndGet();
                }
            } catch(InterruptedException e){ Thread.currentThread().interrupt(); }
        }, "Barbeiro");
        barberThread.start();

        Thread[] clients = new Thread[totalClients];
        Random rnd = new Random();
        for(int i=0;i<totalClients;i++){
            final int id = i+1;
            clients[i] = new Thread(() -> {
                try{
                    Thread.sleep(rnd.nextInt(interArrival.max-interArrival.min+1)+interArrival.min);
                    System.out.println(LocalTime.now()+" [Cliente-"+id+"] chegou");
                    if (mutex.tryAcquire()) {
                        if (waiting < chairs) {
                            waiting++;
                            System.out.println(LocalTime.now()+" [Cliente-"+id+"] esperando (cadeiras ocupadas="+waiting+")");
                            customers.release();
                            mutex.release();
                            barber.acquire(); // wait until barber is ready
                            System.out.println(LocalTime.now()+" [Cliente-"+id+"] sendo atendido");
                        } else {
                            // no chairs
                            rejected.incrementAndGet();
                            System.out.println(LocalTime.now()+" [Cliente-"+id+"] foi embora (sem cadeiras)");
                            mutex.release();
                        }
                    } else {
                        // couldn't get mutex quickly; try again (avoid busy-wait)
                        mutex.acquire();
                        if (waiting < chairs) {
                            waiting++;
                            System.out.println(LocalTime.now()+" [Cliente-"+id+"] esperando (cadeiras ocupadas="+waiting+")");
                            customers.release();
                            mutex.release();
                            barber.acquire();
                            System.out.println(LocalTime.now()+" [Cliente-"+id+"] sendo atendido");
                        } else {
                            rejected.incrementAndGet();
                            System.out.println(LocalTime.now()+" [Cliente-"+id+"] foi embora (sem cadeiras)");
                            mutex.release();
                        }
                    }
                } catch(InterruptedException e){ Thread.currentThread().interrupt(); }
            }, "Cliente-"+id);
            clients[i].start();
        }

        // wait clients to finish arrival
        for(Thread t: clients) t.join();
        // after all clients arrived, wait a bit for barber to finish
        Thread.sleep(2000);
        long totalTime = Math.max(1, barberBusyTime); // avoid zero
        double utilization = (double)barberBusyTime / (System.currentTimeMillis() - (System.currentTimeMillis()-2000)) ; // approximation
        // interrupt barber thread
        barberThread.interrupt();

        System.out.println("=== RESULTADO ===");
        System.out.println("Clientes atendidos: " + served.get());
        System.out.println("Clientes rejeitados: " + rejected.get());
        long run = Math.max(1, (long)(served.get() * ((service.min+service.max)/2.0)));
        System.out.printf("Utilizacao (aprox, ms ocupado): %d ms\n", barberBusyTime);
    }
}

