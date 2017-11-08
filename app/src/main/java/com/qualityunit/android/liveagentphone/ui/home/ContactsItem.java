package com.qualityunit.android.liveagentphone.ui.home;

import java.util.List;

/**
 * Created by rasto on 6.10.16.
 */

public class ContactsItem {

    public String id;
    public String firstname;
    public String lastname;
    public String system_name;
    public String avatar_url;
    public String type;
    public List<String> emails;
    public List<String> phones;

    public ContactsItem(String id, String firstname, String lastname, String system_name, String avatar_url, String type) {
        this.id = id;
        this.firstname = firstname;
        this.lastname = lastname;
        this.system_name = system_name;
        this.avatar_url = avatar_url;
        this.type = type;
    }

    @Override
    public String toString() {
        return "ContactsItem{" +
                "id='" + id + '\'' +
                ", firstname='" + firstname + '\'' +
                ", lastname='" + lastname + '\'' +
                ", system_name='" + system_name + '\'' +
                ", avatar_url='" + avatar_url + '\'' +
                ", type='" + type + '\'' +
                ", emails=" + emails.toString() +
                ", phones=" + phones.toString() +
                '}';
    }
}
