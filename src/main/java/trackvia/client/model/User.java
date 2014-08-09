package trackvia.client.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class User {
    private long id;
    @SerializedName("first_name") private String firstName;
    @SerializedName("last_name") private String lastName;
    private String email;
    @SerializedName("Status") private String status;
    @SerializedName("Time Zone") private String timezone;
    @SerializedName("Created") private Date created;

    public User() {}

    public User(final String firstName, final String lastName, final String status, final String email,
                final String timezone, final Date created) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.status = status;
        this.email = email;
        this.timezone = timezone;
        this.created = created;
    }

    public long getId() {
        return id;
    }

    private void setId(long id) {
        this.id = id;
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

    public void setLastName(String last_name) {
        this.lastName = last_name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserStatus getStatus() {
        return UserStatus.get(status);
    }

    public void setStatus(UserStatus status) {
        this.status = status.code();
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Date getCreated() {
        return created;
    }

    private void setCreated(Date created) {
        this.created = created;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof User)) return false;

        User otherUser = (User) o;

        if (!(otherUser.id == id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) id;
        return result;
    }
}
