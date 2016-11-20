package com.home.pr.opendoor;

import java.util.Date;

/**
 * Created by Pr on 19/11/2016.
 */
public class StatusClass {
    private long id;
    private String key;
    private String name;
    private String description;
    private boolean history;
    private int unit_type;
    private int unit_symbol;
    private String last_value;
    private String status;
    private boolean enabled;
    private String license;
    private Date created_at;
    private Date updated_at;


    public String getDoorStatus() {
        return this.last_value;
    }
}
