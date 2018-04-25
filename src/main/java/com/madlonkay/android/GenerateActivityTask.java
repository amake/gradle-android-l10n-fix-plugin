package com.madlonkay.android;

import com.android.build.gradle.AppPlugin;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.lang.model.element.Modifier;


public class GenerateActivityTask extends DefaultTask {

    public static final String GENERATE_ACTIVITY_TASK_NAME = "generateL10nFixActivity";

    @OutputDirectory
    public File getOutputDirectory() {
        return new File(getProject().getBuildDir(), "generated/source/l10nFix");
    }

    @TaskAction
    public void generate() throws IOException {
        String applicationId = null;
        for (AppPlugin plugin : getProject().getPlugins().withType(AppPlugin.class)) {
            applicationId = plugin.getExtension().getDefaultConfig().getApplicationId();
        }
        Objects.requireNonNull(applicationId, "Could not determine the Android app's applicationId");

        ClassName buildConfig = ClassName.get(applicationId, "BuildConfig");
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
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addAnnotation(requiresApiN)
                .addAnnotation(suppressDeprecation)
                .addStatement("$T resources = getResources()", resources)
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

        MethodSpec attachBaseContext = MethodSpec.methodBuilder("attachBaseContext")
                .addModifiers(Modifier.PROTECTED)
                .returns(void.class)
                .addAnnotation(Override.class)
                .addParameter(context, "base")
                .beginControlFlow("if ($T.VERSION.SDK_INT >= $T.VERSION_CODES.N)", build, build)
                    .addStatement("$T currentLocales = base.getResources().getConfiguration().getLocales()", localeList)
                        .beginControlFlow("if (!$N(currentLocales.get(0)))", isSupportedLocale)
                            .addStatement("$T supportedLocales = $N(currentLocales)", localeList, filterUnsupportedLocales)
                            .beginControlFlow("if (!supportedLocales.isEmpty())")
                                .addStatement("$T config = new $T()", configuration, configuration)
                                .addStatement("config.setLocales(supportedLocales)")
                                .addStatement("base = base.createConfigurationContext(config)")
                            .endControlFlow()
                        .endControlFlow()
                    .endControlFlow()
                .addStatement("super.attachBaseContext(base)")
                .build();

        TypeSpec l10nActivity = TypeSpec.classBuilder("L10nActivity")
                .superclass(activity)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addMethod(isSupportedLocale)
                .addMethod(filterUnsupportedLocales)
                .addMethod(fixLocales)
                .addMethod(attachBaseContext)
                .build();

        JavaFile javaFile = JavaFile.builder(applicationId, l10nActivity)
                .build();
        javaFile.writeTo(getOutputDirectory());
    }
}
