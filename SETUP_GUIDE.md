# GateKeeper Mobile: Complete Setup, Build, and Run Guide

This guide explains how to open, build, install, and run the GateKeeper Mobile Android application.

It is written for users who may not be technical. Follow each step carefully and do not skip the waiting steps, especially when Android Studio is downloading or syncing files.

---

# 1. What This Project Is

GateKeeper Mobile is an Android application that runs on an Android phone.

The app needs to be built using Android Studio. After building, Android Studio will install the app on a connected Android phone.

A real Android phone is recommended because GateKeeper uses VPN-related Android functionality. Some emulators may not fully support this feature.

---

# 2. What You Need Before Starting

Before starting, make sure you have:

1. A laptop or desktop computer
2. Internet connection
3. Android Studio installed
4. The project ZIP file
5. An Android phone
6. A USB cable that supports data transfer

Important: Some USB cables are charging-only cables. If the phone does not appear in Android Studio, try another cable.

---

# 3. Project ZIP File

You should receive a ZIP file named something similar to:

```text
GateKeeper-Mobile-Submission.zip
```

After extracting the ZIP file, the project folder should contain files and folders like these:

```text
app/
gradle/
public/
.gitignore
build.gradle.kts
gradle.properties
gradlew
gradlew.bat
settings.gradle.kts
README.md
DOCUMENTATION.md
features.md
REQUIREMENTS.md
```

The important files are:

```text
app/
build.gradle.kts
settings.gradle.kts
gradlew
gradlew.bat
gradle/
```

Do not open only the `app` folder in Android Studio.

You must open the main project folder, the folder that contains `app`, `build.gradle.kts`, and `settings.gradle.kts`.

---

# 4. Install Android Studio

1. Open your browser.

2. Go to the official Android Studio website:

```text
https://developer.android.com/studio
```

3. Download Android Studio for your operating system.

4. Run the installer.

5. Keep the default installation options selected.

6. Make sure Android Studio installs the following:

```text
Android Studio
Android SDK
Android SDK Platform Tools
Android Build Tools
Android Emulator / Android Virtual Device
```

7. After installation, open Android Studio.

8. On the first launch, Android Studio will show a setup wizard.

9. Choose:

```text
Standard
```

10. Click Next and allow Android Studio to download the required files.

This may take several minutes depending on internet speed.

---

# 5. Extract the Project ZIP File

1. Find the ZIP file:

```text
GateKeeper-Mobile-Submission.zip
```

2. Right-click the ZIP file.

3. Select:

```text
Extract All
```

4. Extract the project to a simple location, for example:

```text
C:\Projects\GateKeeper-Mobile\
```

Avoid very long folder paths such as:

```text
C:\Users\YourName\Downloads\Some Folder\Another Folder\Final Latest New Project Copy\
```

A simple folder path is better and reduces build issues.

---

# 6. Open the Project in Android Studio

1. Open Android Studio.

2. On the welcome screen, click:

```text
Open
```

If Android Studio already has another project open, click:

```text
File > Open
```

3. Select the extracted project folder.

Make sure you select the folder that contains:

```text
app/
build.gradle.kts
settings.gradle.kts
gradlew
```

4. Click:

```text
OK
```

5. Android Studio will open the project.

---

# 7. Wait for Gradle Sync

After the project opens, Android Studio will start something called Gradle Sync.

Gradle Sync means Android Studio is checking the project and downloading the required libraries.

You may see messages like:

```text
Gradle sync started
Downloading dependencies
Indexing files
Building project information
```

Do not press the Run button yet.

Wait until Android Studio finishes syncing.

You may see a message like:

```text
Gradle sync finished
```

or the progress bar at the bottom of Android Studio will disappear.

The first Gradle Sync may take several minutes.

---

# 8. Prepare the Android Phone

A real Android phone is recommended.

The app may run on an emulator, but VPN-related features may not work properly on all emulators.

---

# 9. Enable Developer Options on the Phone

1. On the Android phone, open:

```text
Settings
```

2. Go to:

```text
About phone
```

3. Find:

```text
Build number
```

On some phones, it may be inside:

