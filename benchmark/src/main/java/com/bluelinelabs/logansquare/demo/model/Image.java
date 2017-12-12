package com.bluelinelabs.logansquare.demo.model;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

import org.mariotaku.pjp.PicoField;
import org.mariotaku.pjp.PicoObject;

@JsonObject
@PicoObject
public class Image {

    @JsonField
    @PicoField
    public String id;

    @JsonField
    @PicoField
    public String format;

    @JsonField
    @PicoField
    public String url;

    @JsonField
    @PicoField
    public String description;

}
