package ph.codeia.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

import javax.annotation.Generated;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import ph.codeia.meta.GeneratedFrom;
import ph.codeia.query.Results;

/**
 * This file is a part of the vanilla project.
 */

@AutoService(Processor.class)
public class AndroidContentProcessor extends AbstractProcessor {

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(GeneratedFrom.class.getCanonicalName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Elements e = processingEnv.getElementUtils();
        Types t = processingEnv.getTypeUtils();

        ClassName androidContent = ClassName.get("ph.codeia.androidutils", "AndroidContent");
        TypeSpec queryable = TypeSpec.interfaceBuilder("AndroidQueryable")
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariable(TypeVariableName.get("T"))
                .addMethod(MethodSpec.methodBuilder("query")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(androidContent, "source")
                        .returns(ParameterizedTypeName.get(
                                ClassName.get(Results.class),
                                TypeVariableName.get("T")))
                        .build())
                .build();

        TypeSpec.Builder facade = TypeSpec.classBuilder("GenerateQuery")
                .addJavadoc(Util.SIGNATURE)
                .addAnnotation(AnnotationSpec.builder(Generated.class)
                        .addMember("value", "{$S}", AndroidContentProcessor.class.getCanonicalName())
                        .addMember("date", "$S", Util.ISO_DATE_FORMAT.format(new Date()))
                        .build())
                .addModifiers(Modifier.PUBLIC)
                .addType(queryable);

        boolean shouldWrite = false;
        for (Element root : roundEnv.getElementsAnnotatedWith(GeneratedFrom.class)) {
            String target = root.getAnnotation(GeneratedFrom.class).value();
            TypeName targetTypeName = TypeName.get(e.getTypeElement(target).asType());
            ParameterizedTypeName returnTypeName = ParameterizedTypeName.get(
                    ClassName.get("ph.codeia.query", "GenerateQuery", "AndroidQueryable"),
                    targetTypeName);
            facade.addMethod(MethodSpec.methodBuilder("from")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(targetTypeName, "query", Modifier.FINAL)
                    .returns(returnTypeName)
                    .addStatement("return $L", TypeSpec.anonymousClassBuilder("")
                            .superclass(returnTypeName)
                            .addMethod(MethodSpec.methodBuilder("query")
                                    .addAnnotation(Override.class)
                                    .addModifiers(Modifier.PUBLIC)
                                    .addParameter(androidContent, "source")
                                    .returns(ParameterizedTypeName.get(
                                            ClassName.get(Results.class),
                                            targetTypeName))
                                    .addStatement("$1T q = new $1T(query)",
                                            TypeName.get(root.asType()))
                                    .addStatement("return source.get(" +
                                            "q.dataset(), q.projection(), " +
                                            "q.selection(), q.selectionArgs(), " +
                                            "q.sortOrder(), q)")
                                    .build())
                            .build())
                    .build());
            shouldWrite = true;
        }

        if (!shouldWrite) {
            return false;
        }

        try {
            JavaFile.builder("ph.codeia.query", facade.build())
                    .skipJavaLangImports(true)
                    .build().writeTo(processingEnv.getFiler());
        } catch (IOException ignored) {}

        return true;
    }
}
