# HindsightMobile
1) Takes a screenshot every 2 seconds (only of apps you choose)
2) Reads and embeds the text from each screenshot with OCR
3) Lets you chat with anything you've seen on your phone via a local LLM (my favorite: llama3.2 1B)

# Demos
<a href="https://www.loom.com/share/3537d558aff84cf8950d4348fb76a194">
  <img style="max-width:300px;" src="https://cdn.loom.com/sessions/thumbnails/3537d558aff84cf8950d4348fb76a194-341fff36e746979b-full-play.gif">
</a>
<a href="https://www.loom.com/share/54559342b2b34028b76e92be645942b5">
  <img style="max-width:300px;" src="https://cdn.loom.com/sessions/thumbnails/54559342b2b34028b76e92be645942b5-96c643714f1aa0ac-full-play.gif">
</a>

# Installation
## Become Google Play Tester
1. Join the Discord below and message in the `google-play-testing` channel or DM me directly
2. If you don't have Discord you can email: `connor@hindsight.life`

## Build from Source
1. `git clone --recursive https://github.com/cparish312/HindsightMobile.git`
2. Open the Project in Android Studio
3. Connect your Android Device
4. You need to do a release build for the LLM to run quickly:
   * Go View -> Tool Windows -> Build Variants and then click the drop down for release
5. Run the application using `Run` > `Run 'app'` or the play button in Android Studio
    * If getting incompatible AGP version install the newest version of Android Studio

# Communication
<a href="https://discord.gg/eqGN3wVe">
    <img src="https://img.shields.io/badge/Join%20us%20on-Discord-5865F2?logo=discord&logoColor=white&style=flat-square" alt="Join us on Discord">
</a>

Setup an onboarding session or just chat about the project [here](https://calendly.com/connorparish9)

# Settings
* `Ingest Screenshots`: runs a manual ingestion of screenshots
    * Add to db
    * OCR
    * Embed
* `Manage Recordings`: Takes you to manage recordings screen
  * If checked the app will be record
  * Delete all content (screenshots, videos, embeddings, OCR Results) for a given app
* `Delete Screenshots From the Last:`
  * Let's you delete recent screenshots and OCR results
* `Chat`: go to chat
* `Server Upload`: Setup and run server upload. Server setup can be found at [hindsight](https://github.com/cparish312/hindsight).
* `Screen Recording`: Start Screen recording Background Process (May have to click stop on Notification to stop)
* `Auto Ingest`: 
  * Runs auto ingest everytime your phone screen turns off
* `Auto Ingest When Not Charging`:
  * If off then auto ingestion will only run if your phone is charging
* IMPORTANT PLEASE READ THIS `Record New Apps By Default`: when you enter an app that has not been
    recorded yet it will automatically start recording

# Bonus
* If you click on the Assistant's response you can see the exact prompt that went into the LLM

# Shoutouts
* [LMPlayground](https://github.com/andriydruk/LMPlayground/tree/main)
* [Android-Document-QA](https://github.com/shubham0204/Android-Document-QA/tree/main)