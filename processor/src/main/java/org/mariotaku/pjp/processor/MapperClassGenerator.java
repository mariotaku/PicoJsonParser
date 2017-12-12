package org.mariotaku.pjp.processor;


import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.mariotaku.pjp.PicoMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Modifier;

public class MapperClassGenerator {

    private static TypeName STRING = TypeName.get(String.class);

    public TypeSpec generate(ObjectModel model) {
        TypeSpec.Builder builder = TypeSpec.classBuilder(model.getMapperName());
        List<FieldInfo> fields = model.getFields();
        builder.addModifiers(Modifier.PUBLIC);
        builder.superclass(ParameterizedTypeName.get(ClassName.get(PicoMapper.class), model.getTypeName()));
        Set<TypeName> addedConverters = new HashSet<>(), addedMappers = new HashSet<>();
        ObjectModel superModel = model.getSuperModel();
        if (superModel != null) {
            ClassName mapper = superModel.getMapperName();
            addedMappers.add(mapper);
            builder.addField(FieldSpec.builder(mapper, ObjectModel.escape(mapper),
                    Modifier.STATIC, Modifier.FINAL).initializer("new $T()", mapper).build());
        }
        for (FieldInfo fieldInfo : fields) {
            TypeName converter = fieldInfo.converter;
            if (converter != null && !addedConverters.contains(converter)) {
                addedConverters.add(converter);
                builder.addField(FieldSpec.builder(converter, ObjectModel.escape(converter),
                        Modifier.STATIC, Modifier.FINAL).initializer("new $T()", converter).build());
            }
            TypeName mapper = fieldInfo.mapper;
            if (mapper != null && !addedMappers.contains(mapper)) {
                addedMappers.add(mapper);
                builder.addField(FieldSpec.builder(mapper, ObjectModel.escape(mapper),
                        Modifier.STATIC, Modifier.FINAL).initializer("new $T()", mapper).build());
            }
        }
        builder.addMethod(generateNewObjectMethod(model.getTypeName()));
        builder.addMethod(generateParseFieldMethod(model.getTypeName(), fields, superModel));

        if (model.isSerializer()) {
            builder.addMethod(generateSerializeObjectMethod(model.getTypeName(), fields, superModel));
        }

        return builder.build();
    }

