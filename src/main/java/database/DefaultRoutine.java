package database;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement
public class DefaultRoutine {
    private int routineID;
    private String name;
    private String icon;
    private boolean enabled;
    private String time;
    private String[] periodicity;

    public int getRoutineID() {
        return routineID;
    }

    public void setRoutineID(int routineID) {
        this.routineID = routineID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public boolean isEnabled() {
        return enabled;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String[] getPeriodicity() {
        return periodicity;
    }
    public void setPeriodicity(String[] periodicity) {
        this.periodicity = periodicity;
    }

    @XmlTransient
    public boolean isOneTimeRoutine() {
        return periodicity == null || periodicity[0].isEmpty();
    }
}
