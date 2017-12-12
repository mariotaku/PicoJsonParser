package com.bluelinelabs.logansquare.demo.serializetasks;

import android.util.JsonWriter;

import com.bluelinelabs.logansquare.demo.model.Response;
import com.bluelinelabs.logansquare.demo.model.ResponsePicoMapper;

import java.io.StringWriter;

public class PicoSerializer extends Serializer {

    private ResponsePicoMapper mResponsePicoMapper = new ResponsePicoMapper();

    public PicoSerializer(SerializeListener parseListener, Response response) {
        super(parseListener, response);
    }

    @Override
    protected String serialize(Response response) {
        try {
            StringWriter writer = new StringWriter();
            mResponsePicoMapper.serializeObject(new JsonWriter(writer), response);
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            System.gc();
        }
    }
}