```text
Settings > About phone > Software information
```

4. Tap `Build number` 7 times quickly.

5. The phone may ask for the phone password, PIN, or pattern.

6. After that, you should see a message like:

```text
You are now a developer!
```

---

# 10. Enable USB Debugging

1. Go back to the main Settings screen.

2. Open:

```text
Developer options
```

On some phones, it may be located here:

```text
Settings > System > Developer options
```

3. Find:

```text
USB debugging
```

4. Turn it ON.

5. Confirm the warning message if shown.

---

# 11. Connect the Phone to the Laptop

1. Connect the Android phone to the laptop using a USB cable.

2. Unlock the phone screen.

3. A message may appear on the phone:

```text
Allow USB debugging?
```

4. Select:

```text
Always allow from this computer
```

5. Tap:

```text
Allow
```

6. In Android Studio, look at the top area near the green Run button.

7. Your phone should appear in the device list.

Example device names:

```text
Samsung
Pixel
OnePlus
Redmi
Xiaomi
Motorola
```

---

# 12. Build and Run the App

1. In Android Studio, make sure the selected run option is:

```text
app
```

2. Select your connected Android phone from the device dropdown.

3. Click the green Run button.

It looks like this:

```text
▶
```

You can also press:

```text
Shift + F10
```

on Windows.

4. Android Studio will now build the project.

The first build can take a few minutes.

5. If the build is successful, Android Studio will automatically install the app on the phone.

6. The GateKeeper Mobile app should open automatically.

---

# 13. First Setup Inside the App

1. When the app opens, go through the onboarding screens.

2. When you activate protection, Android may show a VPN permission message.

The message may say that the app wants to set up a VPN connection.

3. Tap:

```text
OK
```

or:

```text
Allow
```

4. A small key icon may appear in the phone status bar.

This means the VPN protection is active.

5. The app dashboard should show a protected status, such as:

```text
System Protected
```

The app is now running successfully.

---

# 14. Common Problems and Easy Fixes

This section explains common problems that may happen and how to fix them.

---

## Problem 1: Android Studio Takes Too Long to Open the Project

This is normal during the first setup.

Android Studio may need to download Gradle, Android SDK files, and project libraries.

What to do:

```text
Wait patiently.
Keep the internet connected.
Do not close Android Studio during the first sync.
```

If it is stuck for more than 20–30 minutes, close Android Studio and open the project again.

---

## Problem 2: Gradle Sync Failed

This means Android Studio could not prepare the project properly.

Common reasons:

```text
Internet is not working
Required SDK is missing
Gradle download failed
Android Studio was interrupted
```

Try this:

1. Check internet connection.
2. Click:

```text
Try Again
```

3. Or click:

```text
File > Sync Project with Gradle Files
```

4. Restart Android Studio if needed.
5. Open the project again.

---

## Problem 3: Android SDK Not Found

Android Studio may show a message saying the SDK is missing.

This usually happens when Android Studio is newly installed.

What to do:

1. Click the option shown by Android Studio, such as:

```text
Install SDK
```

or:

```text
Download SDK
```

2. Accept the license agreement.
3. Wait for installation to complete.
4. Run Gradle Sync again.

---

## Problem 4: The Phone Does Not Appear in Android Studio

This is common.

Try these fixes:

1. Unlock the phone screen.
2. Disconnect and reconnect the USB cable.
3. Try another USB cable.
4. Use a data-transfer cable, not a charging-only cable.
5. Enable USB debugging again.
6. Check the phone screen for the message:

```text
Allow USB debugging?
```

7. Change USB mode on the phone to:

```text
File Transfer
```

or:

```text
MTP
```

On Windows, you may also need to install the phone manufacturer’s USB driver.

---

## Problem 5: “Allow USB Debugging” Message Does Not Appear

Try this:

1. Disconnect the phone.
2. Turn USB debugging OFF.
3. Turn USB debugging ON again.
4. Reconnect the phone.
5. Unlock the phone screen.
6. Check for the popup again.

If it still does not appear, use another USB cable or another USB port.

---

## Problem 6: The App Builds but Does Not Install

This may happen if:

