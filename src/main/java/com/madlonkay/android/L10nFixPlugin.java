package com.madlonkay.android;

import com.android.build.gradle.AppPlugin;
import com.android.build.gradle.BasePlugin;
import com.android.build.gradle.LibraryPlugin;
import com.android.build.gradle.api.AndroidSourceDirectorySet;
import com.android.build.gradle.api.AndroidSourceSet;
import com.android.build.gradle.internal.dsl.DefaultConfig;
import com.android.builder.internal.ClassFieldImpl;
import com.android.builder.model.ClassField;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.LogLevel;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiConsumer;

public class L10nFixPlugin implements Plugin<Project> {
    private static final LogLevel LOG_LEVEL = LogLevel.DEBUG;
    private static final List<Class<? extends BasePlugin>> ANDROID_PLUGINS = Arrays.asList(AppPlugin.class, LibraryPlugin.class);
    private static final String DEFAULT_LOCALE = "en";
    private static final String SUPPORTED_LOCALES_FIELD_NAME = "SUPPORTED_LOCALES";
    private static final String SUPPORTED_LOCALES_FIELD_TYPE = "String[]";

    private static final Set<String> RES_LOCALES = new HashSet<>();
    private static final Set<String> SUPPORTED_LOCALES = new HashSet<>();

    public void apply(Project project) {
        L10nFixExtension extension = project.getExtensions().create("l10n", L10nFixExtension.class);

        // Find all locales indicated by resources in all projects
        Set<String> resLocales = new HashSet<>();
        iterProjects(project, (proj, plugin) -> resLocales.addAll(resolveLocales(proj, plugin)));

        // Apply appropriate resConfigs to all projects
        iterProjects(project, (proj, plugin) -> setResConfigs(proj, plugin, resLocales));

        // Add generate task only if this is the app
        project.getPlugins().withType(AppPlugin.class, plugin -> addGenerateActivityTask(project, plugin));

        // The rest must be done after evaluation so that the extension can be initialized
        iterPlugins(project, (proj, plugin) -> proj.afterEvaluate(p -> setBuildConfigField(p, extension, resLocales)));
    }

    private void iterProjects(Project project, BiConsumer<Project, BasePlugin<?>> consumer) {
        for (Project p : project.getRootProject().getAllprojects()) {
            iterPlugins(p, consumer);
        }
    }

    private void iterPlugins(Project project, BiConsumer<Project, BasePlugin<?>> consumer) {
        for (Class<? extends BasePlugin> clazz : ANDROID_PLUGINS) {
            for (BasePlugin<?> plugin : project.getPlugins().withType(clazz)) {
                consumer.accept(project, plugin);
            }
        }
    }

    private void setResConfigs(Project project, BasePlugin<?> plugin, Set<String> resLocales) {
        Set<String> storedResLocales = RES_LOCALES;
        storedResLocales.addAll(resLocales);
        DefaultConfig defaultConfig = plugin.getExtension().getDefaultConfig();
        defaultConfig.addResourceConfigurations(storedResLocales);
        project.getLogger().log(LOG_LEVEL, "{} resource configurations: {}", project.getName(), defaultConfig.getResourceConfigurations());
    }

    private void setBuildConfigField(Project project, L10nFixExtension extension, Set<String> resLocales) {
        String defaultLocale = extension.getDefaultLocale() != null ? extension.getDefaultLocale() : DEFAULT_LOCALE;

        iterProjects(project, (proj, plug) -> {
            Set<String> storedSupportedLocales = SUPPORTED_LOCALES;
            storedSupportedLocales.addAll(Util.toBcp47(resLocales));
            storedSupportedLocales.add(defaultLocale);

            List<String> bcp47Locales = new ArrayList<>(storedSupportedLocales);
            bcp47Locales.sort(Comparator.naturalOrder());
            String fieldValue = Util.toArrayLiteral(bcp47Locales);
            ClassField field = new ClassFieldImpl(SUPPORTED_LOCALES_FIELD_TYPE, SUPPORTED_LOCALES_FIELD_NAME, fieldValue);

            proj.getLogger().log(LOG_LEVEL,  "{}: {} = {}", proj.getName(), SUPPORTED_LOCALES_FIELD_NAME, fieldValue);
            plug.getExtension().getDefaultConfig().addBuildConfigField(field);
        });
    }

    private Set<String> resolveLocales(Project project, BasePlugin<?> plugin) {
        Set<String> result = new HashSet<>();
        for (AndroidSourceSet sourceSet : plugin.getExtension().getSourceSets()) {
            AndroidSourceDirectorySet res = sourceSet.getRes();
            if (sourceSet.getName().toLowerCase(Locale.ENGLISH).contains("test")) {
                continue;
            }
            project.getLogger().log(LOG_LEVEL, "Inspecting {} {}", project.getName(), res.getName());
            for (File file : res.getSourceFiles()) {
                String locale = Util.resolveLocale(file);
                project.getLogger().log(LOG_LEVEL, "{} -> {}", file, locale);
                if (locale != null) {
                    result.add(locale);
                }
            }
        }
        return result;
    }

    private void addGenerateActivityTask(Project project, AppPlugin plugin) {
        GenerateActivityTask task = project.getTasks().create(GenerateActivityTask.GENERATE_ACTIVITY_TASK_NAME, GenerateActivityTask.class);
        AndroidSourceSet sourceSet = plugin.getExtension().getSourceSets().getByName("main");
        sourceSet.getJava().srcDir(task.getOutputDirectory());
        project.getTasks().getByName("preBuild").getInputs().files(task.getOutputs());
        project.afterEvaluate(proj ->
                proj.getPlugins().withType(AppPlugin.class, plug ->
                        proj.getTasks().withType(GenerateActivityTask.class, t ->
                                t.getInputs().property("applicationId", plug.getExtension().getDefaultConfig().getApplicationId()))));
    }
}
