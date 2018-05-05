# Audio Switcher
This is a simple Android application that serves one goal, make switching between Google Play Music and Podcast Addict easier by just using the headset button.

In my everyday commute, I need to be able to rapidly switch between listening to music (because its better than silence) and listening to podcasts (because I can't play on a DS and listen to podcasts at the same time).

# Usage
- Import as an Android Studio application.
- After building and installing, go to "Notification Access" under Settings, and give Audio Switcher access to notifications. This is required to be able to `addOnActiveSessionsChangedListener` to `mMediaSessionManager`