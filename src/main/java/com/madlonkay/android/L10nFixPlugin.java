package com.madlonkay.android;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.AppPlugin;
import com.android.build.gradle.BaseExtension;
import com.android.build.gradle.BasePlugin;
import com.android.build.gradle.LibraryExtension;
import com.android.build.gradle.LibraryPlugin;
import com.android.build.gradle.api.AndroidSourceDirectorySet;
import com.android.build.gradle.api.AndroidSourceSet;
import com.android.build.gradle.api.BaseVariant;
import com.android.build.gradle.internal.dsl.DefaultConfig;

import org.gradle.api.DomainObjectSet;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.logging.LogLevel;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

public class L10nFixPlugin implements Plugin<Project> {
    private static final String VERBOSITY_PROPERTY = "l10nFixVerbosity";
    private static final List<Class<? extends BasePlugin>> ANDROID_PLUGINS = Arrays.asList(AppPlugin.class, LibraryPlugin.class);
    private static final String DEFAULT_LOCALE = "en";
    private static final String SUPPORTED_LOCALES_FIELD_NAME = "SUPPORTED_LOCALES";
    private static final String SUPPORTED_LOCALES_FIELD_TYPE = "String[]";

    private static final Set<String> RES_LOCALES = new HashSet<>();
    private static final Set<String> BCP47_LOCALES = new HashSet<>();

    private int verbosity;

    @Override
    public void apply(Project project) {
        verbosity = Util.readIntProperty(project, VERBOSITY_PROPERTY, 0);

        L10nFixExtension extension = project.getExtensions().create("l10n", L10nFixExtension.class);

        if (RES_LOCALES.isEmpty()) {
            // Find all locales indicated by resources in all projects
            iterProjects(project, proj -> resolveLocalesFileSystem(proj, RES_LOCALES));
            logInfo(project, "Detected resource locales in filesystem: {}", RES_LOCALES);
        }

        // Apply appropriate resConfigs to project
        iterPlugins(project, plugin ->
                setResConfigs(project, plugin, Collections.unmodifiableSet(RES_LOCALES)));

        // Add code-generation tasks to all variants
        iterVariants(project, variant ->
                addGenerateCodeTask(project, variant));

        // The rest must be done after evaluation so that the extensions can be initialized
        project.afterEvaluate(proj -> {
            // Check for locales that couldn't be detected by [resolveLocalesFileSystem]
            Set<String> missingLocales = new HashSet<>();
            iterPlugins(proj, plugin -> resolveLocalesActual(proj, plugin, missingLocales));
            missingLocales.removeAll(RES_LOCALES);
            if (!missingLocales.isEmpty()) {
                logWarn(proj, "Project contains resources with locales that must be manually specified in `resConfigs`: {}", missingLocales);
            }

            iterProjects(proj, p ->
                    iterPlugins(p, plugin ->
                            resolveConfiguredLocales(p, plugin, RES_LOCALES)));
            Util.transformInto(RES_LOCALES, Util::toBcp47, BCP47_LOCALES);
            BCP47_LOCALES.add(getDefaultLocale(proj, extension));
            iterProjects(proj, p ->
                    iterVariants(p, variant ->
                            setBuildConfigField(p, variant, Collections.unmodifiableSet(BCP47_LOCALES))));
        });
    }

    private void setResConfigs(Project project, BasePlugin<?> plugin, Collection<String> resLocales) {
        DefaultConfig defaultConfig = plugin.getExtension().getDefaultConfig();
        if (Util.containsLocaleQualifier(defaultConfig.getResourceConfigurations())) {
            logInfo(project, "Manual resource configurations present; skipping");
        } else {
            defaultConfig.addResourceConfigurations(resLocales);
            logInfo(project, "Adding resource configurations to {}: {}", project.getName(), resLocales);
            logDebug(project, "...result: {}", defaultConfig.getResourceConfigurations());
        }
    }

    private String getDefaultLocale(Project project, L10nFixExtension extension) {
        String defaultLocale = extension.getDefaultLocale();
        if (defaultLocale == null) {
            logDebug(project, "{} default locale not specified; using default ({})", project.getName(), DEFAULT_LOCALE);
            defaultLocale = DEFAULT_LOCALE;
        } else {
            logDebug(project, "{} default locale: {}", project.getName(), defaultLocale);
        }
        return Util.toBcp47(defaultLocale);
    }

