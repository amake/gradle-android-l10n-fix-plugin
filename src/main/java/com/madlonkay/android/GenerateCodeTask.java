package com.madlonkay.android;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.lang.model.element.Modifier;


public class GenerateCodeTask extends DefaultTask {

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

        // Delete output dir in case package name changed, to prevent extraneous files
        getProject().delete(getOutputDirectory());

        boolean useSupportLib = dependsOnSupportLibrary(getProject());

        ClassName buildConfig = ClassName.get(buildConfigPackageName, "BuildConfig");
        ClassName activity = useSupportLib ? ClassName.get("android.support.v7.app", "AppCompatActivity")
                : ClassName.get("android.app", "Activity");
        ClassName context = ClassName.get("android.content", "Context");
        ClassName configuration = ClassName.get("android.content.res", "Configuration");
        ClassName resources = ClassName.get("android.content.res", "Resources");
        ClassName build = ClassName.get("android.os", "Build");
        ClassName localeList = ClassName.get("android.os", "LocaleList");
        ClassName requiresApi = ClassName.get("android.support.annotation", "RequiresApi");
        ParameterizedTypeName listOfLocale = ParameterizedTypeName.get(List.class, Locale.class);
        ParameterizedTypeName arrayListOfLocale = ParameterizedTypeName.get(ArrayList.class, Locale.class);

        ClassName ulocale = ClassName.get("android.icu.util", "ULocale");

        AnnotationSpec requiresApiN = AnnotationSpec.builder(requiresApi)
                .addMember("api", "$T.VERSION_CODES.N", build)
                .build();
        AnnotationSpec suppressDeprecation = AnnotationSpec.builder(SuppressWarnings.class)
                .addMember("value", "$S", "deprecation")
                .build();

        ClassName log = ClassName.get("android.util", "Log");
        String tag = "L10nFix";

