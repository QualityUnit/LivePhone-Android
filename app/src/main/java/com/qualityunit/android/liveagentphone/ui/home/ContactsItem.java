package com.qualityunit.android.liveagentphone.ui.home;

import java.util.List;

/**
 * Created by rasto on 6.10.16.
 */

public class ContactsItem {

    private String firstName;
    private String lastName;
    private String systemName;
    private String avatarUrl;
    private String type;
    private List<String> emails;
    private List<String> phones;

    public ContactsItem(String firstName, String lastName, String systemName, String avatarUrl, String type) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.systemName = systemName;
        this.avatarUrl = avatarUrl;
        this.type = type;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getSystemName() {
        return systemName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getType() {
        return type;
    }

    public List<String> getEmails() {
        return emails;
    }

    public List<String> getPhones() {
        return phones;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setEmails(List<String> emails) {
        this.emails = emails;
    }

    public void setPhones(List<String> phones) {
        this.phones = phones;
    }
}
