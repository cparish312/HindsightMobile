# HindsightMobile
* Take a screenshot on your android every 2 seconds.
* Run OCR
* Use Objectbox for RAG
* Pass to LLM running locally using llamacpp

# Installation
1. `git clone --recursive https://github.com/cparish312/HindsightMobile.git`
2. Open the Project in Android Studio
3. Connect your Android Device
4. You need to do a release build for the LLM to run quickly:
   * Go View -> Tool Windows -> Build Variants and then click the drop down for release
5. Run the application using `Run` > `Run 'app'` or the play button in Android Studio
    * If getting incompatible AGP version install the newest version of Android Studio

# Communication
<a href="https://discord.gg/CmWWW94E">
    <img src="https://img.shields.io/discord/1285689349442109451?color=5865F2&logo=discord&logoColor=white&style=flat-square" alt="Join us on Discord">
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
* `Chat`: go to chat
* `Screen Recording`: Start Screen recording Background Process (May have to click stop on Notification to stop)
* `Auto Ingest`: Auto Run Ingestion at
* `Hour to Auto Ingest (Military Time)`
    * Default is 2 am
* IMPORTANT PLEASE READ THIS `Record New Apps By Default`: when you enter an app that has not been
    recorded yet it will automatically start recording

# Bonus
* If you click on the Assistant's response you can see the exact prompt that went into the LLM

# Shoutouts
* [LMPlayground](https://github.com/andriydruk/LMPlayground/tree/main)
* [Android-Document-QA](https://github.com/shubham0204/Android-Document-QA/tree/main)