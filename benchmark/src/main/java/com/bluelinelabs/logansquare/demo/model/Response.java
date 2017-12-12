package com.bluelinelabs.logansquare.demo.model;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

import org.mariotaku.pjp.PicoField;
import org.mariotaku.pjp.PicoObject;

import java.util.List;

@JsonObject
@PicoObject
public class Response {

    @JsonField
    @PicoField
    public List<User> users;

    @JsonField
    @PicoField
    public String status;

    @SerializedName("is_real_json") // Annotation needed for GSON
    @JsonProperty("is_real_json") // Annotation needed for Jackson Databind
    @JsonField(name = "is_real_json")
    @PicoField("is_real_json")
    public boolean isRealJson;
}
