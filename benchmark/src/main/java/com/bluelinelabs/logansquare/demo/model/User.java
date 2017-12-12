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
public class User {

    @SerializedName("_id") // Annotation needed for GSON
    @JsonProperty("_id")
    @PicoField("_id")
    @JsonField(name = "_id")
    public String id;

    @JsonField
    @PicoField
    public int index;

    @JsonField
    @PicoField
    public String guid;

    @SerializedName("is_active") // Annotation needed for GSON
    @JsonProperty("is_active") // Annotation needed for Jackson Databind
    @PicoField("is_active")
    @JsonField(name = "is_active")
    public boolean isActive;

    @JsonField
    @PicoField
    public String balance;

    @SerializedName("picture") // Annotation needed for GSON
    @JsonProperty("picture") // Annotation needed for Jackson Databind
    @PicoField("picture")
    @JsonField(name = "picture")
    public String pictureUrl;

    @JsonField
    @PicoField
    public int age;

    @JsonField
    @PicoField(ignoreNull = true)
    public Name name;

    @JsonField
    @PicoField
    public String company;

    @JsonField
    @PicoField
    public String email;

    @JsonField
    @PicoField
    public String address;

    @JsonField
    @PicoField
    public String about;

    @JsonField
    @PicoField
    public String registered;

    @JsonField
    @PicoField
    public double latitude;

    @JsonField
    @PicoField
    public double longitude;

    @JsonField
    @PicoField
    public List<String> tags;

    @JsonField
    @PicoField
    public List<Integer> range;

    @JsonField
    @PicoField
    public List<Friend> friends;

    @JsonField
    @PicoField
    public List<Image> images;

    @JsonField
    @PicoField
    public String greeting;

    @SerializedName("favorite_fruit") // Annotation needed for GSON
    @JsonProperty("favorite_fruit") // Annotation needed for Jackson Databind
    @PicoField("favorite_fruit")
    @JsonField(name = "favorite_fruit")
    public String favoriteFruit;

    @SerializedName("eye_color") // Annotation needed for GSON
    @JsonProperty("eye_color") // Annotation needed for Jackson Databind
    @PicoField("eye_color")
    @JsonField(name = "eye_color")
    public String eyeColor;

    @JsonField
    @PicoField
    public String phone;
}
