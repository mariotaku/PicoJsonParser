package com.bluelinelabs.logansquare.demo.parsetasks;

import android.util.JsonReader;

import com.bluelinelabs.logansquare.demo.model.Response;
import com.bluelinelabs.logansquare.demo.model.ResponsePicoMapper;

import java.io.StringReader;

public class PicoParser extends Parser {

    private ResponsePicoMapper mPicoMapper = new ResponsePicoMapper();

    public PicoParser(ParseListener parseListener, String jsonString) {
        super(parseListener, jsonString);
    }

    @Override
    protected int parse(String json) {
        try {
            JsonReader reader = new JsonReader(new StringReader(json));
            Response response = mPicoMapper.parse(reader);
            return response.users.size();
        } catch (Exception e) {
            return 0;
        } finally {
            System.gc();
        }
    }

}
