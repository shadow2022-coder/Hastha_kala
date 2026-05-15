# Hasta-Kala Shop

An Android app built for small handcraft business owners in India — specifically for people who are still running their shop on paper registers and a calculator and would like to stop doing that.

---

## The problem this solves

If you've ever been inside a small handcraft shop in India, you've probably seen the owner flip through a worn notebook to check stock, scribble a bill on paper, or just guess at what's selling well. It works, kind of — until it doesn't. Stock runs out without warning. Sales data disappears. There's no way to know which products are actually making money.

And most apps that claim to solve this? They're in English, built for bigger businesses, and assume you have reliable internet.

Hasta-Kala Shop is built specifically for this gap. It's offline-first, works in Hindi, Kannada, and Telugu (not just English), and stays simple enough that someone who isn't particularly tech-savvy can pick it up quickly.

---

## What it does

**Dashboard** — The home screen shows you today's income, how many units you've sold, total products, and which items are running low. Everything at a glance, no digging around.

**Quick Sell** — A fast billing screen with a searchable product grid. You can type in quantities or tap `+`/`-`. It's designed to move fast when you have a customer in front of you.

**Inventory Management** — Add, edit, or delete products. Each product can have color variants, a stock count, and a price.

**AI Insights** — Connect your own API key from Gemini, OpenAI, Claude, or Mistral, and the app will generate business recommendations, pricing suggestions, and sales analysis in plain language. The responses can also be read aloud in your selected language, which matters if reading on a small screen is inconvenient.

**Settings** — Choose your AI provider, paste in your API key (it's encrypted locally, not sent anywhere else), pick your language, and set a low-stock threshold.

A few other things worth knowing: there's a search bar on most screens, the app never syncs to the cloud automatically (you control any exports), and there's no ad network or data collection baked in.

---

## Tech stack

Written in Kotlin with Jetpack Compose for the UI. Room handles local storage, Hilt manages dependency injection, and Retrofit talks to whichever AI provider you configure. API keys are stored with AndroidX Security (encrypted shared preferences). Charts come from MPAndroidChart.

The architecture is MVVM — UI layer talks to ViewModels, ViewModels talk to repositories, repositories talk to Room or the network. One activity, Navigation Compose for screen routing.

---

## Languages supported

English, Hindi, Kannada, and Telugu. You switch from Settings. AI responses also come back in your selected language.

---

## AI providers

The app supports four, all using a pluggable adapter so you can swap them out from Settings:

- **Google Gemini** (gemini-2.0-flash)
- **OpenAI** (gpt-4o-mini)
- **Anthropic Claude** (claude-3-haiku)
- **Mistral** (mistral-small-latest)

You bring your own API key. The app doesn't provide one. Keys are encrypted and stay on your device.

---

## Setting it up

You'll need Android Studio (Hedgehog or newer), JDK 17, and Android SDK targeting API 26 minimum / API 35 compile.

```bash
git clone https://github.com/<your-username>/giths2.git
cd giths2
```

Open the folder in Android Studio and let Gradle sync finish — it downloads all dependencies on its own. Android Studio should auto-generate `local.properties` with your SDK path. If it doesn't, create that file manually:

```properties
# macOS
sdk.dir=/Users/<username>/Library/Android/sdk

# Windows
sdk.dir=C\:\\Users\\<username>\\AppData\\Local\\Android\\Sdk

# Linux
sdk.dir=/home/<username>/Android/Sdk
```

If you want a signed release build, copy `keystore.properties.example` to `keystore.properties` and fill in your keystore details.

---

## Running it

In Android Studio: pick a device (emulator or physical with USB debugging on), hit Run.

From the terminal:

```bash
# macOS / Linux
./gradlew installDebug

# Windows
gradlew.bat installDebug
```

---

## Building an APK

Debug (for testing):

```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

Release (for distribution, requires keystore setup):

```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

To install on an emulator, drag and drop the APK onto the emulator window, or:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Project structure

```
giths2/
├── app/src/main/java/com/hastakala/shop/
│   ├── data/              # Room entities, DAOs, repositories
│   ├── di/                # Hilt modules
│   ├── network/ai/        # AI provider adapters (Gemini, OpenAI, Claude, Mistral)
│   ├── ui/                # Screens and ViewModels (home, sell, inventory, insights, settings)
│   └── util/              # Currency and time helpers
├── res/
│   ├── values/            # English strings
│   ├── values-hi/         # Hindi
│   ├── values-kn/         # Kannada
│   └── values-te/         # Telugu
```

---

## Running tests

```bash
./gradlew test
```

There are unit tests for Room database operations and the repository layer.

---

## What's missing (known gaps)

These aren't planned features — they're things that would make the app meaningfully more useful but aren't built yet:

- Barcode scanning for product lookup
- Sales reports with date range filtering
- Camera-based product images
- File-based backup and restore
- More Indian languages (Tamil, Malayalam, Bengali)
- PDF invoice generation
- Dark mode
- Multiple shop profiles

---

## A note on the project

This was built for a college submission, focused on a real problem: small handcraft business owners in India who don't have good tools in their own language. The code is structured to be extendable, but it's also genuinely trying to be useful, not just technically correct.
