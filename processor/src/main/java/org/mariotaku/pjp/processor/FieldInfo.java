package org.mariotaku.pjp.processor;

import com.squareup.javapoet.TypeName;

public class FieldInfo {
    private String fieldName;
    String[] jsonNames;
    TypeName type;
    TypeName converter;
    TypeName mapper;
    boolean mapperGeneric;
    boolean serializable;
    boolean ignoreNull;
    boolean ignoreInvalid;

    public FieldInfo(String fieldName) {
        this.fieldName = fieldName;
    }

    public String[] getJsonNames() {
        return jsonNames;
    }

    public String getFieldName() {
        return fieldName;
    }

    public TypeName getType() {
        return type;
    }

    public TypeName getConverter() {
        return converter;
    }

    public String getConverterFieldName() {
        if (converter == null) throw new NullPointerException();
        return converter.toString().replace('.', '_');
    }

    public String getMapperFieldName() {
        if (mapper == null)
            throw new IllegalStateException(String.format("Type %s does not have a mapper", type));

        return mapper.toString().replace('.', '_');
    }

    public boolean hasMapper() {
        return mapper != null && !mapperGeneric;
    }

    public boolean isIgnoreNull() {
        return ignoreNull;
    }

    public boolean isIgnoreInvalid() {
        return ignoreInvalid;
    }
}
