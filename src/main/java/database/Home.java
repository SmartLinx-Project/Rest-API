package database;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Home {
    private int homeID;
    private int hubID; //se 0 è come se fosse null
    private String name;
    private String address;


    public Home() {}

    public Home(int homeID, String name, String address) {
        this.homeID = homeID;
        this.name = name;
        this.address = address;
    }

    public int getHomeID() {
        return homeID;
    }

    public void setHomeID(int homeID) {
        this.homeID = homeID;
    }

    public int getHubID() {
        return hubID;
    }

    public void setHubID(int hubID) {
        this.hubID = hubID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
