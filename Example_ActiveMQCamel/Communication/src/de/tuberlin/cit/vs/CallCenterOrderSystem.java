// File: CallCenterOrderSystem.java
package de.tuberlin.cit.vs;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.*;

/**
 * Channel Adapter → every 2 minutes dumps queued orders into a timestamped file
 * in “orders/” directory. Each line: FullName, surfboards, divingSuits, CustomerID
 */
public class CallCenterOrderSystem {
    private static final File OUT = new File("orders");
    private static final BlockingQueue<String> queue = new LinkedBlockingQueue<>();

    public static void main(String[] args) throws Exception {
        OUT.mkdirs();
        // background flusher
        ScheduledExecutorService svc = Executors.newSingleThreadScheduledExecutor();
        svc.scheduleAtFixedRate(() -> {
            try {
                List<String> batch = new ArrayList<>();
                queue.drainTo(batch);
                if (!batch.isEmpty()) {
                    File f = new File(OUT, "orders_" + System.currentTimeMillis() + ".txt");
                    try (FileWriter w = new FileWriter(f)) {
                        for (String o : batch) w.write(o + "\n");
                    }
                    System.out.println("Flushed " + batch.size() + " call-center orders → " + f.getName());
                }
            } catch (Exception e) { e.printStackTrace(); }
        }, 2, 2, TimeUnit.MINUTES);

        // read from stdin
        Scanner sc = new Scanner(System.in);
        System.out.println("CallCenter> enter: FullName, surf, dive, custID or 'quit'");
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if (line.equalsIgnoreCase("quit")) break;
            queue.offer(line.trim());
        }
        svc.shutdownNow();
    }
}