        FieldSpec supportedLocales = FieldSpec.builder(listOfLocale, "SUPPORTED_LOCALES",
                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .build();

        CodeBlock supportedLocalesInit = CodeBlock.builder()
                .addStatement("$T list = $T.emptyList()", listOfLocale, Collections.class)
                .beginControlFlow("if ($T.VERSION.SDK_INT >= $T.VERSION_CODES.LOLLIPOP)", build, build)
                    .addStatement("list = new $T($T.SUPPORTED_LOCALES.length)", arrayListOfLocale, buildConfig)
                    .beginControlFlow("for (int i = 0; i < $T.SUPPORTED_LOCALES.length; i++)", buildConfig)
                        .addStatement("list.add($T.forLanguageTag($T.SUPPORTED_LOCALES[i]))", Locale.class, buildConfig)
                    .endControlFlow()
                    .addStatement("list = $T.unmodifiableList(list)", Collections.class)
                .endControlFlow()
                .addStatement("$N = list", supportedLocales)
                .build();

        MethodSpec.Builder isSupportedLocaleImplBuilder = MethodSpec.methodBuilder("isSupportedLocale")
                .addModifiers(Modifier.STATIC)
                .returns(boolean.class)
                .addParameter(Locale.class, "locale")
                .addParameter(listOfLocale, "supportedLocales")
                .beginControlFlow("for (int i = 0; i < supportedLocales.size(); i++)")
                    .addStatement("$T loc = supportedLocales.get(i)", Locale.class)
                    .beginControlFlow("if (loc.equals(locale))")
                        .addStatement("return true")
                    .nextControlFlow("else if (loc.getLanguage().equals(locale.getLanguage()))")
                        .addStatement("$T uloc = $T.addLikelySubtags($T.forLocale(loc))", ulocale, ulocale, ulocale)
                        .addStatement("$T ulocale = $T.addLikelySubtags($T.forLocale(locale))", ulocale, ulocale, ulocale)
                        .beginControlFlow("if (uloc.getScript().equals(ulocale.getScript()))")
                            .addStatement("return true")
                        .endControlFlow()
                    .endControlFlow()
                .endControlFlow()
                .addStatement("return false");
        if (useSupportLib) {
            isSupportedLocaleImplBuilder.addAnnotation(requiresApiN);
        }
        MethodSpec isSupportedLocaleImpl = isSupportedLocaleImplBuilder.build();

        MethodSpec.Builder isSupportedLocaleBuilder = MethodSpec.methodBuilder("isSupportedLocale")
                .addJavadoc("Whether or not the specified {@code $T} is supported by this app.", Locale.class)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(boolean.class)
                .addParameter(Locale.class, "locale")
                .addStatement("return $N(locale, $N)", isSupportedLocaleImpl, supportedLocales);
        if (useSupportLib) {
            isSupportedLocaleBuilder.addAnnotation(requiresApiN);
        }
        MethodSpec isSupportedLocale = isSupportedLocaleBuilder.build();

        MethodSpec.Builder filterUnsupportedLocalesBuilder = MethodSpec.methodBuilder("filterUnsupportedLocales")
                .addJavadoc("Remove locales not supported by this app from the provided {@code LocaleList}.")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(localeList)
                .addParameter(localeList, "locales")
                .addStatement("$T filtered = new $T(locales.size())", listOfLocale, arrayListOfLocale)
                .beginControlFlow("for (int i = 0; i < locales.size(); i++)")
                    .addStatement("$T loc = locales.get(i)", Locale.class)
                    .beginControlFlow("if ($N(loc))", isSupportedLocale)
                        .addStatement("filtered.add(loc)")
                    .endControlFlow()
                .endControlFlow()
                .addStatement("return new $T(filtered.toArray(new Locale[filtered.size()]))", localeList);
        if (useSupportLib) {
            filterUnsupportedLocalesBuilder.addAnnotation(requiresApiN);
        }
        MethodSpec filterUnsupportedLocales = filterUnsupportedLocalesBuilder.build();

        MethodSpec.Builder fixLocalesBuilder = MethodSpec.methodBuilder("fixLocales")
                .addJavadoc("Fix the specified {@code $T} to ensure that it only has locales supported by this app.", resources)
                .addJavadoc("Call this after runtime contamination, e.g. after loading {@code WebView}. ")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(void.class)
                .addAnnotation(suppressDeprecation)
                .addParameter(resources, "resources")
                .addStatement("$T config = resources.getConfiguration()", configuration)
                .addStatement("$T currentLocales = config.getLocales()", localeList)
                .beginControlFlow("if (!$N(currentLocales.get(0)))", isSupportedLocale)
                    .addStatement("$T supportedLocales = $N(currentLocales)", localeList, filterUnsupportedLocales)
                    .addStatement("$T.d($S, $S + currentLocales.toLanguageTags() + $S + supportedLocales.toLanguageTags())",
                            log, tag, "Fixing language tags; before=", "; after=")
                    .beginControlFlow("if (!supportedLocales.isEmpty())")
                        .addStatement("config.setLocales(supportedLocales)")
                        .addComment("updateConfiguration() is deprecated in SDK 25, but the alternative")
                        .addComment("requires restarting the activity, which we don't want to do here.")
                        .addStatement("resources.updateConfiguration(config, resources.getDisplayMetrics())")
                    .endControlFlow()
                .endControlFlow();
        if (useSupportLib) {
            fixLocalesBuilder.addAnnotation(requiresApiN);
        }
        MethodSpec fixLocales = fixLocalesBuilder.build();

        TypeSpec l10nUtil = TypeSpec.classBuilder("L10nUtil")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addField(supportedLocales)
                .addStaticBlock(supportedLocalesInit)
                .addMethod(isSupportedLocaleImpl)
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
                        .addStatement("$T.d($S, $S + currentLocales.toLanguageTags() + $S + supportedLocales.toLanguageTags())",
                                log, tag, "Fixing language tags: before=", "; after=")
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

    private static boolean dependsOnSupportLibrary(Project project) {
        for (Configuration configuration : project.getConfigurations()) {
            for (Dependency dependency : configuration.getAllDependencies()) {
                if ("com.android.support".equals(dependency.getGroup()) && "appcompat-v7".equals(dependency.getName())) {
                    return true;
                }
            }
        }
        return false;
    }
}
