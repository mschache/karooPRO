# KarooPRO — ride by effort on your Karoo

KarooPRO is a free, open-source extension for the **Hammerhead Karoo 3** (and Karoo 2) that puts your whole ride on one page.

The top of the page shows one big number: 3-second power, speed or heart rate. Tap it to switch between them. The other two sit right below. The number and the page are colored by your training zone, so you feel your effort before you read it.

Below that is a panel you flip with a tap on its left or right side.

| Panel | What it shows |
|---|---|
| **Ride** | Four tiles you choose, each with a 5-minute mini graph |
| **Effort** | 10-minute effort bars colored by zone, W′ balance, grade, VAM, ascent, terrain profile (the route ahead while navigating) |
| **Weather** | Head, tail or crosswind for your direction of travel, gusts, rain chance, temperature, and the next 6 hours (Open-Meteo) |
| **Lap** | Four lap fields with ▲/▼ against the previous lap |
| **Analysis** | Best 5 s / 1 min / 5 min / 20 min power, time in zones, NP, TSS, IF, climbing, work |
| **Workout** | Opens by itself during a structured workout: step, time left, target with PUSH / EASE / IN RANGE, step average, time in target |
| **Bike** | Opens by itself when Di2 or AXS connects: gears, drivetrain with cross-chain warning, batteries, L/R balance, shifts, ratio |

With no power meter, KarooPRO starts on speed and colors by heart rate.

**Workout alerts:**
- 10 seconds before a step ends you get a double beep and a countdown.
- 5-4-3-2-1 ticks are optional.
- When the next step starts, its target is shown for 5 seconds.
- Alerts can appear as a window over the panel or across the full field, plus a Karoo pop-up when you're on another page.

**Extra fields you can place anywhere:**
- **PRO Wind:** a compass that turns with your heading.
- **PRO W′ Balance:** in kJ.
- **PRO Headwind:** head (+) or tail (−) wind in your speed unit.

<p>
  <img src="docs/screenshot-1.png" width="240" alt="Workout panel">
  <img src="docs/screenshot-2.png" width="240" alt="Effort panel">
  <img src="docs/screenshot-3.png" width="240" alt="Bike panel">
  <img src="docs/screenshot-weather.png" width="240" alt="Weather panel">
  <img src="docs/screenshot-alert.png" width="240" alt="Next-step countdown">
</p>

## Map layers

While you navigate a route, KarooPRO draws on top of the Karoo's own map:

- **Wind on the route ahead (next 30 km):** green for tailwind, yellow for crosswind, red for headwind. Each point uses the Open-Meteo forecast for the hour you'll actually get there, based on your average speed.
- **Climbs by grade (next 50 km):** a grey-to-black band under the route, darker for steeper: 3–6 %, 6–9 %, 9–12 %, 12 %+.
- **Wind arrows every 5 km:** they point where the wind blows, in the same colors.

Each layer can be switched off in KarooPRO's settings. The layers need an active navigation with a GPS fix, and the wind needs WiFi or the Companion app.

## Install

**Karoo 3, with the Companion app:**
1. On your phone, open the [latest release](https://github.com/mschache/karooPRO/releases/latest).
2. Long-press `app-release.apk` and share the link to the **Hammerhead Companion** app.
3. Confirm the install on the Karoo. The Karoo must be on WiFi.
4. Later versions appear as updates in the Karoo's extension list.

**Karoo 2 or developers, with adb:**
1. Enable Developer options on the Karoo: Settings → About → tap Build number 7 times.
2. Turn on USB debugging.
3. Run:

```bash
adb install -r app-release.apk
```

## Set up the page

1. On the Karoo, open **Profiles** and choose or create a profile.
2. Go to **Data pages**, add a page and remove its default field.
3. Tap **Add field** → **KarooPRO** → **KarooPRO**. The field fills the page.

Now ride:
- Tap the top half to switch the big number.
- Tap the left or right third of the lower half to change panels.

You can also map **KarooPRO: next panel**, **previous panel** or **switch power/speed/HR** to a remote button as Karoo bonus actions.

## Settings

Open the **KarooPRO** app on the Karoo to:
- Hide or reorder panels, and choose whether Workout and Bike open by themselves.
- Pick the default big number, the zone color style (tint the page, fill the top, or color the number) and the theme.
- Choose the 4 Ride tiles and the 4 Lap fields.
- Set CP and W′ for W′ balance (CP defaults to your Karoo FTP; W′ to 20 kJ).
- Set the workout alert style, beeps and the pop-up on other pages.
- Turn weather on or off.

**Preview** shows the real field with demo data, so you can check every panel without riding.

## Good to know

- **Next workout step.** The Karoo shares only the running workout step. The countdown to the next step is exact, but its target appears when the step starts.
- **Weather.** Weather comes from [Open-Meteo](https://open-meteo.com). It needs WiFi or the Companion app, and is fetched only while the Weather panel or a wind field is in use: every 15 minutes, or after 3 km.
- **Battery levels.** Di2/AXS and power-meter batteries are shown as the levels the Karoo reports (new, good, OK, low, critical), not percentages.
- **Rendering.** The field is drawn as a single image about once per second, the Karoo's limit for extension views. When its page is not on screen, it stops drawing.

## Build

You need JDK 17+ and the Android SDK. karoo-ext comes from JitPack, so no GitHub token is needed.

```bash
./gradlew testDebugUnitTest assembleDebug
```

Releases are built by GitHub Actions from a `vX.Y.Z` tag. That tag must match `karoopro.version` in `gradle.properties`.

The release keystore is passed in through these repository secrets:
- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

The workflow attaches `app-release.apk` and `manifest.json`, which the Karoo uses to find updates.

## Credits and license

- **License:** KarooPRO is licensed under Apache-2.0.
- **SDK:** Built on Hammerhead's [karoo-ext](https://github.com/hammerheadnav/karoo-ext) (Apache-2.0).
- **Weather:** Data by [Open-Meteo.com](https://open-meteo.com) (CC BY 4.0).
- **Font:** Numbers use [Oswald](https://github.com/googlefonts/OswaldFont) (SIL Open Font License 1.1, see `LICENSES/OFL-Oswald.txt`).
- **Other open-source Karoo extensions (Apache-2.0 / MIT):**
  - The Karoo zone colors and the bitmap-number approach come from [BigNum](https://github.com/smartycoder/karoo-bignum).
  - The update throttle follows [ki2](https://github.com/valterc/ki2).
  - The wind math follows [Headwind](https://github.com/timklge/karoo-headwind).
  - Workout stream handling follows [Workout+](https://github.com/angkyria/karoo-workout-plus).

KarooPRO is not affiliated with Hammerhead or SRAM.
