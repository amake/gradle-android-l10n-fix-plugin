package com.madlonkay.android;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.lang.model.element.Modifier;


public class GenerateCodeTask extends DefaultTask {

    public static final String GENERATE_CODE_TASK_NAME = "generateL10nFix";

    private String buildConfigPackageName;

    @Input
    public String getBuildConfigPackageName() {
        return buildConfigPackageName;
    }

    public void setBuildConfigPackageName(String buildConfigPackageName) {
        this.buildConfigPackageName = buildConfigPackageName;
    }

    @OutputDirectory
    public File getOutputDirectory() {
        return new File(getProject().getBuildDir(), "generated/source/l10nFix");
    }

    @TaskAction
    public void generate() throws IOException {
        Objects.requireNonNull(buildConfigPackageName, "Could not determine the app's buildConfigPackageName");

        ClassName buildConfig = ClassName.get(buildConfigPackageName, "BuildConfig");
        ClassName activity = ClassName.get("android.support.v7.app", "AppCompatActivity");
        ClassName context = ClassName.get("android.content", "Context");
        ClassName configuration = ClassName.get("android.content.res", "Configuration");
        ClassName resources = ClassName.get("android.content.res", "Resources");
        ClassName build = ClassName.get("android.os", "Build");
        ClassName localeList = ClassName.get("android.os", "LocaleList");
        ClassName requiresApi = ClassName.get("android.support.annotation", "RequiresApi");
        ParameterizedTypeName listOfLocale = ParameterizedTypeName.get(List.class, Locale.class);
        ParameterizedTypeName arrayListOfLocale = ParameterizedTypeName.get(ArrayList.class, Locale.class);

        AnnotationSpec requiresApiN = AnnotationSpec.builder(requiresApi)
                .addMember("api", "$T.VERSION_CODES.N", build)
                .build();
        AnnotationSpec requiresApiLollipop = AnnotationSpec.builder(requiresApi)
                .addMember("api", "$T.VERSION_CODES.LOLLIPOP", build)
                .build();
        AnnotationSpec suppressDeprecation = AnnotationSpec.builder(SuppressWarnings.class)
                .addMember("value", "$S", "deprecation")
                .build();

        MethodSpec isSupportedLocale = MethodSpec.methodBuilder("isSupportedLocale")
                .addJavadoc("Whether or not the specified {@code $T} is supported by this app.", Locale.class)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(boolean.class)
                .addAnnotation(requiresApiN)
                .addParameter(Locale.class, "locale")
                .beginControlFlow("for (int i = 0; i < $T.SUPPORTED_LOCALES.length; i++)", buildConfig)
                    .addStatement("$T loc = $T.SUPPORTED_LOCALES[i]", String.class, buildConfig)
                    .beginControlFlow("if (loc.equals(locale.getLanguage()) || loc.equals(locale.toLanguageTag()))")
                        .addStatement("return true")
                    .endControlFlow()
                .endControlFlow()
                .addStatement("return false")
                .build();

        MethodSpec filterUnsupportedLocales = MethodSpec.methodBuilder("filterUnsupportedLocales")
                .addJavadoc("Remove locales not supported by this app from the provided {@code LocaleList}.")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(localeList)
                .addAnnotation(requiresApiLollipop)
                .addParameter(localeList, "locales")
                .addStatement("$T filtered = new $T(locales.size())", listOfLocale, arrayListOfLocale)
                .beginControlFlow("for (int i = 0; i < locales.size(); i++)")
                    .addStatement("$T loc = locales.get(i)", Locale.class)
                    .beginControlFlow("if ($N(loc))", isSupportedLocale)
                        .addStatement("filtered.add(loc)")
                    .endControlFlow()
                .endControlFlow()
                .addStatement("return new $T(filtered.toArray(new Locale[filtered.size()]))", localeList)
                .build();

        MethodSpec fixLocales = MethodSpec.methodBuilder("fixLocales")
                .addJavadoc("Fix the specified {@code $T} to ensure that it only has locales supported by this app.", resources)
                .addJavadoc("Call this after runtime contamination, e.g. after loading {@code WebView}. ")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(void.class)
                .addAnnotation(requiresApiN)
                .addAnnotation(suppressDeprecation)
                .addParameter(resources, "resources")
                .addStatement("$T config = resources.getConfiguration()", configuration)
                .addStatement("$T currentLocales = config.getLocales()", localeList)
                .beginControlFlow("if (!$N(currentLocales.get(0)))", isSupportedLocale)
                    .addStatement("$T supportedLocales = $N(currentLocales)", localeList, filterUnsupportedLocales)
                    .beginControlFlow("if (!supportedLocales.isEmpty())")
                        .addStatement("config.setLocales(supportedLocales)")
                        .addComment("updateConfiguration() is deprecated in SDK 25, but the alternative")
                        .addComment("requires restarting the activity, which we don't want to do here.")
                        .addStatement("resources.updateConfiguration(config, resources.getDisplayMetrics())")
                    .endControlFlow()
                .endControlFlow()
                .build();

        TypeSpec l10nUtil = TypeSpec.classBuilder("L10nUtil")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(isSupportedLocale)
                .addMethod(filterUnsupportedLocales)
                .addMethod(fixLocales)
                .build();

        JavaFile.builder(buildConfigPackageName, l10nUtil)
                .build()
                .writeTo(getOutputDirectory());

        MethodSpec attachBaseContext = MethodSpec.methodBuilder("attachBaseContext")
                .addModifiers(Modifier.PROTECTED)
                .returns(void.class)
                .addAnnotation(Override.class)
                .addParameter(context, "base")
                .beginControlFlow("if ($T.VERSION.SDK_INT >= $T.VERSION_CODES.N)", build, build)
                    .addStatement("$T currentLocales = base.getResources().getConfiguration().getLocales()", localeList)
                    .beginControlFlow("if (!$N.$N(currentLocales.get(0)))", l10nUtil, isSupportedLocale)
                        .addStatement("$T supportedLocales = $N.$N(currentLocales)", localeList, l10nUtil, filterUnsupportedLocales)
                        .beginControlFlow("if (!supportedLocales.isEmpty())")
                            .addStatement("$T config = new $T()", configuration, configuration)
                            .addStatement("config.setLocales(supportedLocales)")
                            .addStatement("base = base.createConfigurationContext(config)")
                        .endControlFlow()
                    .endControlFlow()
                .endControlFlow()
                .addStatement("super.attachBaseContext(base)")
                .build();

        TypeSpec l10nActivity = TypeSpec.classBuilder("L10nFixActivity")
                .superclass(activity)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addMethod(attachBaseContext)
                .build();

        JavaFile.builder(buildConfigPackageName, l10nActivity)
                .build()
                .writeTo(getOutputDirectory());
    }
}