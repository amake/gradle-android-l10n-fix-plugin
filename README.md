# Gradle Android L10N Fix Plugin

This plugin provides solutions for the [compile-time and runtime language
resource contamination
problems](https://gist.github.com/amake/0ac7724681ac1c178c6f95a5b09f03ce) that
break display-language fallback according to user preferences on Android 7 and
later.

## Do I need this?

- Does your app run on Android 7 (SDK 24) or later?
- Does your app support more than one display language?
- Do you use the AppCompat library? Any Google Play Services libraries?

If so, your app probably suffers from [language resource
contamination](https://gist.github.com/amake/0ac7724681ac1c178c6f95a5b09f03ce),
which prevents some users from seeing the app in their preferred language.

Go check how many localizations Google thinks you support on the Google Play Console:

![Too many localizations](web/google-play-console-localizations.png)

Surprised?

You can also check your APK directly yourself. Here's the result for the default
new project template app created by Android Studio 3.1:

```
$ ./gradlew assemble

BUILD SUCCESSFUL in 5s
51 actionable tasks: 1 executed, 50 up-to-date
$ $ANDROID_HOME/build-tools/27.0.3/aapt d --values resources app/build/outputs/apk/release/app-release-unsigned.apk resources.arsc | grep -E 'config ([a-z]{2}(-r[A-Z]{2})?|b\+[a-zA-Z+]+):' | sort | uniq
      config af:
      config am:
      config ar:
      config az:
      config b+sr+Latn:
      ⋮
      (plus 78 more)
```

## How does it work?

### Compile-time contamination fix

The compile-time fix is essentially a reimplementation of [`resConfig
'auto'`](https://google.github.io/android-gradle-dsl/3.1/com.android.build.gradle.internal.dsl.ProductFlavor.html#com.android.build.gradle.internal.dsl.ProductFlavor:resConfig%28java.lang.String%29),
which was [deprecated in Android Gradle Plugin
3.1](https://android.googlesource.com/platform/tools/base/+/6b7799c36f1ba5194f73f5c14a7b0365a8428714%5E%21/)
due to "issues with multi-module projects"*.

The plugin works by inspecting your `res` folder hierarchy to detect what
locales your app supports, and filters out unwanted locales by setting the
appropriate `resConfig` filters.

\*It's not clear exactly what the issues with `resConfig 'auto'` were, but this
plugin has been tested with multi-module projects and works correctly in that it
picks up all locales across all modules.

### Runtime contamination fix

The plugin offers two facilities for dealing with runtime resource contamination
from e.g. referencing the Chrome-based
[WebView](https://developer.android.com/reference/android/webkit/WebView.html)
class:

- `L10nActivity`: An `Activity` class that ensures that only supported locales
  are held by its base context. This is needed to keep locales correct after
  configuration changes such as device rotation.
- `L10nUtil`: A utility class offering static methods for dealing with supported
  locales, most importantly:
  - `fixLocales(Resources)`: Call this on your activity's resources immediately
    after referencing `WebView` to restore the correct locales.

The above features are backed by `BuildConfig.SUPPORTED_LOCALES`, an array of supported locales generated from the information collected for the compile-time contamination fix.

## Usage

1. Add the plugin to your root project
   ([instructions](https://plugins.gradle.org/plugin/com.madlonkay.android-l10n-fix))
2. Apply the plugin to your Android projects. These are any projects to which
   a `com.android.*` plugin is applied.
    ```
    apply plugin: 'com.madlonkay.android-l10n-fix'
    ```
   This activates the compile-time fix. Note:
    - This plugin should be applied *after* the Android plugin
    - Apply this plugin to all Android projects that have locale-specific
      resources
3. (Optional) Configure the plugin with a `l10n` block. See below for options.
4. To activate the runtime fix:
   1. Make your `Activity` classes extend `L10nActivity`
   2. Anywhere you first reference the `WebView` class (loading a layout
      containing a `WebView`, using any `WebView` static methods), immediately
      afterwards call `L10nUtil.fixLocales(getResources())`

## Configuration

In the same project you applied the plugin to:

```
l10n {
    defaultLocale = 'en'
}
```

Options:

- `defaultLocale`: The locale for "default" resources (i.e. in `values`, not
  `values-XX`). This defaults to `en`.

## License

Apache 2.0

## See also

- [Correct localization on Android
  7](https://gist.github.com/amake/0ac7724681ac1c178c6f95a5b09f03ce)
