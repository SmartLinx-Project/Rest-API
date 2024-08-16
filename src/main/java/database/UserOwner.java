package database;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UserOwner extends User {
    private boolean owner;

    public UserOwner() {
        super();
    }
    public UserOwner(String firstName, String lastName, String mail, String profilePicture, boolean owner) {
        super(firstName, lastName, mail, profilePicture);
        this.owner = owner;
    }
    public boolean isOwner() {
        return owner;
    }

    public void setOwner(boolean owner) {
        this.owner = owner;
    }
}
