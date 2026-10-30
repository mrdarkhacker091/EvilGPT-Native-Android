# EvilGPT — Native Android

This project is a native Android rebuild of the EvilGPT chat experience based on the supplied `EVIL-GPT2-new.zip` source archive.

## Native stack

- Kotlin + Jetpack Compose (no WebView, PWA, or Trusted Web Activity)
- Material 3 dark interface using the original EvilGPT black / green / red brand palette and supplied app icon
- Supabase Auth REST endpoints for sign-in and sign-up
- Supabase PostgREST for chat sessions and messages
- Existing `evil-chat` Supabase Edge Function for AI generation and server-sent-event streaming
- `get_public_ai_config` RPC for the live model list, with a fallback list from the existing web bundle
- Gradle + GitHub Actions build pipeline

## Current native screens and behaviours

- Sign in and create account
- Native chat screen and EvilGPT branding
- Model selector using the existing model IDs and live model configuration
- Streaming assistant responses from the existing `evil-chat` function
- Cloud chat history, create/open/rename/delete conversations, and favourites
- Private chat mode (messages are not written to the chat history tables)
- Native image picker and upload to the existing `chat-images` Supabase Storage bucket
- Web-search and deep-think request toggles (the existing backend remains responsible for access/entitlement checks)
- Session refresh using the Supabase refresh token

## Build with GitHub Actions

1. Create or open the GitHub repository for this project.
2. Push the contents of this folder to any branch (the workflow runs on all branches). For a safe first test, use a branch such as `native-rebuild` and keep the existing `main` branch untouched.
3. Open **Actions → Android CI → Build and test native Android app**.
4. When the workflow completes, open its run and download the `EvilGPT-native-debug-apk` artifact.

The workflow installs Java 17, Android SDK platform 35, and Gradle 8.9, then runs:

```bash
gradle --no-daemon clean :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk` and uploaded as a workflow artifact.

## Local build

Install JDK 17, Android SDK platform 35, and Gradle 8.9, then run:

```bash
gradle :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

You can override the Supabase endpoint and publishable key without editing source:

```bash
gradle -PSUPABASE_URL=https://YOUR_PROJECT.supabase.co -PSUPABASE_ANON_KEY=YOUR_PUBLISHABLE_KEY :app:assembleDebug
```

The default Supabase project URL and publishable/anon key are taken from the supplied public web app bundle. They are not AI-provider secrets. Never put service-role keys, provider API keys, SMTP passwords, or other private credentials in the APK or Git repository. AI provider credentials must remain on the server side.

## Important parity notes

This is the first native implementation pass, not a claim of complete feature parity with every page in the supplied website. The core authenticated chat flow, session history, model selection, private mode, and streaming are ported. General document/code-file attachments, read-aloud voice generation, push-notification registration, admin tools, billing/upgrade screens, referral flows, source citations rendering, and some advanced message actions still need dedicated native implementations and backend/device testing.

The original archive included a `.env` file. It was deliberately excluded from this Android project. Do not upload that file to GitHub or bundle its contents into the APK. If the archive has been shared outside a trusted private environment, rotate the private API keys and mail-app password it contained.

## Project identity

- Application ID: `com.evilgpt.app`
- App label: `EvilGPT`
- Minimum Android: API 23 (Android 6.0)
- Target / compile SDK: 35
