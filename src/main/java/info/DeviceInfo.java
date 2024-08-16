package info;

import database.Device;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DeviceInfo extends Device {
    private String status;

    public DeviceInfo() {
        super();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
