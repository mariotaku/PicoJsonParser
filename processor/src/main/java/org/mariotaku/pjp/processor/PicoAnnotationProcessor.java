package org.mariotaku.pjp.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.mariotaku.pjp.PicoField;
import org.mariotaku.pjp.PicoObject;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

public class PicoAnnotationProcessor extends AbstractProcessor {
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(PicoObject.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Map<TypeName, ObjectModel> models = new HashMap<>();
        Filer filer = processingEnv.getFiler();
        for (Element type : roundEnv.getElementsAnnotatedWith(PicoObject.class)) {
            ObjectModel objectModel = ObjectModel.create((TypeElement) type);
            models.put(objectModel.getTypeName(), objectModel);
        }
        for (Element field : roundEnv.getElementsAnnotatedWith(PicoField.class)) {
            ObjectModel model = models.get(TypeName.get(field.getEnclosingElement().asType()));
            model.addField((VariableElement) field);
        }
        MapperClassGenerator generator = new MapperClassGenerator();
        for (ObjectModel model : models.values()) {
            model.complete(models);
            TypeSpec spec = generator.generate(model);
            ClassName typeName = (ClassName) model.getTypeName();
            try {
                JavaFile.builder(typeName.packageName(), spec).build().writeTo(filer);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }
}
