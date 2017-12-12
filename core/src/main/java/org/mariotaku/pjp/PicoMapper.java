package org.mariotaku.pjp;

import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public abstract class PicoMapper<T> {

    public void parseObject(JsonReader reader, T instance) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            parseField(reader, instance, name);
        }
        reader.endObject();
    }

    public void parseList(JsonReader reader, List<T> list) throws IOException {
        reader.beginArray();
        while (reader.hasNext()) {
            T item = newObject();
            parseObject(reader, item);
            list.add(item);
        }
        reader.endArray();
    }

    public void parseMap(JsonReader reader, Map<String, T> map) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            T item = newObject();
            parseObject(reader, item);
            map.put(name, item);
        }
        reader.endObject();
    }

    public T parse(JsonReader reader) throws IOException {
        T instance = newObject();
        parseObject(reader, instance);
        return instance;
    }

    public void serializeObject(JsonWriter writer, T instance) throws IOException {
        throw new UnsupportedOperationException();
    }

    protected abstract T newObject();

    protected abstract void parseField(JsonReader reader, T instance, String name) throws IOException;

}
