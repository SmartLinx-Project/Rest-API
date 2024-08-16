package schedule;

import database.Action;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ScheduledAction extends Action {
    private String ieeeAddress;

    public String getIeeeAddress() {
        return ieeeAddress;
    }

    public void setIeeeAddress(String ieeeAddress) {
        this.ieeeAddress = ieeeAddress;
    }
}
