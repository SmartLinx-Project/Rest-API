package database;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class StatusRequest {
    int hubId;
    String ieee_address;
    String command;

    public StatusRequest() {
    }

    public int getHubId() {
        return hubId;
    }

    public void setHubId(int hubId) {
        this.hubId = hubId;
    }

    public String getIeee_address() {
        return ieee_address;
    }

    public void setIeee_address(String ieee_address) {
        this.ieee_address = ieee_address;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }
}
