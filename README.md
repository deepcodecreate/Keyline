# Keyline

[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
![Platform](https://img.shields.io/badge/platform-Android_7.0%2B-3DDC84)
![Language](https://img.shields.io/badge/language-Java_17-orange)
![No ads](https://img.shields.io/badge/ads-none-success)

**Keyline** is a free and open-source Android keyboard (IME) built for people who type in more than one language, especially right-to-left languages such as Persian and Arabic. It combines a fast, themeable keyboard with a Gboard-style toolbar, one-handed and floating modes, clipboard history, an emoji panel, a text-editing pad and a built-in translator. There are no ads, no analytics and no tracking.

> Package name: `com.keyline.ime` · Minimum Android version: 7.0 (API 24) · Target SDK: 34

## Features

### Typing
- Layouts for **Persian, English, Arabic, German, Spanish, French, Russian and Turkish**
- Word suggestions, spell check, auto-correct, auto-capitalization and double-space period
- A personal dictionary that learns the words you use (disabled in password fields and in incognito mode)
- Adjustable key height (portrait and landscape), key opacity, font and font size
- Adjustable vibration strength, optional key sounds and key preview popups
- Voice typing through the speech recognizer installed on your device

### Toolbar and panels
- A Gboard-style toolbar: voice typing, translate, undo, clipboard, emoji and menu
- The toolbar gives way to word suggestions while you type and returns when there is nothing to suggest
- A menu with one-handed mode, floating mode, text editing, theme, settings, emoji, language switching and the password manager
- Text-editing pad with arrow keys, select mode, home/end, select all, cut, copy and paste
- Clipboard history with pinning, link filter and undo for deleted entries
- Emoji panel with categories and recent emoji
- Translator that works on the selected text or on the text field
- Menu and text-editing panels follow the language of the active keyboard layout

### One-handed and floating modes
- One-handed mode shrinks the whole keyboard towards one side, with animated resize and side switching
- Drag the edge handle to resize the keyboard directly
- Floating mode lets you move the keyboard anywhere on the screen and resize it with the corner handle
- Sliders in the settings for exact width and size

### Appearance
- Light and dark themes with custom colors
- Your own background image, with a built-in cropper and dim control
- Glass-style keys, adjustable key transparency, and the option to hide key backgrounds
- Custom fonts

### Customization and data
- Layouts are plain JSON files, with a built-in layout editor and a template for new languages
- Import and export of layouts
- Backup and restore of all settings
- Email and login suggestions from a local, encrypted vault (AES key stored in the Android Keystore)

## Privacy

- No ads, analytics, crash reporters or tracking libraries
- No Google Play Services and no proprietary dependencies
- Typed text stays on your device. The only feature that uses the network is the **translator**: when you use it, the text you choose to translate is sent to the Google Translate web service. Nothing is sent otherwise.
- Voice typing is handled by the speech recognizer installed on your device. Depending on your device it may process audio online.
- Suggestions and the personal dictionary are not learned in password fields or when incognito mode is on

### Permissions

| Permission | Why it is needed |
|---|---|
| `VIBRATE` | Haptic feedback when you press keys |
| `RECORD_AUDIO` | Voice typing (requested only when you use it) |
| `INTERNET` | The translator |

## Installation

Keyline is built from source. You can:

1. Build it yourself (see below), or
2. Download a release APK from the [Releases](https://github.com/deepcodecreate/Keyline/releases) page, or
3. Install it from F-Droid once it is available there.

After installing, open the app, enable **Keyline** in your system keyboard settings and select it as your input method.

## Building from source

Requirements: JDK 17, the Android SDK (platform 34), and an internet connection for Gradle dependencies.

```
git clone https://github.com/deepcodecreate/Keyline.git
cd Keyline
./gradlew assembleRelease
```

The APK is written to `app/build/outputs/apk/release/`.

To sign a release with your own key, create a `keystore.properties` file in the `app/` directory:

```
storeFile=/path/to/your.keystore
storePassword=...
keyAlias=...
keyPassword=...
```

This file is listed in `.gitignore` and must never be committed. Without it, the release build is signed with the debug key.

## Tech stack

| | |
|---|---|
| Language | Java 17 |
| Build system | Gradle 8.14 with the Android Gradle Plugin 8.7 |
| Min / target SDK | 24 / 34 |
| UI | Android Views (custom `KeyboardView`), AndroidX AppCompat, Material Components 1.12 |
| Storage | Room 2.6 for the user dictionary and clipboard history, SharedPreferences for settings |
| Emoji | AndroidX Emoji2 (bundled) |

## Project structure

```
app/src/main/
├── java/com/keyline/ime/
│   ├── ime/         Input method service and input logic
│   ├── view/        Keyboard view, toolbar, suggestion bar, menu, one-handed and floating root
│   ├── layout/      Layout model, parser and validator
│   ├── theme/       Themes, fonts and background images
│   ├── suggest/     Suggestion engine, trie and spell helper
│   ├── clipboard/   Clipboard history
│   ├── emoji/       Emoji panel
│   ├── translate/   Translator
│   ├── voice/       Voice typing
│   ├── security/    Encrypted credential vault
│   ├── settings/    Settings screens and preferences
│   └── db/          Room database
├── assets/
│   ├── layouts/     Keyboard layouts (JSON)
│   ├── dictionaries/ Word lists
│   └── emoji/       Emoji data
└── res/             Resources and translations
```

## Adding a language

1. Copy `app/src/main/assets/layouts/template.json` and edit the rows, labels and long-press popups.
2. Add its id to the `BUNDLED` list in `LayoutRepository.java`, or import the file from the app using the layout editor.
3. Add a word list to `app/src/main/assets/dictionaries/` if you want suggestions for the new language.

## Contributing

Bug reports, translations and pull requests are welcome. Please open an issue first for larger changes. Keep the code free of proprietary dependencies, because the project is meant to stay buildable with free software only.

## فارسی

**Keyline** یک کیبورد رایگان و متن‌باز اندروید است که برای نوشتن به چند زبان، به‌ویژه زبان‌های راست‌به‌چپ مثل فارسی و عربی ساخته شده. نوار ابزار شبیه Gboard، حالت یک‌دستی و شناور (با تغییر اندازه به‌صورت کشیدن)، تاریخچه کلیپ‌بورد، ایموجی، صفحه ویرایش متن، مترجم، تم و تصویر پس‌زمینه دلخواه از امکانات آن است. برنامه تبلیغ، آمارگیری و ردیابی ندارد. فقط مترجم از اینترنت استفاده می‌کند و متنی را که برای ترجمه انتخاب می‌کنید به Google Translate می‌فرستد.

زبان برنامه‌نویسی: Java · حداقل نسخه اندروید: ۷.۰ · مجوز: Apache 2.0

## License

Copyright 2026 deepcodecreate

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE) for details.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, without warranties or conditions of any kind.
