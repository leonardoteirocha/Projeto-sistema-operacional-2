package ProducerConsumer;

import java.io.*;
import java.net.InetAddress;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class ProducerConsumerMain {
    public static void main(String[] args) throws Exception {
        String inputFile = args.length>0? args[0] : "inputs/producer_input.txt";
        Properties props = loadProps(inputFile);

        int P = Integer.parseInt(props.getProperty("produtores","3"));
        int C = Integer.parseInt(props.getProperty("consumidores","3"));
        int bufferSize = Integer.parseInt(props.getProperty("buffer","10"));
        int itensPorProdutor = Integer.parseInt(props.getProperty("itens_por_produtor","500"));
        Range delayProd = Range.parse(props.getProperty("delay_produtor_ms","2-6"));
        Range delayCons = Range.parse(props.getProperty("delay_consumidor_ms","1-4"));

        System.out.println("Executando em: " + InetAddress.getLocalHost().getHostName());

        Buffer buffer = new Buffer(bufferSize);
        AtomicInteger totalProduced = new AtomicInteger(0);
        AtomicInteger totalConsumed = new AtomicInteger(0);
        long start = System.currentTimeMillis();

        Thread[] producers = new Thread[P];
        Thread[] consumers = new Thread[C];

        for (int i=0;i<P;i++){
            producers[i] = new Thread(new Producer(i+1, itensPorProdutor, buffer, delayProd, totalProduced), "Produtor-"+(i+1));
            producers[i].start();
        }

        for (int i=0;i<C;i++){
            consumers[i] = new Thread(new Consumer(i+1, buffer, delayCons, totalConsumed, P*itensPorProdutor), "Consumidor-"+(i+1));
            consumers[i].start();
        }

        for (Thread t: producers) t.join();
        // after producers done, consumers may still be waiting; we can stop them when totalConsumed == totalProduced
        while (totalConsumed.get() < totalProduced.get()) {
            Thread.sleep(100);
        }
        // interrupt consumers
        for (Thread t: consumers) t.interrupt();
        for (Thread t: consumers) t.join();

        long end = System.currentTimeMillis();

        System.out.println("=== RESULTADO ===");
        System.out.println("Total produzido: " + totalProduced.get());
        System.out.println("Total consumido: " + totalConsumed.get());
        System.out.printf("Tempo total: %.3f s\n", (end-start)/1000.0);
    }

    static Properties loadProps(String path) throws IOException {
        Properties p = new Properties();
        try(BufferedReader br = new BufferedReader(new FileReader(path))){
            String line;
            while((line=br.readLine())!=null){
                line = line.trim();
                if(line.isEmpty() || line.startsWith("#")) continue;
                String key = line.split("=")[0].trim();
                String val = line.substring(line.indexOf('=')+1).trim();
                key = key.replaceAll("\\s+","_");
                p.setProperty(key, val);
            }
        }
        return p;
    }

    static class Range {
        int min, max;
        Range(int a,int b){min=a;max=b;}
        static Range parse(String s){
            String[] parts = s.split("-");
            int a = Integer.parseInt(parts[0]);
            int b = parts.length>1? Integer.parseInt(parts[1]) : a;
            return new Range(a,b);
        }
        int randomMs(){ return min + new Random().nextInt(max-min+1);}
    }
}
