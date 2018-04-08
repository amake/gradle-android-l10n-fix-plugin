package com.madlonkay.android;

import com.android.build.gradle.AppPlugin;
import com.android.build.gradle.BasePlugin;
import com.android.build.gradle.LibraryPlugin;
import com.android.build.gradle.api.AndroidSourceDirectorySet;
import com.android.build.gradle.api.AndroidSourceSet;
import com.android.build.gradle.internal.dsl.DefaultConfig;
import com.android.builder.internal.ClassFieldImpl;
import com.android.builder.model.ClassField;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.logging.LogLevel;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class L10nFixPlugin implements Plugin<Project> {
    private static final LogLevel LOG_LEVEL = LogLevel.DEBUG;
    private static final List<Class<? extends BasePlugin>> ANDROID_PLUGINS = Arrays.asList(AppPlugin.class, LibraryPlugin.class);
    private static final String DEFAULT_LOCALE = "en";
    private static final String SUPPORTED_LOCALES_FIELD_NAME = "SUPPORTED_LOCALES";
    private static final String SUPPORTED_LOCALES_FIELD_TYPE = "String[]";

    public void apply(Project project) {
        L10nFixExtension extension = project.getExtensions().create("l10n", L10nFixExtension.class);

        // Find all locales indicated by resources in all projects
        Set<String> resLocales = new HashSet<>();
        for (Project p : project.getRootProject().getAllprojects()) {
            for (Class<? extends BasePlugin> clazz : ANDROID_PLUGINS) {
                for (BasePlugin<?> plugin : p.getPlugins().withType(clazz)) {
                    resLocales.addAll(resolveLocales(p, plugin));
                }
            }
        }
        // Apply appropriate config to this project
        for (Class<? extends BasePlugin> clazz : ANDROID_PLUGINS) {
            project.getPlugins().withType(clazz, plugin -> doConfiguration(project, extension, plugin, resLocales));
        }
    }

    private void doConfiguration(Project project, L10nFixExtension extension, BasePlugin<?> plugin, Set<String> resLocales) {
        DefaultConfig defaultConfig = plugin.getExtension().getDefaultConfig();
        defaultConfig.addResourceConfigurations(resLocales);
        project.getLogger().log(LOG_LEVEL, "Resource configurations: {}", defaultConfig.getResourceConfigurations());

        // The rest must be done after evaluation so that the extension can be initialized
        project.afterEvaluate(p -> {
            List<String> bcp47Locales = new ArrayList<>(Util.toBcp47(resLocales));
            String defaultLocale = extension.getDefaultLocale();
            if (defaultLocale == null) {
                defaultLocale = DEFAULT_LOCALE;
            }
            bcp47Locales.add(defaultLocale);
            bcp47Locales.sort(Comparator.naturalOrder());

            String fieldValue = Util.toArrayLiteral(bcp47Locales);
            ClassField field = new ClassFieldImpl(SUPPORTED_LOCALES_FIELD_TYPE, SUPPORTED_LOCALES_FIELD_NAME, fieldValue);
            defaultConfig.addBuildConfigField(field);
            p.getLogger().log(LOG_LEVEL,  "{} = {}", SUPPORTED_LOCALES_FIELD_NAME, fieldValue);
        });
    }

    private Set<String> resolveLocales(Project project, BasePlugin<?> plugin) {
        Set<String> result = new HashSet<>();
        for (AndroidSourceSet sourceSet : plugin.getExtension().getSourceSets()) {
            AndroidSourceDirectorySet res = sourceSet.getRes();
            project.getLogger().log(LOG_LEVEL, "Inspecting {} {}", project.getName(), res.getName());
            for (File file : res.getSourceFiles()) {
                String locale = Util.resolveLocale(file.getPath());
                project.getLogger().log(LOG_LEVEL, "{} -> {}", file, locale);
                if (locale != null) {
                    result.add(locale);
                }

            }
        }
        return result;
    }
}
