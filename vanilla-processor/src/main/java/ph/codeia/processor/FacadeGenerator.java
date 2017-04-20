package ph.codeia.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;

import java.io.IOException;
import java.util.Collection;
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

import ph.codeia.meta.GeneratedFrom;
import ph.codeia.query.Queryable;
import ph.codeia.query.Results;

/**
 * This file is a part of the vanilla project.
 */

@AutoService(Processor.class)
public class FacadeGenerator extends AbstractProcessor {

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

        TypeVariableName t = TypeVariableName.get("T");
        ParameterizedTypeName tCollection = ParameterizedTypeName.get(
                ClassName.get(Collection.class),
                WildcardTypeName.supertypeOf(t));

        TypeSpec.Builder facade = TypeSpec.classBuilder("GenerateQuery")
                .addJavadoc(Util.SIGNATURE)
                .addAnnotation(AnnotationSpec.builder(Generated.class)
                        .addMember("value", "{$S}", FacadeGenerator.class.getCanonicalName())
                        .addMember("date", "$S", Util.ISO_DATE_FORMAT.format(new Date()))
                        .build())
                .addModifiers(Modifier.PUBLIC)
                .addMethod(MethodSpec.methodBuilder("transfer")
                        .addModifiers(Modifier.STATIC)
                        .addTypeVariable(t)
                        .returns(TypeName.BOOLEAN)
                        .addParameter(ParameterizedTypeName.get(
                                ClassName.get(Results.class),
                                t), "results")
                        .addParameter(tCollection, "sink")
                        .addCode(CodeBlock.builder()
                                .addStatement("boolean ok = results.ok()")
                                .beginControlFlow("if (ok) for ($T row : results)", t)
                                .addStatement("sink.add(row)")
                                .endControlFlow()
                                .addStatement("results.dispose()")
                                .addStatement("return ok")
                                .build())
                        .build());

        boolean shouldWrite = false;
        for (Element root : roundEnv.getElementsAnnotatedWith(GeneratedFrom.class)) {
            String target = root.getAnnotation(GeneratedFrom.class).value();
            TypeName targetTypeName = TypeName.get(e.getTypeElement(target).asType());
            ParameterizedTypeName returnTypeName = ParameterizedTypeName.get(
                    ClassName.get(Queryable.Runner.class),
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
                                    .addParameter(Queryable.class, "source")
                                    .returns(ParameterizedTypeName.get(
                                            ClassName.get(Results.class),
                                            targetTypeName))
                                    .addStatement("$1T q = new $1T(query)",
                                            TypeName.get(root.asType()))
                                    .addStatement("return source.run(q, q)")
                                    .build())
                            .addMethod(MethodSpec.methodBuilder("drain")
                                    .addAnnotation(Override.class)
                                    .addModifiers(Modifier.PUBLIC)
                                    .addParameter(Queryable.class, "source")
                                    .addParameter(
                                            ParameterizedTypeName.get(
                                                    ClassName.get(Collection.class),
                                                    WildcardTypeName.supertypeOf(targetTypeName)),
                                            "sink")
                                    .returns(TypeName.BOOLEAN)
                                    .addStatement("return transfer(query(source), sink)")
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
                    .build()
                    .writeTo(processingEnv.getFiler());
        } catch (IOException ignored) {}

        return true;
    }
}
