# Privacy Policy for Veto

Last updated: 6 September 2026

Veto is an open source Android app that lets you scan files, links and
installed apps using the VirusTotal service. This policy explains what happens
to your data.

## Summary

Veto does not have its own servers, accounts, analytics or advertising.
It talks only to VirusTotal, and only when you start a scan.

## What is sent, and where

To analyse an item you choose, the app sends it to VirusTotal through the
official VirusTotal API, authenticated with the API key you provide:

* Files and installed apps: the file (or the app's APK) and its cryptographic
  hashes are uploaded so VirusTotal can analyse them.
* Links: the URL you enter is submitted to VirusTotal.

The analysis results are returned by VirusTotal and shown in the app. This
processing is performed by VirusTotal and is governed by the VirusTotal privacy
policy and terms of service. Veto is not affiliated with VirusTotal.

## What is stored on your device

The following are stored locally on your device using Android DataStore and are
never sent anywhere except to VirusTotal as needed to perform a scan:

* Your VirusTotal API key
* Your saved scans
* Your app settings

Your API key is treated as a credential. It is encrypted with an AES-256-GCM key
held in the Android Keystore, which never leaves the device, and the settings
store that holds it is excluded from cloud backup and from device to device
transfer. Your key therefore stays on the device you entered it on. If you
restore the app onto a new device you will be asked for the key again.

You can remove this data at any time by clearing the app's storage or
uninstalling the app.

## Permissions

* Internet: to reach the VirusTotal API.
* Query all packages: to list your installed apps so you can pick one to scan.
* Post notifications: to show scan progress and the result notification.
* Foreground service and data sync: to keep a scan running when the app is in
  the background.
* Vibrate: for haptic feedback.

## Children

Veto is not directed at children.

## Changes

If this policy changes, the updated version will be published in the project
repository.

## Contact

makerlab.fffm+github_veto@gmail.com
