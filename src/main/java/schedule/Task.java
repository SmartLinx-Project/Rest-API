package schedule;

import database.DB;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;

public class Task extends Thread{

    public Task() {}

    @Override
    public void run() {
        try {
            ArrayList<ScheduledRoutine> routines = DB.getScheduledRoutines();

            LocalTime currentTime = LocalTime.now();
            for(ScheduledRoutine routine : routines) {

                //controlla se la schedulazione è disabilitata o se oggi non è un giorno periodico
                if(!routine.isEnabled() || !isPeriodicDay(routine.getPeriodicity(), getCurrentDay()) && !routine.isOneTimeRoutine())
                    continue;

                LocalTime time = LocalTime.parse(routine.getTime());

                if (currentTime.getHour() == time.getHour() && currentTime.getMinute() == time.getMinute()) {
                    routine.run();

                    if(routine.isOneTimeRoutine())
                        DB.disableRoutine(routine.getRoutineID());
                }
            }
        } catch (SQLException | MqttException e) {
            e.printStackTrace();
        } catch (IllegalStateException ignored) {}
    }

    private String getCurrentDay() {
        LocalDate today = LocalDate.now();
        DayOfWeek dayOfWeek = today.getDayOfWeek();
        return dayOfWeek.name().toLowerCase();
    }

    private boolean isPeriodicDay(String[] periocity, String currentDay) {
        for (String day : periocity)
            if (day.equalsIgnoreCase(currentDay))
                return true;
        return false;
    }
}
