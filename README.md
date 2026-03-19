# GreenGo 🌿

An eco-friendly travel app for Android built with Kotlin, Jetpack Compose, and Android 8+.

---

## Features

- 🗺️ **Eco Map** — Live map powered by OpenStreetMap and the Overpass API. Shows real-world locations across three filterable categories: Stay (hotels, hostels, guest houses, campsites, chalets, resorts and more), Cycling (bike rentals, repair stations, bicycle shops), and Nature (nature reserves, parks, gardens, national parks, forests, beaches, peaks, waterfalls, viewpoints and more). Markers load dynamically as you pan and zoom, with automatic fallback across multiple Overpass mirrors.
- 🌍 **Translator** — Real-time translation with speech input and text-to-speech output using ML Kit (17 languages)
- 👟 **Pedometer** — Step counter using the hardware step counter sensor
- 🎮 **Games** — Reef Rescuers ocean cleanup game, Eco Trivia, Wild Recall memory game
- 📬 **Contact** — In-app feedback form. Please just use it normally to send feedback. Do not attempt to probe, spam, inject anything, or abuse it in any way. Build and run the app as intended and leave it at that.
- ⚙️ **Settings** — Theme selection, preferences

---

## Requirements

- Android 8.0 (API 26) or higher
- See publishing section below

---

## Setup

1. Go to the [Releases](../../releases) section of this repo
2. Download **GreenGo.apk**
3. Transfer to your Android phone
4. Tap to install — allow "Install from unknown sources" if prompted

---

## Known Notes

### Map
Markers load live from Overpass API and may take a moment depending on which mirror responds. This is expected behavior from community-run public servers with no guaranteed uptime.

### Contact Form
Just build and run. Do not open, probe, or interact with the backend in any way outside of normal in-app use. No spamming, no XSS, no cyberattacks, no reports, nothing. If you mess with it, you will lose access to the services it relies on permanently. You have been warned.

### Sound
If sounds glitch while your phone is not on silent, that is not a bug. Android has audio session limits and an incoming call or a burst of rapid news notifications can interrupt playback. That is a system-level limitation, not something the app controls.

### Translation
ML Kit downloads language models on first use per language. An internet connection is required the first time you translate to a new language. After that it works offline.

### Tested Devices
The app has been tested on emulator only at this stage. Real device testing is pending. It is strongly recommended to use an Android 8+ compatible device only.

---

## Development Time

For transparency — the Android version of this app took approximately **30–40 hours** across multiple AI sessions and tools to build and stabilize. Kotlin/Compose, OSMDroid, ML Kit, the Vsync game loop, and matching the iOS architecture precisely were the main challenges. This is mentioned not as a complaint but so other developers know what to expect when porting a native iOS app to Android.

---

## Publishing — Read This Carefully

**Do not publish this app anywhere.** Not the App Store. Not Google Play. Not any other store, website, PWA, or social media account. All of the above lead to publishing and distribution which is strictly not permitted.

Use only your own personal device to sideload and test. That is all.

Do not create accounts anywhere to distribute this. Do not make a website for it. Do not post it anywhere.

If this app appears on any app store, website, PWA, or social media account, the author will take action with GitHub or any relevant company to ensure that person cannot develop or distribute software again. This is not a joke.

---

For license and full details see the iOS version at [https://github.com/eliekh05/GreenGo](https://github.com/eliekh05/GreenGo).
