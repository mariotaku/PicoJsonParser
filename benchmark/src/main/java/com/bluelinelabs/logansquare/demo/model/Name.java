package com.bluelinelabs.logansquare.demo.model;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

import org.mariotaku.pjp.PicoField;
import org.mariotaku.pjp.PicoObject;

@JsonObject
@PicoObject
public class Name {

    @JsonField
    @PicoField
    public String first;

    @JsonField
    @PicoField
    public String last;
}
