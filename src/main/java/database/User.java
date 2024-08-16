package database;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class User {
    private String firstName;
    private String lastName;
    private String mail;
    private String profilePicture;

    public User() {}

    public User(String firstName, String lastName, String mail, String profilePicture) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.mail = mail;
        this.profilePicture = profilePicture;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }
}
