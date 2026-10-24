# What Karoo riders ask for, and where KarooPRO fits

Research date: 2026-10-24. Scope: Karoo 2 and Karoo 3 (the 2024 "Karoo") riders. Evidence comes mostly from 2024 to 2026, with older Karoo 2 threads used where the request is still open. Numbers in brackets are sources (see [Sources](#6-sources)).

**How the evidence was gathered (read this before trusting the counts)**
- **Hammerhead community forum:** all 4,361 posts were pulled through the forum's public Zendesk API [1] and grouped by title keywords. A "post" count includes bug reports on the same topic. "Votes" are net up-votes. Counts are given all-time and since 2024-05-01 (Karoo 3 launch, "K3 era").
- **GitHub:** issues, stars and release-asset downloads come from `gh api` [77]. Release downloads count updates as well as first installs. Library-listed extensions also have their `manifest.json` and icons polled by the Karoo, so only the APK count is used here, and it is a rough proxy.
- **Connect IQ:** download figures come from the store API [79–83]. They look like floor buckets (1k / 10k / 100k / 1M), so read them as "at least".
- **Reddit was not reachable.** reddit.com blocked both fetches and the search tool, so this report contains no Reddit evidence. One GitHub issue says its request was first posted on the Karoo subreddit [70]. That is the only trace.
- **Quotes:** I kept to one direct quote in the whole document and paraphrased every other rider statement with attribution.

---

## 1. Summary

- **The most-asked-for things that Hammerhead hasn't fully solved:**
  - **Effort-aware fueling.** Eat/drink reminders were the loudest request for years: 11 posts and 163 votes [1][18][19]. Native Custom Alerts (Dec 2025) now handle time, distance and calorie reminders [44][56], but there is still no carb balance or intake logging.
  - **Denser layouts and map-page fields.** 34 posts and 235 votes [1][27][28][29][36].
  - **Better climb tools.** 33 K3-era posts and 110 votes [1][4][5][6].
  - **Smarter radar and radar-triggered lights.** 40 K3-era radar posts [1][8][37].
  - **Crash detection.** 136 votes over 9 posts [1][20][21].
  - **Garmin-style pacing (Power Guide, real-time stamina).** [10][62][64]
- **Adoption, not forum votes, shows the wind demand.** Headwind has the most APK downloads of the open-source library extensions measured [77], and WindField has at least 100k Connect IQ downloads [79].
- **Hammerhead shipped many old asks in 2025–26:** extension store, music control, bell, custom alerts, Climber colors, Next Primary Target field, trail maps, Karoo Share [53]. The open gaps are now mostly analytics and pacing, not basic plumbing.
- **Third-party extensions now cover** crash detection, POIs, music, ghost racing, nutrition plans, weather and W′ [45][65]. Several are paid or subscription-based: Saifely, Best Bike Split Pro, Xert [45][54].

**Best opportunities for KarooPRO**
1. **Route pacing plus a stamina gauge** (the Power Guide / real-time-stamina gap). KarooPRO already has W′, climbs, wind and route elevation. [7][10][62][64]
2. **A fuel panel**: carbs burned versus eaten from power, tap or button logging, effort-aware nudges, and FIT fields. [16][17][18][70][81]
3. **A slim "PRO strip" field for the map page** that carries KarooPRO-only values (W′, wind, time to top, segment gap). [27][28][29][72][73][82]
4. **A gear-limit beep** for AXS and Di2, a cheap win on top of the existing Bike panel. [22][23][67]
5. **An in-ride results list** of completed climbs and segments with power stats. [12][13][24]

---

## 2. Ranked demand table

Legend:
- **Native?** is checked against Karoo docs and release notes up to build 1.663.2548 (2026-10-15) [50][53].
- **KP** = KarooPRO v0.2.3 coverage [84].
- **Feasible** = can a karoo-ext 1.1.9 extension do it [66].

| # | Feature | Demand evidence | Native on Karoo? | Extension today | KP covers? | Feasible? | Sources |
|---|---|---|---|---|---|---|---|
| 1 | **Fueling: eat/drink reminders → carb/energy tracking** | **Forum:** 11 title posts, 163 votes, 131 comments all-time (top: 39 and 38 votes, 2021). Another 3 generic reminder/alarm posts add about 50 votes. 5 K3-era posts, 12 votes, 36 comments. One request marked "not planned" in 2022. **Reviews:** the5krunner con list; TrainerRoad switcher. **Adoption:** karoo-reminder about 12.0k APK downloads; Connect IQ Eat! Drink! Reminder! at least 100k | **Partly.** Custom Alerts (one-time, repeat, event-based) since 1.600.2303 (2025-12-11). No carb or intake model | karoo-reminder, nomride, KSafe (fuel tracking), CrankBite (library 2026-10), Vin HkE | No | Yes | [1][16][17][18][19][42][44][56][61][62][70][77][81] |
| 2 | **Denser layouts / more fields on the map page / HUD** | **Forum:** 34 title posts, 235 votes all-time ("Use the Space!" 66 votes, 2021); K3 era 13 posts, 69 votes. Includes fields hidden before turns (14 votes), narrower fields (11), three per row (8). **GitHub:** KDouble 45★, 11.4k APK; Barberfish 37★, with HUD issues for fields from other extensions and multiple HUDs. **Connect IQ:** All In One at least 1M | **Partly** (fixed Karoo layouts) | KDouble, Barberfish, KStack, BigNum, Vin HkE | Partly (full page only; no slim strip) | Yes | [1][11][27][28][29][36][62][72][73][77][82] |
| 3 | **Climb tools beyond Climber** (colors, time to top, zoom to what's left, climb stats, custom climb page) | **Forum:** 93 title posts, 312 votes all-time (includes bugs); K3 era 33 posts, 110 votes. Grade colors: 23 votes, 87 comments (fixed in 1.634.2440, 2026-05-27). Time-for-climb field: 10 votes. Zoom/remaining-only: 10 votes. **Reviews:** the5krunner con (can't tune climb parameters) | **Yes, mostly:** Climber, climb data fields, customizable drawer. No time-to-top field | 7Climb (library), RouteGraph (35★), Climber+, ALTGRAPH | **Yes** (Climb panel: time to top, W′ empty-in, colored profile) | Yes | [1][4][5][6][24][30][46][50][53][61][65][75] |
| 4 | **Radar extras and radar-triggered lights** | **Forum:** K3 era 40 radar posts, 79 votes, 189 comments. Radar auto light: 13 votes, 18 comments. Turn light off when paused: 9 votes. "All clear" tone: 18 votes (2021). MyBikeTraffic: 16 votes (2022). **GitHub:** kxradar 30★, eiRadar 28★, KarooFireFly 37★. **Connect IQ:** My Bike Radar Traffic at least 100k | **Partly.** Radar sidebar, 3 tones. Lights turn on and off only at ride start/stop | kxradar, eiRadar, RadarCount, KarooFireFly (radar flash via an undocumented API), LightONKaroo | No | Radar data yes. **Light control no** (no karoo-ext API) | [1][8][37][39][40][48][49][65][66][76][77][80] |
| 5 | **Wind relative to heading, and weather on route** | **Forum:** few posts (5 titles, 33 votes). Wind was "not planned" in 2021. **Adoption:** Headwind leads the measured open-source library extensions (78★, 27.8k APK, a 25-comment request for weather along the route). WindField on Connect IQ at least 100k. **Reviews:** the5krunner con (no wind field) | No (temperature only) | Headwind, Epic Ride Weather, WindField, myWindsock (all in library) | **Yes** (Weather panel, wind fields, route wind layer) | Yes | [1][43][45][50][61][68][77][79] |
| 6 | **Crash / incident detection** | **Forum:** 9 title posts, 136 votes, 150 comments (56 votes in 2019, 28 in 2021). K3 era: 3 posts, 15 votes. **Other:** Garmin 840 switcher's second request; DCR commenters | No | KSafe (free, library since 2026-05), Saifely (paid), veloVigil | No | Possible, but needs messaging and contacts (poor fit) | [1][9][20][21][22][45][54][59] |
| 7 | **Power Guide / route pacing / real-time stamina / training status** | **Forum:** 9 title posts, 50 votes, plus Firstbeat (10 votes); the Power Guide thread collects several +1s through 2026. **Reviews:** TrainerRoad switcher lists the gap. **Comparison:** cyclistshub table shows Real-Time Stamina and Power Guide as Edge-only | No | Best Bike Split (library 2026-05; some features need Pro), myWindsock, Xert EBC (subscription), 7Climb (climbs only) | Partly (W′ empty-in only) | Yes | [1][7][10][38][45][54][62][64] |
| 8 | **Shimano Di2, and a gear-limit beep (end of cassette)** | **GitHub:** ki2 is the most-starred Karoo repo (392★, about 72.6k APK); ki2 #70 (beep at top/bottom gear) has 22 comments. **Forum:** Di2 41 title posts, 85 votes all-time. AXS gear-limit beep: 2 K3-era posts, one the Garmin switcher's first request. **Reviews:** DCR and the5krunner note the missing Di2 | AXS yes, Di2 no. Gear-limit beep not documented | ki2 (Di2, has the beep), karoo-gearbeeper (AXS, closed source), rutagear | Gears and a cross-chain warning on screen. **No beep** | Yes | [1][22][23][51][59][61][65][67][77] |
| 9 | **Strava Live Segment polish** (in-ride results list, auto-enable along route, average-power field, bigger display) | **Forum:** K3 era 23 title posts, 46 votes (includes bugs). Climb/segment summary: 6 votes. Completed-segments summary: 4 votes, +1s. Auto-enable on route: 5 votes | Yes (Live Segments and fields since 2025-07) | — | Segment panel; the result shows for only 10 s | Yes | [1][12][13][41][52][53] |
| 10 | **Workout display** (clear target range, continuous deviation, full workout graph) | **Review:** DCR calls workouts the weakest area (2024). **GitHub:** Powerbar #33 (workout target band) has 25 comments; Barberfish #27. **Forum:** workout preview graph as a field (3 votes) | **Improved:** primary/secondary targets, Next Primary Target (2026-04), workout graph fields, compliance score (2026-05) | Powerbar (target band), Workout+, Barberfish | **Yes** (PUSH/EASE, step average, time in target, alerts) | Mostly. Future steps can't be read | [32][47][50][53][54][59][69][72] |
| 11 | **POIs: list by distance, along-route resupply** | **Forum:** K3 era 33 title posts, 83 votes (POI list 13 votes; in-ride POI list improvements 11) | Partly (POI drawer) | RouteGraph, Resupply, Shelto, Waybook, Waypoints, CupRoute | No | Yes | [1][25][26][45][65][71] |
| 12 | **Music / media control** | **Forum:** K3 era 13 posts, 63 votes (30 votes, 47 comments). **Other:** TrainerRoad switcher | **Yes** since 1.585.2249 (2025-10-02) | Spintunes, WaterMelonControl | No (not needed) | — | [1][3][53][62] |
| 13 | **Zone-colored fields, speed vs average** | **Forum:** 8 title posts, 37 votes; graphical speed field: 14 comments. **2026 wishlist:** above/below-average dots | Zone colors yes (2025-03). No speed vs average | karoo-colorspeed, Barberfish, BigNum, KStack, Powerbar | Zone colors yes. Speed vs average no | Yes | [1][11][25][50][65] |
| 14 | **W′ balance** | **Forum:** 4 posts, 10 votes (niche). **GitHub:** 4 W′ extensions, 9–16★ each. **Connect IQ:** Dr Skiba W′ Bal only at least 1k | No | WPrimeKarooExtension, karoo-wprimebalance, 7Climb, W′ Race | **Yes** | Yes | [1][33][34][65][75][77][83] |
| 15 | **Group track / better live tracking** | **Forum:** 53 title posts, 168 votes all-time; K3 era 18 posts, 34 votes. Group Track: 5 votes | Live tracking (30 s), auto-share | "Group Ride for Karoo" (unverified, forum comment) | No | Needs a server (poor fit) | [1][30][53] |
| 16 | **Descent view** | **Forum:** 1 post, 3 votes, 5 comments. **GitHub:** Barberfish issues about descent colors | No | RouteGraph (blue descent hues) | No (climbs only) | Yes | [14][72] |
| 17 | **Virtual partner / ghost** | **Forum:** 3 votes in 2026, called a "years old request" in the thread | No | KGhost (library 2026-10), Vin HkE | No | Yes | [15][45][57] |

---

## 3. Per-feature notes (top 12)

### 1. Fueling
- **What riders ask:** an audible eat/drink nudge was asked for repeatedly from 2021 [18][19][42]. Riders called its absence surprising on a premium unit [17] and asked for a basic time or calorie alarm [16].
- **What exists:** Custom Alerts now cover time, distance and calorie alerts, plus power/HR/cadence range alerts, with snooze [44][56].
- **What's still asked for:**
  - A reminder every X kJ, which ties eating to effort, not the clock [70 #19].
  - A one-tap combined carb and drink log [74].
  - Route-based fuel plans; CrankBite only landed in the library on 2026-10-15 [55].
- **Gap:** no free on-device carb balance (burn versus intake) next to the rider's main data. nomride and KSafe do this as separate fields [65][74].

### 2. Layout density and map-page fields
- **What riders ask:**
  - More than two fields per row on the map page, without fields eating map space [27].
  - Half-height fields [28].
  - Map fields that don't vanish at turn prompts [29].
  - Control over the size and position of map-page fields [11].
- **Switcher view:** the TrainerRoad switcher calls Karoo's display options limited [62].
- **GitHub signal:** users want one compact HUD that combines KDouble, Headwind and W′ values [72 #30][73]. There are also asks for different HUDs per page [72 #35].
- **Gap:** extensions can't read other extensions' data [66]. A KarooPRO strip can only combine values KarooPRO computes itself, which is its edge.

### 3. Climb tools
- **What Hammerhead fixed:** Wahoo-style grade colors were the biggest Climber ask (87 comments) and were fixed in May 2026 [4][53].
- **What's still open:**
  - A field for estimated time to finish the current climb; the poster notes Wahoo has one [6].
  - Zooming the climb profile to the part still ahead [5].
  - Picking the key fields shown during climbs [30].
  - Climb category, NP and VAM in climb results [24].
  - A preview of upcoming elevation even with no route loaded [11].
- **KarooPRO:** already covers time to top, W′ empty-in and a grade-colored profile. Missing are a post-climb results list and the no-route upcoming-elevation preview.

### 4. Radar and lights
- **Top K3-era radar ask:** turn the radar light on only when a car approaches, to save battery. The poster says Wahoo has this natively and Garmin via Connect IQ [8].
- **Related asks:** turn lights off when the ride is paused [37], an "all clear" tone [40], and MyBikeTraffic-style vehicle counts [39].
- **Native today:** lights turn on and off only at ride start and end [49]. The radar has tones and a sidebar [48].
- **Feasibility:** karoo-ext has no light-control API (issue #41 was closed without one) [66]. KarooFireFly drives lights through an undocumented internal API [76]. Per-target radar speed is also not exposed yet (open issue #82) [66].

### 5. Wind and weather
- **Forum signal is low.** Hammerhead declined live wind in 2021 [43].
- **Adoption signal is high:**
  - Headwind: 78★ and the most APK downloads of the open-source library extensions measured [77].
  - Riders asked it for headwind along the route; v1.5 added route forecasts [68].
  - WindField has at least 100k Connect IQ downloads [79].
  - Four weather extensions now sit in the library [45].
- **KarooPRO:** covers this already (panel, fields, route wind layer). It's a strength, not a gap.

### 6. Crash / incident detection
- **History:** asked for since 2019 (56 votes) [20][21]. A 2026 post asks for it built in, with no extension or subscription needed [9].
- **Switchers:** a Garmin switcher lists it second [22].
- **Extensions:** KSafe (free) entered the library in May 2026 [54]; Saifely is paid [9][45].
- **Source conflict:** cyclistshub's table marks incident detection as available on Karoo 3 [64]. That conflicts with Hammerhead's docs and forum, so treat it as an error.
- **Not a KarooPRO fit:** it needs contacts, messaging and a large false-alarm design effort.

### 7. Power Guide, pacing and real-time stamina
- **What riders ask:** a power plan for a specific route, split by sections from the profile and the rider's FTP [10]. Replies through 2026 echo it, for example: "It's the only feature that I miss leaving my Edge 1040." (commenter, 2026-04-12) [10]
- **Other signals:**
  - One reply wants distance-based workouts as the mechanism [10].
  - Another misses the Edge 1050's estimate of how long the current effort can last [10].
  - The TrainerRoad switcher lists Power Guide, stamina and pacing as missing [62].
  - cyclistshub shows both as Edge-only [64].
  - A 2024 post asks for Firstbeat-style performance and recovery metrics. Hammerhead replied that it is on the backlog with no timeline [7].
- **What exists:** Best Bike Split (some features need Pro) and myWindsock are in the library [45][54]. Neither is free on-device pacing.
- **Related GitHub asks:** a time ahead/behind field against a planned time-distance profile [71 #251], and a grade-aware ETA [72 #26]. Riders also complain that the native ETA is naive [35].

### 8. Di2 and the gear-limit beep
- **The ask:** a beep when you hit the largest or smallest cog. Garmin does this, and it's useful when you can't look down, for example on a fast descent [67 #70]. Two AXS riders (one coming from an Edge 840) ask for the same [22][23].
- **What exists:** the native audio alert list has no gear event [51]. ki2 has the beep for Di2; karoo-gearbeeper is a closed-source AXS-only extension [65].
- **Also asked:** a gear-ratio display [67 #64].
- **KarooPRO:** already computes cross-chaining, so it's one beep pattern away.

### 9. Strava Live Segments
- **The ask:** riders say the result screen disappears before they're able to read it after a hard effort [12][13]. They want a list of completed segments like Climber's completed-climbs list [13].
- **Also asked:** auto-enable segments along a planned route [41], and an average-power field [1].
- **KarooPRO:** shows the result for 10 s. Keeping a results list is cheap.

### 10. Workouts
- **2024 baseline:** DCR's main software criticism was that the workout screen didn't show the target range [59].
- **Since then, Hammerhead added:** primary/secondary target fields, a Next Primary Target field (2026-04-30), workout graphs and a compliance score [50][53][54].
- **Still asked for:**
  - A target band inside a bar (Powerbar #33, 25 comments) [69].
  - Continuous over/under coloring, not just in/over/under [72 #27].
  - A readable static workout graph as a field [32].
- **Limit:** extensions still can't read upcoming steps. The Next Primary Target type isn't in karoo-ext 1.1.9 [66].

### 11. POIs and resupply
- **The ask:** a POI list with distance from here, an ETA, and filtering, with Garmin's POI list as the reference [25][26].
- **What exists:** well covered by extensions (RouteGraph, Resupply, Shelto, Waybook) [45][65][71].
- **Remaining gap:** extension POIs don't get native approach alerts (open karoo-ext #77) [66].
- **Not a KarooPRO priority.**

### 12. Descents
- **The ask:** a Climber-like view for descents (grade, distance) [14]. A reply wants the elevation profile on the map page the way Wahoo does it [14].
- **What exists:** RouteGraph colors descents [14]. Barberfish users ask for more visible descent colors [72].
- **KarooPRO:** has the route elevation data already. Demand is small, but so is the cost.

---

## 4. Garmin-switcher gap list

| Garmin feature or Connect IQ app | What switchers say | Karoo today | Extension option | Sources |
|---|---|---|---|---|
| **Power Guide** | The one thing missed after leaving an Edge 1040; others +1 | Not native | Best Bike Split (Pro for some features), myWindsock | [10][45][54][62][64] |
| **Real-time stamina** | Edge 1050 estimate of how long the current effort lasts is missed | Not native | Xert EBC (subscription) | [10][45][62][64] |
| **Firstbeat training status and recovery** | Missed by a Garmin tester | Not native; Hammerhead says it is on the backlog | — | [7][59] |
| **Incident detection** | Second request of an Edge 840 switcher | Not native | KSafe, Saifely | [9][22][45] |
| **Di2 gear-limit beep** | Garmin beeps at the end of the cassette | Not native (not in the audio alert list) | ki2 (Di2), karoo-gearbeeper (AXS) | [22][23][51][67] |
| **ClimbPro** | Edge users ask for a time-to-top field (also on Wahoo), climb VAM/NP, adjustable climb parameters | Climber native since 2021 | 7Climb, Climber+ | [6][24][46][61][78] |
| **Varia radar auto light** (Connect IQ on Garmin, native on Wahoo) | Light on only when a car approaches | Lights only on/off at ride start/stop | KarooFireFly (undocumented API) | [8][49][76] |
| **My Bike Radar Traffic** (Connect IQ, at least 100k) | Vehicle counts, mybiketraffic.com upload | Not native | RadarCount | [39][65][80] |
| **WindField** (Connect IQ, at least 100k) | Live wind on the data screen | Not native | Headwind, WindField, Epic Ride Weather, myWindsock | [45][79] |
| **Eat/drink alerts** (Connect IQ Eat! Drink! Reminder!, at least 100k) | Cheap units have them | Custom Alerts since 2025-12 | karoo-reminder, nomride, KSafe, CrankBite | [17][44][81] |
| **All In One / dense fields** (Connect IQ, at least 1M) | A post claims the highest-installed Garmin extension adds three fields per row (unverified) | Fixed layouts | KDouble, Barberfish, KStack | [27][82] |
| **Speed versus average in one field** | Garmin field missed; asked to color speed above/below average | Not native | karoo-colorspeed | [25] |
| **POI list by distance** | Garmin's POI list is the benchmark | POI drawer (distances are additive, not from here) | Waypoints, Resupply, RouteGraph | [25][26] |
| **GroupTrack** | Group asks for an equivalent | Not native | "Group Ride for Karoo" (unverified) | [30] |
| **Virtual partner** | Old request | Not native | KGhost | [15] |
| **Music control, bell, Trailforks-style trails** | Commonly missed in 2024 | Now native: media player 2025-10, bell 2025-08, Trail Maps 2026-07 | — | [3][53][59][62] |

---

## 5. Ideas KarooPRO could own

Feasibility is judged against karoo-ext 1.1.9 [66]. Extensions can:
- stream power, HR, grade, climbs, segments, radar, shifting, workout step and targets, and navigation route polyline and elevation;
- draw fields and map layers;
- beep, show in-ride alerts, use HTTP, write FIT developer fields, and use bonus actions.

They can't read upcoming workout steps, control lights, or read other extensions' data.

1. **Route pacing: "Plan" panel and pacing on the Climb panel**
   - **What it does:**
     - Takes the navigated route's elevation, FTP, CP/W′ and the Open-Meteo wind KarooPRO already fetches.
     - Gives a target power for the next section and a predicted finish time.
     - Shows time ahead or behind the plan.
     - Adds a grade- and wind-aware ETA.
   - **Evidence:** Power Guide gap [10][62][64]; pacer option asked since 2021 [1]; time ahead/behind profile [71 #251]; native ETA complaints [35]; Best Bike Split is the only library option and partly paid [54].
   - **Feasibility:** high. The route polyline and elevation are available to extensions [66]. Everything else is on-device math, which KarooPRO largely already does for climbs.

2. **Stamina gauge (a free real-time-stamina equivalent)**
   - **What it does:**
     - Combines the existing W′ balance (short term) with a long-term energy budget from kJ and time at intensity.
     - Shows a stamina percentage and time or distance left at the current effort.
     - Can be a big-number option and a tile.
   - **Evidence:** [7][10][62][64]. Today only Xert EBC offers this, by subscription [45].
   - **Feasibility:** high for the math. Label the long-term part an estimate.

3. **Fuel panel**
   - **What it does:**
     - Estimates carbs burned from power and %FTP (falls back to HR without a power meter).
     - Logs intake by tap or bonus action: gel, bar and bottle presets.
     - Beeps "eat now" by deficit or every X kJ.
     - Writes FIT developer fields.
   - **Evidence:** historic top demand [18][19][42]; Karoo 3 era [16][17]; kJ-trigger request [70 #19]; Connect IQ Eat! Drink! at least 100k [81]. Native Custom Alerts are only time, distance and calorie timers [44].
   - **Feasibility:** high. It uses power streams, bonus actions, PlayBeepPattern, InRideAlert and FIT developer fields.
   - **Risk:** a crowded space (nomride, KSafe, CrankBite). KarooPRO's edge is showing it next to W′ and the workout.

4. **"PRO strip": a slim one-row field for map pages**
   - **What it does:**
     - Shows 3–6 chosen values, including KarooPRO-only ones: W′, stamina, headwind, time to top, segment gap, workout target delta.
     - Swaps context by itself: climb values on climbs, segment gap on segments.
   - **Evidence:** [27][28][29][36][11][62][72 #30 #35][73][82].
   - **Feasibility:** high. It's a graphical field drawn at the slot size, refreshed once per second.

5. **Gear-limit beep and gear ratio**
   - **What it does:**
     - Distinct beeps at the largest and smallest cog, and at the effective end when the next shift would cross-chain.
     - Gear ratio or gear inches on the Bike panel.
   - **Evidence:** ki2 #70 (22 comments), #64 [67]; AXS riders [22][23].
   - **Feasibility:** very high. The shifting data types and PlayBeepPattern exist, and Drivetrain.kt already computes cross-chaining.

6. **Results list for climbs and segments**
   - **What it does:** keeps every completed segment (time, gap to PR/KOM) and climb (time, VAM, average W, W/kg, NP, W′ low point) for the ride, on a "Results" panel or the existing Segment/Climb panels.
   - **Evidence:** [12][13][24]. Native Climber lists completed climbs but without power stats [55].
   - **Feasibility:** very high (segment and climb streams). It's mostly UI.

7. **Descent view**
   - **What it does:** mirrors the Climb panel for descents on a navigated route: distance and height to the bottom, steepest grade ahead, a blue-hued profile, and an optional descent band on the map.
   - **Evidence:** [14][72]; modest demand.
   - **Feasibility:** high. It reuses the climb code paths on the route elevation polyline.

8. **Workout polish**
   - **What it does:**
     - Continuous deviation color (red far below, neutral at target, green above).
     - A target-band bar on the Workout panel.
     - A static workout-structure strip when the coach relay knows the plan.
   - **Evidence:** [32][59][69 #33][72 #27].
   - **Feasibility:** high for the band and colors. The structure strip only works with the relay, because karoo-ext can't read future steps [66].

**Not recommended for KarooPRO**
- **Crash detection:** KSafe is free and in the library; it needs messaging [45][54].
- **Light control:** there's no karoo-ext API [66][76].
- **POI search:** many extensions already do it [45][65].
- **Music:** native since 2025-10 [53].
- **Group tracking:** needs a server [30].
- **Ghost:** KGhost is in the library [57].

---

## 6. Sources

Forum posts are on support.hammerhead.io. Dates are post dates; votes and comments were counted on 2026-10-24.

**Hammerhead community forum**
1. Hammerhead community forum, full post export via the public Zendesk API. https://support.hammerhead.io/api/v2/help_center/community/posts.json, 4,361 posts, retrieved 2026-10-24. Supports all forum counts, votes and theme groupings.
2. "Enable SDK for Karoo (3)". https://support.hammerhead.io/hc/en-us/community/posts/25940221488539, 2024-05-21 (111 votes, 232 comments). Supports the top-voted K3 ask (now met by karoo-ext).
3. "Music Control". https://support.hammerhead.io/hc/en-us/community/posts/26055479589019, 2024-05-26 (30 votes, 47 comments). Supports music demand and the 2025-10 native delivery.
4. "climber grade colors". https://support.hammerhead.io/hc/en-us/community/posts/33671393075227, 2025-02-05 (23 votes, 87 comments). Supports the grade-color demand and the fix in 1.634.2440.
5. "Feature request. Climber behaviour". https://support.hammerhead.io/hc/en-us/community/posts/40514597839771, 2025-08-29 (10 votes). Supports climb-profile zoom and remaining-only view.
6. "Feature Request: Data field for estimated time for current climb". https://support.hammerhead.io/hc/en-us/community/posts/38230111875867, 2025-06-22 (10 votes). Supports the time-to-top field (Wahoo has it).
7. "Garmin: Firstbeat-Integration / Hammerhead: ?". https://support.hammerhead.io/hc/en-us/community/posts/26876315406235, 2024-06-25 (10 votes). Supports the performance/recovery metrics gap and Hammerhead's backlog reply.
8. "Feature Request: Radar (Varia) Auto mode (lights on only when a car is approaching)". https://support.hammerhead.io/hc/en-us/community/posts/43392639083419, 2025-11-17 (13 votes, 18 comments). Supports radar-triggered lights.
9. "Built-in crash detection". https://support.hammerhead.io/hc/en-us/community/posts/48570052312091, 2026-04-01 (9 votes). Supports crash-detection demand and the KSafe/Saifely mentions.
10. "Is there an extension for Karoo that works similarly to Garmin Power Guide?". https://support.hammerhead.io/hc/en-us/community/posts/40454699760539, 2025-08-27 (replies to 2026-06). Supports the Power Guide and stamina gap; source of the one direct quote.
11. "Wishes for 2026". https://support.hammerhead.io/hc/en-us/community/posts/46433192036251, 2026-02-09 (13 comments). Supports map-field layout, colored fields, predictive elevation, and zoomable pages.
12. "Feature request- In ride climb and segment summary". https://support.hammerhead.io/hc/en-us/community/posts/41348892681243, 2025-09-23 (6 votes). Supports the results list.
13. "Feature Request: Summary of Completed Strava Segments". https://support.hammerhead.io/hc/en-us/community/posts/40801030178587, 2025-09-06 (4 votes). Supports the segment results list.
14. "Feature Request: Downhill 'Descender' Mode for Karoo 3". https://support.hammerhead.io/hc/en-us/community/posts/41308032727963, 2025-09-22 (3 votes, 5 comments). Supports the descent view.
15. "Feature Request: Virtual Race Partner". https://support.hammerhead.io/hc/en-us/community/posts/54181194360347, 2026-08-09. Supports ghost demand and KGhost.
16. "Hydration and nutrition on Karoo". https://support.hammerhead.io/hc/en-us/community/posts/30568573518363, 2024-11-01 (14 comments). Supports K3-era fueling demand.
17. "Lack of Drink/Food Alerts". https://support.hammerhead.io/hc/en-us/community/posts/30719910895771, 2024-11-07. Supports fueling demand.
18. "Feeding and hydration notices". https://support.hammerhead.io/hc/en-us/community/posts/1260802672689, 2021-04-27 (39 votes). Supports historic fueling demand.
19. "(features request) Audio alert for eating and drinking". https://support.hammerhead.io/hc/en-us/community/posts/1260801125789, 2021-02-04 (38 votes). Supports historic fueling demand.
20. "Feature Request: Crash sensor support". https://support.hammerhead.io/hc/en-us/community/posts/360034811314, 2019-09-07 (56 votes). Supports crash detection.
21. "Feature Request: Crash Detection". https://support.hammerhead.io/hc/en-us/community/posts/4406519829147, 2021-09-09 (28 votes). Supports crash detection.
22. "Garmin Edge 840 to new Karoo observations + feature requests". https://support.hammerhead.io/hc/en-us/community/posts/41717250764059, 2025-10-03. Supports the AXS end-of-gear beep and crash detection as a switcher's top asks.
23. "HH alert for top / bottom gear reached on AXS groupset". https://support.hammerhead.io/hc/en-us/community/posts/48723441595931, 2026-04-05 (4 votes). Supports the gear-limit beep.
24. "Feature Request | Climb Data". https://support.hammerhead.io/hc/en-us/community/posts/42430999078299, 2025-10-22. Supports climb category, NP and VAM in results (Garmin 1040 switcher).
25. "Feature request: Graphical speed field". https://support.hammerhead.io/hc/en-us/community/posts/40399871052571, 2025-08-26 (14 comments). Supports speed versus average (Garmin switcher).
26. "In-Ride list of POIs: Suggestions for improvement". https://support.hammerhead.io/hc/en-us/community/posts/33333335887899, 2025-01-29 (11 votes). Supports the POI list with Garmin as benchmark.
27. "Three data fields per row". https://support.hammerhead.io/hc/en-us/community/posts/34742975105435, 2025-03-08 (8 votes). Supports layout density; the Garmin install-count claim is unverified.
28. "Narrower data fields". https://support.hammerhead.io/hc/en-us/community/posts/33573673183003, 2025-02-03 (11 votes). Supports layout density.
29. "Data fields disappear before a turn." https://support.hammerhead.io/hc/en-us/community/posts/42279184046875, 2025-10-18 (14 votes, 23 comments). Supports map-page field pain.
30. "Group Track extension?". https://support.hammerhead.io/hc/en-us/community/posts/38291756113691, 2025-06-24 (5 votes). Supports group tracking; the "Group Ride for Karoo" claim in comments is unverified.
31. "Custom datafields when climbing". https://support.hammerhead.io/hc/en-us/community/posts/27667833703195, 2024-07-24 (6 votes). Supports custom climb fields.
32. "Feature Request: Workout preview graph as a data field during activity". https://support.hammerhead.io/hc/en-us/community/posts/45817128806171, 2026-01-23. Supports a workout structure view.
33. "Feature request: Historic graphs and work+w'". https://support.hammerhead.io/hc/en-us/community/posts/54360544259483, 2026-08-13. Supports W′ and power-history graphs.
34. "W Prime (W') data field - can it be added?". https://support.hammerhead.io/hc/en-us/community/posts/37890349571995, 2025-06-11 (8 comments). Supports W′ demand.
35. "Calculation of the ETA". https://support.hammerhead.io/hc/en-us/community/posts/35960528781467, 2025-04-14 (replies to 2026-07). Supports naive-ETA complaints.
36. "Use the Space! (How it is vs how it could be)". https://support.hammerhead.io/hc/en-us/community/posts/1260803144990, 2021-05-23 (66 votes). Supports historic layout demand.
37. "Feature Request - Turn off Varia/Ant+ light when manually pausing ride". https://support.hammerhead.io/hc/en-us/community/posts/40273444022555, 2025-08-22 (9 votes). Supports light behavior.
38. "Best Bike Split". https://support.hammerhead.io/hc/en-us/community/posts/26967966418715, 2024-06-28 (5 votes). Supports pacing demand.
39. "Feature Request: MyBikeTraffic Integration". https://support.hammerhead.io/hc/en-us/community/posts/9398520826907, 2022-10-06 (16 votes). Supports radar traffic stats.
40. "Varia Radar 'All clear' tone". https://support.hammerhead.io/hc/en-us/community/posts/4412688637851, 2021-12-12 (18 votes). Supports radar audio asks.
41. "Automatically enable Strava Live Segments along a planned route". https://support.hammerhead.io/hc/en-us/community/posts/54259557329435, 2026-08-11 (5 votes). Supports segment polish.
42. "Customizable Reminders (for nutrition, hydration, or other)". https://support.hammerhead.io/hc/en-us/community/posts/4899782746395, 2022-03-23 (14 votes, status "not planned"). Supports historic fueling demand.
43. "Feature request: live wind/weather data". https://support.hammerhead.io/hc/en-us/community/posts/1260800681869, 2021-01-20 (12 votes, "not planned"). Supports wind demand and Hammerhead declining it.

**Hammerhead official docs and release notes**

44. "Karoo OS - Custom Alerts". https://support.hammerhead.io/hc/en-us/articles/44288796614299, created 2025-12-11, updated 2026-10-16. Supports native alert scope (time, distance, calories; power/HR/cadence ranges).
45. "Native Hammerhead Extensions". https://support.hammerhead.io/hc/en-us/articles/51862101013019, created 2026-06-18, updated 2026-10-20. Supports the library list: 7Climb, BBS, CrankBite, Epic Ride Weather, Headwind, KGhost, KSafe, myWindsock, Saifely, Shelto, WindField, Xert EBC and others.
46. "Karoo OS - Climber". https://support.hammerhead.io/hc/en-us/articles/25602163627163, updated 2026-10-20. Supports native Climber behavior.
47. "Karoo OS - Workouts". https://support.hammerhead.io/hc/en-us/articles/18001019006363, updated 2026-10-20. Supports the native workout drawer and fields.
48. "Karoo OS - Rearview Radar Integration". https://support.hammerhead.io/hc/en-us/articles/25605408318107, updated 2026-10-04. Supports native radar features and tones.
49. "Karoo OS - Smart Light Integration". https://support.hammerhead.io/hc/en-us/articles/25606785239195, updated 2026-08-19. Supports lights on/off only at ride start/end.
50. "Data Fields - Legend". https://support.hammerhead.io/hc/en-us/articles/35533240795419, updated 2026-09-24. Supports the full native field list: no W′, no wind, no time-to-top; Next Primary Target present.
51. "Karoo OS - Audio Alerts". https://support.hammerhead.io/hc/en-us/articles/25539022018843, updated 2026-10-18. Supports the absence of a gear-limit audio event.
52. "Karoo OS - Strava Live Segments". https://support.hammerhead.io/hc/en-us/articles/25601968015515, updated 2026-07-10. Supports native segment features (200-segment limit).
53. Software Release Notes index (pages 1–3). https://shop.sram.com/blogs/change-logs/ (also `?page=2`, `?page=3`), entries 2025-01-23 to 2026-10-15. Supports the dates of the extension library (2025-03-06), Climber fields (2025-03-27, 2025-05-29), Live Segment fields (2025-07-24), bell (2025-08-21), media player (2025-10-02), custom alerts (2025-12-11), Karoo Share (2026-03-19), Next Primary Target (2026-04-30), Climber colors (2026-05-27), Trail Maps (2026-07-09) and more.
54. "Karoo Software Build Version: 1.632.2430". https://shop.sram.com/blogs/change-logs/karoo-software-build-version-1-632-2430, 2026-05-14. Supports BBS, KSafe and kxgear added to the library; workout compliance and post-ride workout graph.
55. "Karoo Software Build Version: 1.663.2548". https://shop.sram.com/blogs/change-logs/karoo-software-build-version-1-663-2548, 2026-10-15. Supports CrankBite, Shelto and GluKaMon added; completed climbs in the Climber drawer.
56. "Karoo Software Build Version: 1.600.2303". https://shop.sram.com/blogs/change-logs/karoo-software-build-version-1-600-2303, 2025-12-11. Supports the Custom Alerts launch.
57. "Karoo Software Build Version: 1.658.2535". https://shop.sram.com/blogs/change-logs/karoo-software-build-version-1-658-2535, 2026-10-01. Supports KGhost added; radar disconnect-alert fix.
58. "Karoo OS - Extensions Library". https://support.hammerhead.io/hc/en-us/articles/34676015530907, created 2025-03-06. Supports how the library works.

**Reviews and other forums**

59. DC Rainmaker, "Hammerhead Karoo 3 In-Depth Review". https://www.dcrainmaker.com/2024/05/hammerhead-new-karoo-3-review-upgrade.html, 2024-05-15; comments read through 2026-09. Supports workouts as the weakest area, missing Di2, no training status, and commenter asks (Trailforks, crash detection, map fields, HR alerts).
60. DC Rainmaker, "Hammerhead Karoo Adds App Store: A Few Thoughts". https://www.dcrainmaker.com/2025/03/hammerhead-karoo-adds-app-store-a-few-thoughts.html, 2025-03-10. Supports the early extension store and wind/weather extensions as the ones that matter.
61. the5krunner, "Karoo 3 Review – 14 cons, 14 pros". https://the5krunner.com/2024/05/15/hammerhead-karoo-3-review/, 2024-05-15 (updated 2026-09-30). Supports the cons: no drink/fuel alerts, no wind field, Climber not tunable, Di2 only via ki2.
62. TrainerRoad forum, "A Wahoo user compares: Karoo 3 vs Edge 1050 (long)". https://www.trainerroad.com/forum/t/a-wahoo-user-compares-karoo-3-vs-edge-1050-long/99289, 2025-01-01. Supports the switcher gaps: limited layouts, no native music or eat/drink alerts or bell at the time, Power Guide/stamina/pacing missing.
63. road.cc, "Hammerhead Karoo (3) review". https://road.cc/content/review/hammerhead-karoo-3-308361, 2024-05-15. Supports a positive review with few software criticisms; commenters note missing Di2.
64. Cyclists Hub, "Garmin Edge 1050 vs. Hammerhead Karoo 3". https://www.cyclistshub.com/garmin-edge-1050-vs-hammerhead-karoo-3/, updated 2025-02-07. Supports Real-Time Stamina and Power Guide as Edge-only. Its "crash detection: yes" for Karoo conflicts with Hammerhead's own sources and is treated as an error.

**GitHub**

65. timklge/awesome-karoo README. https://github.com/timklge/awesome-karoo, retrieved 2026-10-24. Supports the extension landscape (nomride, KSafe, RadarCount, karoo-gearbeeper, Resupply, Barberfish, KStack, W′ extensions and others).
66. hammerheadnav/karoo-ext: issues and releases. https://github.com/hammerheadnav/karoo-ext/issues, latest release 1.1.9 (2026-03-17). Supports platform limits:
    - #41 lights control (closed, none added);
    - #56 route weather;
    - #74 native controls;
    - #76 mute state;
    - #77 extension POIs and alerts;
    - #82 radar per-target speed;
    - #83 radar packet rate;
    - DataType.kt has no next-step target.
67. valterc/ki2 issues #70 (gear-limit beep, 2022-12-21, 22 comments), #64 (gear ratio, 12 comments), #13 (shifting to FIT). https://github.com/valterc/ki2/issues/70. Supports Di2 and gear-beep demand.
68. timklge/karoo-headwind README and issues #16 (2024-12-17, 25 comments), #72, #215. https://github.com/timklge/karoo-headwind. Supports wind/weather demand and coverage.
69. timklge/karoo-powerbar issues #33 (workout target band, 2025-05-26, 25 comments), #26, #32. https://github.com/timklge/karoo-powerbar/issues/33. Supports the workout-band and bar demand.
70. timklge/karoo-reminder issues #21 (2025-01-28, mentions a Reddit post), #10, #19 (kJ trigger, 2025-01-21), #33. https://github.com/timklge/karoo-reminder/issues. Supports reminder and fueling demand.
71. timklge/karoo-routegraph issues #144 (POIs along route, 2025-12-19), #251 (time ahead/behind profile, 2026-08-30), #86. https://github.com/timklge/karoo-routegraph/issues. Supports POI and pacing-profile demand.
72. jpweytjens/Barberfish issues #30 (other extensions in the HUD, 2026-10-04), #35 (multiple HUDs), #27 (workout deviation colors), #26 (grade-aware ETA), plus descent-color issues. https://github.com/jpweytjens/Barberfish/issues. Supports HUD, workout and ETA demand.
73. apopovsky/WPrimeKarooExtension issue #53 (share data with other extensions, 2026-10-07). https://github.com/apopovsky/WPrimeKarooExtension/issues/53. Supports the consolidation/HUD demand.
74. lockevod/Karoo-KSafe issue #16 (fueling roadmap, 2026-05-31). https://github.com/lockevod/Karoo-KSafe/issues/16. Supports fueling features in KSafe.
75. yrkan/7climb README. https://github.com/yrkan/7climb, retrieved 2026-10-24. Supports 7Climb coverage (W′, climb pacing, summit ETA).
76. derstrassi/karoofirefly README. https://github.com/derstrassi/karoofirefly, retrieved 2026-10-24. Supports radar-warning light flash via an undocumented internal API.
77. GitHub stars and release-asset download counts via `gh api`, retrieved 2026-10-24. Supports adoption numbers. APK downloads:
    - Headwind 27,785; ki2 72,624; Powerbar 12,691; Reminder 12,015; KDouble 11,395; RouteGraph 6,696.
    - Stars: ki2 392, Headwind 78.

**Connect IQ store** (store API, retrieved 2026-10-24; downloadCount appears bucketed)

78. DC Rainmaker, "Hammerhead Karoo's New CLIMBER Feature: Here's how it works". https://www.dcrainmaker.com/2021/06/hammerhead-karoos-new-climber-feature-heres-how-it-works.html, 2021-06 (date from the URL; seen in search results, not fetched, so unverified). Supports Climber shipping in 2021 as Karoo's ClimbPro equivalent.
79. WindField: wind and radar. https://apps.garmin.com/en-US/apps/a924715a-2d53-42a1-a8f9-e365bc849840. At least 100,000 downloads, 686 reviews, 4.7★.
80. My Bike Radar Traffic. https://apps.garmin.com/en-US/apps/c5d949c3-9acb-4e00-bb2d-c3b871e9e733. At least 100,000 downloads, 235 reviews.
81. Eat! Drink! Reminder!. https://apps.garmin.com/en-US/apps/7449127c-65ba-470b-954c-3a5fa93376c3. At least 100,000 downloads, 41 reviews.
82. All In One (data field). https://apps.garmin.com/en-US/apps/6dec42e9-7fee-402b-95a4-a111e83d45e3. At least 1,000,000 downloads, 696 reviews; multi-sport, so not cycling-only.
83. Dr Skiba W′ Bal. https://apps.garmin.com/en-US/apps/d1561ff8-0938-4b6b-8c77-1c4ea6215dd7. At least 1,000 downloads, 30 reviews (a niche signal).

**KarooPRO**

84. KarooPRO README (v0.2.3), this repository's `README.md`, read 2026-10-24. Supports KarooPRO's current coverage.
