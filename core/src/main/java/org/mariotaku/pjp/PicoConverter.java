package org.mariotaku.pjp;

import android.util.JsonReader;

import java.io.IOException;

public interface PicoConverter<T> {

    T parse(JsonReader reader) throws IOException;

}
