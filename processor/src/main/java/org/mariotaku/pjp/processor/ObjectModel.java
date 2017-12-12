package org.mariotaku.pjp.processor;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import org.mariotaku.pjp.PicoConverter;
import org.mariotaku.pjp.PicoField;
import org.mariotaku.pjp.PicoObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;

public class ObjectModel {
    private final TypeName typeName;
    private final TypeName superTypeName;
    private final boolean serializer;
    private final List<FieldInfo> fields = new ArrayList<>();

    private ObjectModel superModel;

    public ObjectModel(TypeName typeName, TypeName superTypeName, boolean serializer) {
        this.typeName = typeName;
        this.superTypeName = superTypeName;
        this.serializer = serializer;
    }

    public TypeName getTypeName() {
        return typeName;
    }

    public TypeName getSuperTypeName() {
        return superTypeName;
    }

    public void addField(VariableElement field) {
        PicoField fieldAnnotation = field.getAnnotation(PicoField.class);
        String fieldName = field.getSimpleName().toString();
        FieldInfo info = new FieldInfo(fieldName);
        TypeMirror fieldType = field.asType();
        info.jsonNames = fieldAnnotation.value();
        if (info.jsonNames.length == 0) {
            info.jsonNames = new String[]{fieldName};
        }
        info.type = TypeName.get(fieldType);
        TypeName converter;
        try {
            converter = TypeName.get(fieldAnnotation.converter());
        } catch (MirroredTypeException e) {
            converter = TypeName.get(e.getTypeMirror());
        }
        if (!TypeName.get(PicoConverter.class).equals(converter)) {
            info.converter = converter;
        }
        info.ignoreNull = fieldAnnotation.ignoreNull();
        info.ignoreInvalid = fieldAnnotation.ignoreInvalid();
        fields.add(info);
    }

    public ClassName getMapperName() {
        ClassName typeName = (ClassName) this.typeName;
        return typeName.peerClass(typeName.simpleName() + "PicoMapper");
    }

    public List<FieldInfo> getFields() {
        return fields;
    }

    public ObjectModel getSuperModel() {
        return superModel;
    }

    public void complete(Map<TypeName, ObjectModel> models) {
        if (superTypeName != null) {
            ObjectModel superModel = models.get(superTypeName);
            if (superModel != null) {
                this.superModel = superModel;
            }
        }
        for (FieldInfo field : fields) {
            TypeName fieldType = field.getType();
            ObjectModel model = models.get(fieldType);
            TypeName actualType = getFieldActualTypeName(fieldType);
            if (model == null) {
                model = models.get(actualType);
            }
            if (model != null) {
                field.serializable = model.serializer;
                field.mapperGeneric = actualType != null;
                field.mapper = model.getMapperName();
            }
        }
    }

    public boolean isSerializer() {
        return serializer;
    }

    public static ObjectModel create(TypeElement type) {
        PicoObject annotation = type.getAnnotation(PicoObject.class);
        TypeName superTypeName = null;
        if (type.getAnnotation(PicoObject.class) != null) {
            superTypeName = TypeName.get(type.getSuperclass());
        }
        return new ObjectModel(TypeName.get(type.asType()), superTypeName, annotation.serializer());
    }

    public static TypeName getFieldActualTypeName(TypeName fieldType) {
        if (fieldType instanceof ParameterizedTypeName) {
            ParameterizedTypeName parameterizedTypeName = (ParameterizedTypeName) fieldType;
            ClassName rawType = parameterizedTypeName.rawType;
            if (ClassName.get(List.class).equals(rawType)) {
                return parameterizedTypeName.typeArguments.get(0);
            } else if (ClassName.get(Map.class).equals(rawType)) {
                return parameterizedTypeName.typeArguments.get(1);
            }
        } else if (fieldType instanceof ArrayTypeName) {
            return ((ArrayTypeName) fieldType).componentType;
        }
        return null;
    }


    public static String escape(TypeName typeName) {
        return typeName.toString().replace('.', '_');
    }
}

