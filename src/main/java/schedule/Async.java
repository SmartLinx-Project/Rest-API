package schedule;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Async {

    private int corePoolSize;
    private final ScheduledExecutorService executorService;

    public Async () {
        corePoolSize = Runtime.getRuntime().availableProcessors();
        executorService = Executors.newScheduledThreadPool(Math.max(1, corePoolSize));
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public void run(Runnable task) {
        executorService.schedule(task, 0, TimeUnit.MILLISECONDS);
    }

    public void await() {
        // Indica che non accetterà nuovi task e chiude correttamente
        executorService.shutdown();
        try {
            // Aspetta la terminazione di tutti i task, se non sono terminati entro il limite, forza la loro chiusura
            if(!executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            e.printStackTrace();
        }
    }
}
