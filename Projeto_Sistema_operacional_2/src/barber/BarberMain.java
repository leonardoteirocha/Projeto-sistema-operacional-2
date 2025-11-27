package barber;

import java.io.*;
import java.net.InetAddress;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class BarberMain {
    public static void main(String[] args) throws Exception {
        String input = args.length>0? args[0] : "inputs/barber_input.txt";
        Map<String,String> props = load(input);

        int chairs = Integer.parseInt(props.getOrDefault("cadeiras_espera","4"));
        int totalClients = Integer.parseInt(props.getOrDefault("clientes_totais","20"));
        Range interArrival = Range.parse(props.getOrDefault("interchegada_ms","10-40"));
        Range service = Range.parse(props.getOrDefault("servico_ms","30-60"));

        System.out.println("Executando em: " + InetAddress.getLocalHost().getHostName());

        BarberShop shop = new BarberShop(chairs, totalClients, interArrival, service);
        shop.start();
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
