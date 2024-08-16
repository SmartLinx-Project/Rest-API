package database;

import javax.xml.bind.annotation.XmlRootElement;
import java.sql.Time;
import java.time.DayOfWeek;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@XmlRootElement
public class Device {
    private int deviceID;
    private String name;
    private String ieeeAddress;
    private String type;
    private String model;

    public Device() {}

    public int getDeviceID() {
        return deviceID;
    }
    public void setDeviceID(int deviceID) {
        this.deviceID = deviceID;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getIeeeAddress() {
        return ieeeAddress;
    }
    public void setIeeeAddress(String ieeeAddress) {
        this.ieeeAddress = ieeeAddress;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