    private void setBuildConfigField(Project project, BaseVariant variant, Collection<String> bcp47Locales) {
        List<String> localeList = new ArrayList<>(bcp47Locales);
        localeList.sort(Comparator.naturalOrder());
        String fieldValue = Util.toArrayLiteral(localeList);

        logInfo(project, "{} ({}): {} = {}", project.getName(), variant.getName(), SUPPORTED_LOCALES_FIELD_NAME, fieldValue);
        variant.buildConfigField(SUPPORTED_LOCALES_FIELD_TYPE, SUPPORTED_LOCALES_FIELD_NAME, fieldValue);
    }

    private void resolveLocalesFileSystem(Project project, Collection<String> outLocales) {
        ConfigurableFileTree tree = project.fileTree(project.getProjectDir());
        tree.include("**/values/**");
        tree.include("**/values-*/**");
        tree.exclude("**/build/**", "**/test/**", "**/androidTest/**");
        logDebug(project, "Inspecting file tree: {}", tree);
        for (File file : tree.getFiles()) {
            String locale = Util.resolveLocale(file);
            logDebug(project, "{} -> {}", file, locale);
            if (locale != null) {
                outLocales.add(locale);
            }
        }
    }

    private void resolveLocalesActual(Project project, BasePlugin<?> plugin, Collection<String> outLocales) {
        for (AndroidSourceSet sourceSet : plugin.getExtension().getSourceSets()) {
            AndroidSourceDirectorySet res = sourceSet.getRes();
            if (sourceSet.getName().toLowerCase(Locale.ENGLISH).contains("test")) {
                continue;
            }
            logDebug(project, "Inspecting {} {}", project.getName(), res.getName());
            for (File file : res.getSourceFiles()) {
                String locale = Util.resolveLocale(file);
                logDebug(project, "{} -> {}", file, locale);
                if (locale != null) {
                    outLocales.add(locale);
                }
            }
        }
    }

    private void addGenerateCodeTask(Project project, BaseVariant variant) {
        String taskName = Util.makeTaskName("generate", variant.getFlavorName(), variant.getBuildType().getName(), "L10nFix");
        GenerateCodeTask task = project.getTasks().create(taskName, GenerateCodeTask.class);
        logDebug(project, "Generating task: {}", task.getPath());
        task.setBuildConfigPackageName(variant.getGenerateBuildConfig().getBuildConfigPackageName());
        variant.registerJavaGeneratingTask(task, task.getOutputDirectory());
    }

    private void resolveConfiguredLocales(Project project, BasePlugin<?> plugin, Collection<String> outLocales) {
        for (String config : plugin.getExtension().getDefaultConfig().getResourceConfigurations()) {
            if (Util.isLocaleQualifier(config)) {
                outLocales.add(config);
            }
        }
    }

    private static void iterProjects(Project project, Consumer<Project> consumer) {
        for (Project p : project.getRootProject().getAllprojects()) {
            consumer.accept(p);
        }
    }

    private static void iterPlugins(Project project, Consumer<BasePlugin<?>> consumer) {
        for (Class<? extends BasePlugin> clazz : ANDROID_PLUGINS) {
            for (BasePlugin<?> plugin : project.getPlugins().withType(clazz)) {
                consumer.accept(plugin);
            }
        }
    }

    private static void iterVariants(Project project, Consumer<BaseVariant> consumer) {
        iterPlugins(project, plugin -> {
            BaseExtension extension = plugin.getExtension();
            DomainObjectSet<? extends BaseVariant> variants;
            if (extension instanceof AppExtension) {
                variants = ((AppExtension) extension).getApplicationVariants();
            } else if (extension instanceof LibraryExtension) {
                variants = ((LibraryExtension) extension).getLibraryVariants();
            } else {
                return;
            }
            variants.all(consumer::accept);
        });
    }

    private void logWarn(Project project, String format, Object... args) {
        log(project, LogLevel.WARN, format, args);
    }
    private void logInfo(Project project, String format, Object... args) {
        log(project, LogLevel.INFO, format, args);
    }
    private void logDebug(Project project, String format, Object... args) {
        log(project, LogLevel.DEBUG, format, args);
    }

    private void log(Project project, LogLevel level, String format, Object... args) {
        if (verbosity > 0) {
            int adjustedLevelIndex = Arrays.binarySearch(LogLevel.values(), level) + verbosity;
            adjustedLevelIndex = Math.max(0, Math.min(adjustedLevelIndex, LogLevel.values().length));
            level = LogLevel.values()[adjustedLevelIndex];
        }
        project.getLogger().log(level, format, args);
    }
}
