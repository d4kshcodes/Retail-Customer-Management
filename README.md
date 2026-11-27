# Retail Customer Management (Android + Firebase)

A DBMS college project that stores retail customer contact records in **Cloud Firestore**.

## Features
- Register a customer (name, phone, email, address)
- View saved customers in a live-updating list
- Search by name, phone, or email
- Edit an existing customer
- Delete a customer
- Firebase Authentication anonymous sign-in, so Firestore rules can require an authenticated user

## Requirements
- Android Studio (current stable version)
- JDK 17 (Android Studio's bundled JBR is suitable)
- A Firebase account and an Android Firebase project

## Firebase setup (required)
1. Open https://console.firebase.google.com/ and create/select a Firebase project.
2. Add an **Android app** to that Firebase project.
3. Set the Android package name to exactly: `com.example.retailcustomermanagement`
4. Download `google-services.json` and copy it into the `app/` folder (beside `app/build.gradle.kts`).
5. In Firebase Console → **Authentication** → Sign-in method, enable **Anonymous**.
6. In Firebase Console → **Firestore Database**, create a database (choose a location).
7. In Firestore → Rules, publish these rules for this classroom demo:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /customers/{customerId} {
      allow read, write: if request.auth != null;
    }
  }
}
```

The app signs in anonymously, and these rules allow signed-in app users to access the `customers` collection. This is a simple classroom setup, not a complete production authorization design.

## Open and run
1. Extract the ZIP.
2. In Android Studio choose **File → Open** and select the extracted `RetailCustomerManagement` folder (the folder containing `settings.gradle.kts`).
3. Wait for Gradle sync. If Android Studio asks to install Android SDK Platform 35 or Build Tools, accept.
4. Complete the Firebase setup above and put `google-services.json` in `app/`.
5. Click **Run ▶** and select an Android emulator or a USB-debugging-enabled Android phone.

## Create an APK
In Android Studio select **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
The debug APK is normally created at:
`app/build/outputs/apk/debug/app-debug.apk`

## Firestore data structure
Collection: `customers`

Each document contains:
- `name` (string)
- `phone` (string)
- `email` (string)
- `address` (string)
- `createdAt` (timestamp)

## Notes
- `google-services.json` is intentionally not included because it is unique to your Firebase project.
- Do not upload private service-account keys to GitHub. The Android `google-services.json` is a client configuration file, but use Firebase Security Rules to protect your database.
