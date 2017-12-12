package com.bluelinelabs.logansquare.demo.model;

import org.mariotaku.pjp.PicoField;
import org.mariotaku.pjp.PicoObject;

import java.util.List;
import java.util.Map;

@PicoObject
public class User2 extends User {
    @PicoField
    String myField;
    @PicoField
    Map<String, String> stringMapField;
    @PicoField
    Map<String, Image> imageMapField;
    @PicoField
    List<Image> imageListField;
    @PicoField
    Image[] imageArrayField;
    @PicoField
    int[] intArrayField;
}