    private MethodSpec generateNewObjectMethod(TypeName typeName) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("newObject");
        builder.addAnnotation(Override.class);
        builder.addModifiers(Modifier.PROTECTED);
        builder.returns(typeName);
        builder.addStatement("return new $T()", typeName);
        return builder.build();
    }

    private MethodSpec generateParseFieldMethod(TypeName typeName, List<FieldInfo> fields, ObjectModel superModel) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("parseField");
        builder.addAnnotation(Override.class);
        builder.addModifiers(Modifier.PROTECTED);
        builder.addException(IOException.class);
        builder.addParameter(JsonReader.class, "reader");
        builder.addParameter(typeName, "instance");
        builder.addParameter(String.class, "field");
        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        boolean hasAssignmentFlow = false;
        for (int i = 0, j = fields.size(); i < j; i++) {
            FieldInfo field = fields.get(i);
            TypeName fieldType = field.getType();
            String fieldName = field.getFieldName();
            String[] jsonNames = field.getJsonNames();
            for (int k = 0, l = jsonNames.length; k < l; k++) {
                hasAssignmentFlow = true;
                String jsonName = jsonNames[k];
                if (i == 0 && k == 0) {
                    codeBuilder.beginControlFlow("if ($S.equals(field))", jsonName);
                } else {
                    codeBuilder.nextControlFlow("else if ($S.equals(field))", jsonName);
                }
                if (field.converter != null) {
                    codeBuilder.addStatement("instance.$L = $L.parse(reader)", fieldName, field.getConverterFieldName());
                } else if (isBasicType(fieldType)) {
                    assignParsedBasicType(codeBuilder, field, jsonName, fieldType,
                            String.format(Locale.US, "instance.%s = ${readValue}", fieldName), true);
                } else if (field.hasMapper()) {
                    codeBuilder.beginControlFlow("if (reader.peek() == $T.NULL)", JsonToken.class);
                    if (!field.isIgnoreNull()) {
                        codeBuilder.addStatement("instance.$L = null", fieldName);
                    }
                    codeBuilder.addStatement("reader.nextNull()");
                    codeBuilder.nextControlFlow("else");
                    codeBuilder.addStatement("$T item = new $T()", fieldType, fieldType);
                    codeBuilder.addStatement("$L.parseObject(reader, item)", field.getMapperFieldName());
                    codeBuilder.addStatement("instance.$L = item", fieldName);
                    codeBuilder.endControlFlow();
                } else if (fieldType instanceof ParameterizedTypeName) {
                    codeBuilder.beginControlFlow("if (reader.peek() == $T.NULL)", JsonToken.class);
                    if (!field.isIgnoreNull()) {
                        codeBuilder.addStatement("instance.$L = null", fieldName);
                    }
                    codeBuilder.addStatement("reader.nextNull()");
                    codeBuilder.nextControlFlow("else");
                    ClassName rawType = ((ParameterizedTypeName) fieldType).rawType;
                    if (ClassName.get(List.class).equals(rawType)) {
                        TypeName actualType = ObjectModel.getFieldActualTypeName(fieldType);
                        codeBuilder.addStatement("$T list = new $T()", fieldType,
                                ParameterizedTypeName.get(ClassName.get(ArrayList.class), actualType));
                        if (isBasicType(actualType)) {
                            codeBuilder.addStatement("reader.beginArray()");
                            codeBuilder.beginControlFlow("while (reader.hasNext())");
                            assignParsedBasicType(codeBuilder, field, jsonName, actualType, "list.add(${readValue})", false);
                            codeBuilder.endControlFlow();
                            codeBuilder.addStatement("reader.endArray()");
                        } else {
                            codeBuilder.addStatement("$L.parseList(reader, list)", field.getMapperFieldName());
                        }
                        codeBuilder.addStatement("instance.$L = list", fieldName);
                    } else if (ClassName.get(Map.class).equals(rawType)) {
                        TypeName actualType = ObjectModel.getFieldActualTypeName(fieldType);
                        codeBuilder.addStatement("$T map = new $T()", fieldType,
                                ParameterizedTypeName.get(ClassName.get(HashMap.class), STRING, actualType));
                        if (isBasicType(actualType)) {
                            codeBuilder.addStatement("reader.beginObject()");
                            codeBuilder.beginControlFlow("while (reader.hasNext())");
                            codeBuilder.addStatement("String name = reader.nextName()");
                            assignParsedBasicType(codeBuilder, field, jsonName, actualType, "map.put(name, ${readValue})", false);
                            codeBuilder.endControlFlow();
                            codeBuilder.addStatement("reader.endObject()");
                        } else {
                            codeBuilder.addStatement("$L.parseMap(reader, map)", field.getMapperFieldName());
                        }
                        codeBuilder.addStatement("instance.$L = map", fieldName);
                    } else {
                        return fieldTypeUnsupported(typeName, fieldType, fieldName);
                    }

                    codeBuilder.endControlFlow();
                } else if (fieldType instanceof ArrayTypeName) {
                    codeBuilder.beginControlFlow("if (reader.peek() == $T.NULL)", JsonToken.class);
                    if (!field.isIgnoreNull()) {
                        codeBuilder.addStatement("instance.$L = null", fieldName);
                    }
                    codeBuilder.addStatement("reader.nextNull()");
                    codeBuilder.nextControlFlow("else");
                    TypeName actualType = ObjectModel.getFieldActualTypeName(fieldType);
                    assert actualType != null;
                    ParameterizedTypeName listType = ParameterizedTypeName.get(ClassName.get(ArrayList.class),
                            actualType.box());
                    codeBuilder.addStatement("$T list = new $T()", listType, listType);
                    if (isBasicType(actualType)) {
                        codeBuilder.addStatement("reader.beginArray()");
                        codeBuilder.beginControlFlow("while (reader.hasNext())");
                        assignParsedBasicType(codeBuilder, field, jsonName, actualType, "list.add(${readValue})", false);
                        codeBuilder.endControlFlow();
                        codeBuilder.addStatement("reader.endArray()");
                    } else {
                        codeBuilder.addStatement("$L.parseList(reader, list)", field.getMapperFieldName());
                    }
                    if (actualType.isPrimitive()) {
                        ArrayTypeName arrayType = ArrayTypeName.of(actualType);
                        codeBuilder.addStatement("$T array = new $T[list.size()]", arrayType, actualType);
                        codeBuilder.beginControlFlow("for (int i = 0, j = list.size(); i < j; i++)");
                        codeBuilder.addStatement("array[i] = list.get(i)");
                        codeBuilder.endControlFlow();
                        codeBuilder.addStatement("instance.$L = array", fieldName);
                    } else {
                        codeBuilder.addStatement("instance.$L = list.toArray(new $T[list.size()])", fieldName, actualType);
                    }
                    codeBuilder.endControlFlow();
                } else {
                    return fieldTypeUnsupported(typeName, fieldType, fieldName);
                }
            }
        }
        if (hasAssignmentFlow) {
            codeBuilder.nextControlFlow("else");
        }
        if (superModel != null) {
            codeBuilder.addStatement("$L.parseField(reader, instance, field)", ObjectModel.escape(superModel.getMapperName()));
        } else {
            codeBuilder.addStatement("reader.skipValue()");
        }
        if (hasAssignmentFlow) {
            codeBuilder.endControlFlow();
        }
        builder.addCode(codeBuilder.build());
        return builder.build();
    }

    private MethodSpec generateSerializeObjectMethod(TypeName typeName, List<FieldInfo> fields, ObjectModel superModel) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("serializeObject");
        builder.addAnnotation(Override.class);
        builder.addModifiers(Modifier.PUBLIC);
        builder.addException(IOException.class);
        builder.addParameter(JsonWriter.class, "writer");
        builder.addParameter(typeName, "instance");
        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        codeBuilder.addStatement("writer.beginObject()");
        for (int i = 0, j = fields.size(); i < j; i++) {
            FieldInfo field = fields.get(i);
            TypeName fieldType = field.getType();
            String fieldName = field.getFieldName();
            String jsonName = field.getJsonNames()[0];
            codeBuilder.addStatement("writer.name($S)", jsonName);
            if (fieldType.isPrimitive()) {
                codeBuilder.addStatement("writer.value(instance.$L)", fieldName);
            } else if (fieldType.isBoxedPrimitive() || STRING.equals(fieldType)) {
                if (field.isIgnoreNull()) {
                    codeBuilder.beginControlFlow("if (instance.$L != null)", fieldName);
                }
                codeBuilder.addStatement("writer.value(instance.$L)", fieldName);
                if (field.isIgnoreNull()) {
                    codeBuilder.endControlFlow();
                }
            } else if (field.hasMapper()) {
                codeBuilder.beginControlFlow("if (instance.$L != null)", fieldName);
                codeBuilder.addStatement("$L.serializeObject(writer, instance.$L)", field.getMapperFieldName(), fieldName);
                if (!field.isIgnoreNull()) {
                    codeBuilder.nextControlFlow("else");
                    codeBuilder.addStatement("writer.nullValue()");
                }
                codeBuilder.endControlFlow();
            } else if (fieldType instanceof ParameterizedTypeName) {
                codeBuilder.beginControlFlow("if (instance.$L != null)", fieldName);
                ClassName rawType = ((ParameterizedTypeName) fieldType).rawType;
                if (ClassName.get(List.class).equals(rawType)) {
                    TypeName actualType = ObjectModel.getFieldActualTypeName(fieldType);
                    assert actualType != null;
                    codeBuilder.addStatement("writer.beginArray()");
                    codeBuilder.beginControlFlow("for ($T item : instance.$L)", actualType, fieldName);
                    if (actualType.isBoxedPrimitive() || STRING.equals(actualType)) {
                        codeBuilder.addStatement("writer.value(item)", fieldName);
                    } else {
                        codeBuilder.addStatement("$L.serializeObject(writer, item)", field.getMapperFieldName());
                    }
                    codeBuilder.endControlFlow();
                    codeBuilder.addStatement("writer.endArray()");
                } else if (ClassName.get(Map.class).equals(rawType)) {
                    TypeName actualType = ObjectModel.getFieldActualTypeName(fieldType);
                    assert actualType != null;
                    codeBuilder.addStatement("writer.beginObject()");
                    codeBuilder.beginControlFlow("for ($T<$T, $T> item : instance.$L.entrySet())",
                            Map.Entry.class, String.class, actualType, fieldName);
                    codeBuilder.addStatement("writer.name(item.getKey())");
                    if (actualType.isBoxedPrimitive() || STRING.equals(actualType)) {
                        codeBuilder.addStatement("writer.value(item.getValue())", fieldName);
                    } else {
                        codeBuilder.addStatement("$L.serializeObject(writer, item.getValue())", field.getMapperFieldName());
                    }
                    codeBuilder.endControlFlow();
                    codeBuilder.addStatement("writer.endArray()");
                } else {
                    return fieldTypeUnsupported(typeName, fieldType, fieldName);
                }
                if (!field.isIgnoreNull()) {
                    codeBuilder.nextControlFlow("else");
                    codeBuilder.addStatement("writer.nullValue()");
                }
                codeBuilder.endControlFlow();
            } else if (fieldType instanceof ArrayTypeName) {
                codeBuilder.beginControlFlow("if (instance.$L != null)", fieldName);
                TypeName actualType = ObjectModel.getFieldActualTypeName(fieldType);
                assert actualType != null;
                codeBuilder.addStatement("writer.beginArray()");
                codeBuilder.beginControlFlow("for ($T item : instance.$L)", actualType, fieldName);
                if (actualType.isPrimitive() || actualType.isBoxedPrimitive() || STRING.equals(actualType)) {
                    codeBuilder.addStatement("writer.value(item)", fieldName);
                } else {
                    codeBuilder.addStatement("$L.serializeObject(writer, item)", field.getMapperFieldName());
                }
                codeBuilder.endControlFlow();
                codeBuilder.addStatement("writer.endArray()");
                if (!field.isIgnoreNull()) {
                    codeBuilder.nextControlFlow("else");
                    codeBuilder.addStatement("writer.nullValue()");
                }
                codeBuilder.endControlFlow();
            } else {
                return fieldTypeUnsupported(typeName, fieldType, fieldName);
            }
        }
        codeBuilder.addStatement("writer.endObject()");
        builder.addCode(codeBuilder.build());
        return builder.build();
    }

    private static MethodSpec fieldTypeUnsupported(TypeName typeName, TypeName fieldType, String fieldName) {
        throw new UnsupportedOperationException(String.format("Type %s of Field %s.%s is not supported",
                fieldType, typeName, fieldName));
    }

    private static boolean isBasicType(TypeName fieldType) {
        return fieldType.isPrimitive() || fieldType.isBoxedPrimitive() || STRING.equals(fieldType);
    }

    private static void assignParsedBasicType(CodeBlock.Builder codeBuilder, FieldInfo field, String jsonName,
                                              TypeName type, String assignmentFormat, boolean throwIfNull) {
        if (type.isPrimitive()) {
            codeBuilder.beginControlFlow("if (reader.peek() == $T.NULL)", JsonToken.class);
            if (!field.isIgnoreNull()) {
                if (throwIfNull) {
                    codeBuilder.addStatement("throw new $T($S)", IOException.class,
                            String.format(Locale.US, "%s is null", jsonName));
                } else {
                    codeBuilder.addStatement(assignmentFormat.replace("${readValue}", "null"));
                }
            } else {
                codeBuilder.add("// Ignored null value\n");
                codeBuilder.addStatement("reader.nextNull()");
            }
            codeBuilder.nextControlFlow("else");
            if (field.isIgnoreInvalid()) {
                codeBuilder.beginControlFlow("try");
            }
            if (TypeName.BOOLEAN.equals(type)) {
                String readValue = "reader.nextBoolean()";
                codeBuilder.addStatement(assignmentFormat.replace("${readValue}", readValue));
            } else if (TypeName.INT.equals(type)) {
                String readValue = "reader.nextInt()";
                codeBuilder.addStatement(assignmentFormat.replace("${readValue}", readValue));
            } else if (TypeName.LONG.equals(type)) {
                String readValue = "reader.nextLong()";
                codeBuilder.addStatement(assignmentFormat.replace("${readValue}", readValue));
            } else if (TypeName.FLOAT.equals(type)) {
                String readValue = "reader.nextFloat()";
                codeBuilder.addStatement(assignmentFormat.replace("${readValue}", readValue));
            } else if (TypeName.DOUBLE.equals(type)) {
                String readValue = "reader.nextDouble()";
                codeBuilder.addStatement(assignmentFormat.replace("${readValue}", readValue));
            } else if (TypeName.SHORT.equals(type)) {
                String readValue = "(short) reader.nextInt()";
                codeBuilder.addStatement(assignmentFormat.replace("${readValue}", readValue));
            } else if (TypeName.BYTE.equals(type)) {
                String readValue = "(byte) reader.nextInt()";
                codeBuilder.addStatement(assignmentFormat.replace("${readValue}", readValue));
            }
            if (field.isIgnoreInvalid()) {
                codeBuilder.nextControlFlow("catch (IllegalStateException e)");
                codeBuilder.add("// Ignore\n");
                codeBuilder.nextControlFlow("catch (IllegalArgumentException e)");
                codeBuilder.add("// Ignore\n");
                codeBuilder.endControlFlow();
            }
            codeBuilder.endControlFlow();
        } else if (type.isBoxedPrimitive() || STRING.equals(type)) {
            codeBuilder.beginControlFlow("if (reader.peek() == $T.NULL)", JsonToken.class);
            if (!field.isIgnoreNull()) {
                codeBuilder.addStatement(assignmentFormat.replace("${readValue}", "null"));
            } else {
                codeBuilder.add("// Ignored null value\n");
            }
            codeBuilder.addStatement("reader.nextNull()");
            codeBuilder.nextControlFlow("else");

            if (TypeName.get(Boolean.class).equals(type)) {
                String readValue = "reader.nextBoolean()";
                codeBuilder.addStatement(assignmentFormat.replace("${readValue}", readValue));
            } else if (TypeName.get(Integer.class).equals(type)) {
                String readValue = "reader.nextInt()";
                codeBuilder.addStatement(assignmentFormat.replace("${readValue}", readValue));
            } else if (TypeName.get(Long.class).equals(type)) {
                String readValue = "reader.nextLong()";
                codeBuilder.addStatement(assignmentFormat.replace("${readValue}", readValue));
            } else if (TypeName.get(Float.class).equals(type)) {
                String readValue = "reader.nextFloat()";
                codeBuilder.addStatement(assignmentFormat.replace("${readValue}", readValue));
            } else if (TypeName.get(Double.class).equals(type)) {
                String readValue = "reader.nextDouble()";
                codeBuilder.addStatement(assignmentFormat.replace("${readValue}", readValue));
            } else if (TypeName.get(Short.class).equals(type)) {
                String readValue = "(short) reader.nextInt()";
                codeBuilder.addStatement(assignmentFormat.replace("${readValue}", readValue));
            } else if (TypeName.get(Byte.class).equals(type)) {
                String readValue = "(byte) reader.nextInt()";
                codeBuilder.addStatement(assignmentFormat.replace("${readValue}", readValue));
            } else if (STRING.equals(type)) {
                String readValue = "reader.nextString()";
                codeBuilder.addStatement(assignmentFormat.replace("${readValue}", readValue));
            }

            codeBuilder.endControlFlow();
        } else {
            throw new AssertionError("Type " + type);
        }
    }

}
