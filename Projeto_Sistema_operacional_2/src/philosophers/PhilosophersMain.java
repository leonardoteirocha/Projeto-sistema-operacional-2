package philosophers;

import java.io.*;
import java.net.InetAddress;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class PhilosophersMain {
    enum State { THINKING, HUNGRY, EATING }

    static class Philosopher implements Runnable {
        int id, cycles;
        int thinkMin, thinkMax, eatMin, eatMax;
        State[] states;
        Semaphore[] self;
        Semaphore mutex;
        int N;
        AtomicInteger[] meals;
        Random rnd = new Random();
        volatile long totalWait=0;
        volatile int ate=0;

        Philosopher(int id, State[] states, Semaphore[] self, Semaphore mutex, int N,
                    int thinkMin, int thinkMax, int eatMin, int eatMax,
                    AtomicInteger[] meals){
            this.id=id; this.states=states; this.self=self; this.mutex=mutex; this.N=N;
            this.thinkMin = thinkMin; this.thinkMax = thinkMax; this.eatMin=eatMin; this.eatMax=eatMax;
            this.meals=meals;
        }

        int left(){ return (id+N-1)%N; }
        int right(){ return (id+1)%N; }

        void test(int i){
            if(states[i]==State.HUNGRY && states[left(i)]!=State.EATING && states[right(i)]!=State.EATING){
                states[i]=State.EATING;
                self[i].release();
            }
        }

        int left(int i){ return (i+N-1)%N; }
        int right(int i){ return (i+1)%N; }

        public void run(){
            try{
                long endTime = System.currentTimeMillis() + (long)Integer.parseInt(System.getProperty("duration_ms","20000"));
                while(System.currentTimeMillis() < endTime){
                    // thinking
                    int t = rnd.nextInt(thinkMax-thinkMin+1)+thinkMin;
                    Thread.sleep(t);
                    // hungry
                    mutex.acquire();
                    states[id]=State.HUNGRY;
                    long waitStart = System.currentTimeMillis();
                    test(id);
                    mutex.release();
                    self[id].acquire(); // wait until can eat
                    long waited = System.currentTimeMillis()-waitStart;
                    totalWait += waited;

                    // eat
                    int e = rnd.nextInt(eatMax-eatMin+1)+eatMin;
                    System.out.println(LocalTime.now()+" [Filosofo-"+(id+1)+"] comendo. espera(ms)="+waited);
                    Thread.sleep(e);
                    ate++;
                    meals[id].incrementAndGet();

                    mutex.acquire();
                    states[id]=State.THINKING;
                    // wake neighbors
                    test(left());
                    test(right());
                    mutex.release();
                }
                System.out.println(LocalTime.now()+" [Filosofo-"+(id+1)+"] terminou. refeicoes="+ate+" tempo_total_espera_ms="+totalWait);
            } catch(InterruptedException ex){ Thread.currentThread().interrupt();}
        }
    }

    public static void main(String[] args) throws Exception {
        String input = args.length>0? args[0] : "inputs/philosophers_input.txt";
        Map<String,String> props = load(input);

        int N = Integer.parseInt(props.getOrDefault("filosofos","5"));
        int duration = Integer.parseInt(props.getOrDefault("duracao_seg","20")) * 1000;
        String variation = props.getOrDefault("variacao","verificacao");
        Range think = Range.parse(props.getOrDefault("think_ms","50-120"));
        Range eat = Range.parse(props.getOrDefault("eat_ms","40-90"));

        System.setProperty("duration_ms", String.valueOf(duration));
        System.out.println("Executando em: " + InetAddress.getLocalHost().getHostName());
        System.out.println("Variacao: " + variation);

        if(variation.equalsIgnoreCase("simetria")){
            runSymmetry(N, duration, think, eat);
        } else {
            runMonitor(N, duration, think, eat);
        }
    }

    // Solution A: monitor-like (states + per-philosopher semaphores)
    static void runMonitor(int N, int duration, Range think, Range eat) throws InterruptedException {
        State[] states = new State[N];
        Arrays.fill(states, State.THINKING);
        Semaphore[] self = new Semaphore[N];
        for(int i=0;i<N;i++) self[i]=new Semaphore(0);
        Semaphore mutex = new Semaphore(1);
        Thread[] threads = new Thread[N];
        AtomicInteger[] meals = new AtomicInteger[N];
        for(int i=0;i<N;i++) meals[i] = new AtomicInteger(0);

        for(int i=0;i<N;i++){
            Philosopher p = new Philosopher(i, states, self, mutex, N, think.min, think.max, eat.min, eat.max, meals);
            threads[i] = new Thread(p, "Filosofo-"+(i+1));
            threads[i].start();
        }
        Thread.sleep(duration);
        for(Thread t: threads) t.interrupt();
        for(Thread t: threads) t.join();

        System.out.println("=== RESULTADO ===");
        int totalMeals=0;
        for(int i=0;i<N;i++){
            int m = meals[i].get();
            totalMeals += m;
            System.out.println("Filosofo-"+(i+1)+" refeicoes="+m);
        }
        System.out.println("Total refeicoes: "+totalMeals);
    }

    // Solution B: symmetry break (all pick right first except last)
    static void runSymmetry(int N, int duration, Range think, Range eat) throws InterruptedException {
        java.util.concurrent.locks.Lock[] forks = new java.util.concurrent.locks.ReentrantLock[N];
        for(int i=0;i<N;i++) forks[i]=new java.util.concurrent.locks.ReentrantLock();
        AtomicInteger[] meals = new AtomicInteger[N];
        for(int i=0;i<N;i++) meals[i]=new AtomicInteger(0);
        Thread[] threads = new Thread[N];
        Random rnd = new Random();

        for(int i=0;i<N;i++){
            final int id = i;
            threads[i] = new Thread(() -> {
                try{
                    long end = System.currentTimeMillis()+duration;
                    while(System.currentTimeMillis() < end && !Thread.currentThread().isInterrupted()){
                        int t = rnd.nextInt(think.max-think.min+1)+think.min;
                        Thread.sleep(t);
                        // pick forks
                        int left = id;
                        int right = (id+1)%N;
                        if(id == N-1){ // last philosopher picks left first (break symmetry)
                            forks[left].lock();
                            forks[right].lock();
                        } else {
                            forks[right].lock();
                            forks[left].lock();
                        }
                        int e = rnd.nextInt(eat.max-eat.min+1)+eat.min;
                        System.out.println(LocalTime.now()+" [Filosofo-"+(id+1)+"] comendo");
                        Thread.sleep(e);
                        meals[id].incrementAndGet();
                        forks[left].unlock();
                        forks[right].unlock();
                    }
                } catch(InterruptedException ex){ Thread.currentThread().interrupt(); }
            }, "Filosofo-"+(i+1));
            threads[i].start();
        }

        Thread.sleep(duration);
        for(Thread t: threads) t.interrupt();
        for(Thread t: threads) t.join();

        System.out.println("=== RESULTADO ===");
        int total=0;
        for(int i=0;i<N;i++){
            int m = meals[i].get();
            total+=m;
            System.out.println("Filosofo-"+(i+1)+" refeicoes="+m);
        }
        System.out.println("Total refeicoes: "+total);
    }

    static Map<String,String> load(String path) throws IOException {
        Map<String,String> m = new HashMap<>();
        try(BufferedReader br = new BufferedReader(new FileReader(path))){
            String line;
            while((line=br.readLine())!=null){
                line=line.trim();
                if(line.isEmpty()||line.startsWith("#")) continue;
                String key = line.split("=")[0].trim();
                String val = line.substring(line.indexOf('=')+1).trim();
                key = key.replaceAll("\\s+","_");
                m.put(key,val);
            }
        }
        return m;
    }

    static class Range { int min,max; Range(int a,int b){min=a;max=b;} static Range parse(String s){
        String[] p=s.split("-"); int a=Integer.parseInt(p[0]); int b=p.length>1?Integer.parseInt(p[1]):a; return new Range(a,b);
    }}
}
