package com.bluelinelabs.logansquare.demo.model;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

import org.mariotaku.pjp.PicoField;
import org.mariotaku.pjp.PicoObject;

@JsonObject
@PicoObject
public class Friend {

    @JsonField
    @PicoField
    public int id;

    @JsonField
    @PicoField
    public String name;
}
