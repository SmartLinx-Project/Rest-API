package info;

import database.Room;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
@XmlRootElement
public class RoomInfo extends Room {
    private ArrayList<DeviceInfo> devices;

    public RoomInfo() {
        super();
    }

    public ArrayList<DeviceInfo> getDevices() {
        return devices;
    }

    public void setDevices(ArrayList<DeviceInfo> devices) {
        this.devices = devices;
    }
}