```text
The phone storage is full
Another version of the app is already installed
The phone blocked installation
The USB connection disconnected
```

Try this:

1. Free some space on the phone.
2. Uninstall the older GateKeeper app from the phone.
3. Reconnect the phone.
4. Run the app again from Android Studio.

---

## Problem 7: Android Studio Shows “Device Unauthorized”

This means the phone has not allowed the computer yet.

Fix:

1. Unlock the phone.
2. Look for the USB debugging permission popup.
3. Tap:

```text
Allow
```

If the popup does not appear:

1. Go to Developer Options.
2. Tap:

```text
Revoke USB debugging authorizations
```

3. Disconnect and reconnect the phone.
4. Allow USB debugging again.

---

## Problem 8: VPN Permission Does Not Appear

This may happen if:

```text
Another VPN app is already active
The phone blocks VPN permission
The emulator does not support it properly
```

Try this:

1. Turn off any other VPN app on the phone.
2. Restart the phone.
3. Open GateKeeper again.
4. Activate protection again.
5. Use a real Android phone instead of an emulator.

---

## Problem 9: App Opens but Protection Does Not Start

Try this:

1. Make sure VPN permission was accepted.
2. Check if another VPN app is running.
3. Turn off battery saver temporarily.
4. Restart the app.
5. Restart the phone.
6. Try activating protection again.

Some phones may restrict background activity. If needed, allow the app to run in the background from phone settings.

---

## Problem 10: Emulator Does Not Work Properly

The Android Emulator can be used for basic testing, but it may not fully support VPN-related functionality.

Recommended solution:

```text
Use a real Android phone.
```

---

## Problem 11: Build Fails After Moving the Project Folder

If the project was moved after opening it in Android Studio, Android Studio may get confused.

Fix:

1. Close Android Studio.
2. Move the project to a simple folder path, for example:

```text
C:\Projects\GateKeeper-Mobile\
```

3. Open Android Studio again.
4. Click:

```text
File > Open
```

5. Select the project folder again.

---

## Problem 12: Internet Download Failed

During the first build, Android Studio downloads many required files.

If the internet disconnects, the build may fail.

Fix:

1. Reconnect to the internet.
2. Click:

```text
Try Again
```

3. Or click:

```text
Sync Project with Gradle Files
```

---

## Problem 13: Windows Blocks Something During Setup

Windows Defender or antivirus may ask for permission.

If Android Studio or Gradle is blocked, allow access.

Only allow access if the project came from a trusted source.

---

## Problem 14: Phone Shows Security Warning

Android may show a warning because the app is being installed directly from Android Studio and not from the Play Store.

This is normal for testing.

Allow the installation if the project source is trusted.

---

## Problem 15: App Does Not Open Automatically

If Android Studio says the build was successful but the app does not open:

1. Unlock the phone.
2. Check the app drawer.
3. Find:

```text
GateKeeper
```

4. Open it manually.

---

# 15. What Not to Change

Do not rename these files or folders:

```text
app/
gradle/
build.gradle.kts
settings.gradle.kts
gradlew
gradlew.bat
```

Do not delete these files or folders.

Do not open only the `app` folder. Always open the full project folder.

---

# 16. What to Do If Something Still Fails

If the project still does not run, send the developer the following information:

```text
1. Screenshot of the error in Android Studio
2. Screenshot of the phone screen if the issue is on the phone
3. Phone model
4. Android version
5. Laptop operating system
6. Android Studio version
7. Whether a real phone or emulator was used
```

This information helps identify the problem quickly.

---

# 17. Success Checklist

The setup is successful when all of these are true:

```text
Android Studio opens the project
Gradle Sync finishes successfully
The phone appears in Android Studio
The app builds without errors
The app installs on the phone
The app opens on the phone
VPN permission is accepted
The app shows protection is active
```

Once all of these steps are complete, GateKeeper Mobile is ready to use.

---

# 18. Final Note

For best results, use:

```text
A real Android phone
A working internet connection
A proper USB data cable
The latest stable version of Android Studio
```

The first setup may take time, but after the first successful build, future builds are usually much faster.
