package database;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class HomeOwner extends Home {
    private boolean owner;

    public HomeOwner() {
        super();
    }
    public HomeOwner(int homeID, String name, String address, boolean owner) {
        super(homeID, name, address);
        this.owner = owner;
    }
    public boolean isOwner() {
        return owner;
    }
    public void setOwner(boolean owner) {
        this.owner = owner;
    }
}
