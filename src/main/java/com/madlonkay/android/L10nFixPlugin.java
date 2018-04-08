package com.madlonkay.android;

import com.android.build.gradle.AppPlugin;
import com.android.build.gradle.BasePlugin;
import com.android.build.gradle.LibraryPlugin;
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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class L10nFixPlugin implements Plugin<Project> {
    private static final LogLevel LOG_LEVEL = LogLevel.DEBUG;
    private static final String DEFAULT_LOCALE = "en";
    private static final String SUPPORTED_LOCALES_FIELD_NAME = "SUPPORTED_LOCALES";
    private static final String SUPPORTED_LOCALES_FIELD_TYPE = "String[]";

    public void apply(Project project) {
        L10nFixExtension extension = project.getExtensions().create("l10n", L10nFixExtension.class);

        Action<BasePlugin> action = plugin -> doConfiguration(project, extension, plugin);
        project.getPlugins().withType(AppPlugin.class, action);
        project.getPlugins().withType(LibraryPlugin.class, action);
    }

    private void doConfiguration(Project project, L10nFixExtension extension, BasePlugin<?> plugin) {
        DefaultConfig defaultConfig = plugin.getExtension().getDefaultConfig();

        // This must be done earlier than `afterEvaluate` in order to take effect.
        // TODO: Figure out just how late we can do this
        Set<String> resLocales = resolveLocales(project);
        defaultConfig.addResourceConfigurations(resLocales);
        project.getLogger().log(LOG_LEVEL, "Resource configurations: " + defaultConfig.getResourceConfigurations());

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
            p.getLogger().log(LOG_LEVEL, SUPPORTED_LOCALES_FIELD_NAME + " = " + fieldValue);
        });
    }

    private Set<String> resolveLocales(Project project) {
        Set<String> result = new HashSet<>();
        ConfigurableFileTree tree = project.fileTree(project.getProjectDir());
        tree.include("**/res/**");
        project.getLogger().log(LOG_LEVEL, "Inspecting file tree: " + tree);
        for (File file : tree.getFiles()) {
            if (file.getPath().startsWith(project.getBuildDir().getPath())) {
                continue;
            }
            String locale = Util.resolveLocale(file.getPath());
            project.getLogger().log(LOG_LEVEL, file + " -> " + locale);
            if (locale != null) {
                result.add(locale);
            }
        }
        return result;
    }
}
