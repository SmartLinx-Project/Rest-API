package schedule;

import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Schedule {
    public static void polling() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        long period = 60 * 1000; // Un minuto in millisecondi
        scheduler.scheduleAtFixedRate(new Task(), computeInitialDelay(), period, TimeUnit.MILLISECONDS);
    }

    private static long computeInitialDelay() {
        LocalTime currentTime = LocalTime.now();
        // Calcola il ritardo fino al prossimo minuto esatto
        long secondsUntilNextMinute = 60 - currentTime.getSecond(); // Secondi fino al prossimo minuto
        return secondsUntilNextMinute * 1000 - currentTime.getNano() / 1_000_000;
    }
}