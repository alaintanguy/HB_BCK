# HealthBridge Refactoring Prompt

## Objective

You are refactoring an existing Android Studio project written in
Kotlin.

### VERY IMPORTANT

The application is **working correctly**.

Your mission is to improve the architecture **without changing the
behavior**.

The following features are already working and **must remain
unchanged**:

-   Firebase Realtime Database
-   GPS telemetry
-   Voice messaging
-   Text-to-Speech
-   M1 ↔ M2 conversation cycle
-   Message states (new → played)
-   NetworkMonitor
-   Role management

### Do NOT

-   Change Firebase paths.
-   Rename database nodes.
-   Modify the conversation logic.
-   Change telemetry behavior.
-   Change TTS behavior.
-   Change VoiceMessage fields.
-   Introduce new features.
-   Redesign algorithms.

The application must behave exactly as before.

------------------------------------------------------------------------

# Phase 1 -- Speech Manager

Create:

`SpeechManager.kt`

Move all TextToSpeech code into this class.

MainActivity should only call:

``` kotlin
speechManager.initialize()
speechManager.speak(text)
speechManager.shutdown()
```

Compile before continuing.

------------------------------------------------------------------------

# Phase 2 -- Message Manager

Create:

`MessageManager.kt`

Move all message sending and receiving logic from MainActivity.

MainActivity should only call:

``` kotlin
messageManager.send(...)
messageManager.listen(...)
messageManager.acknowledge(...)
```

Compile before continuing.

------------------------------------------------------------------------

# Phase 3 -- Role Manager

Create:

`RoleManager.kt`

Move all role loading logic.

MainActivity should only call:

``` kotlin
roleManager.loadRole()
```

Compile before continuing.

------------------------------------------------------------------------

# Phase 4 -- Permission Manager

Create:

`PermissionManager.kt`

Move all permission handling into this class.

Include:

-   Location permission
-   GPS permission
-   Bluetooth permission
-   Microphone permission

Compile before continuing.

------------------------------------------------------------------------

# Phase 5 -- Network

Move:

`NetworkMonitor.kt`

into its own package and update imports.

Compile before continuing.

------------------------------------------------------------------------

# Phase 6 -- Clean MainActivity

Reduce MainActivity so it mainly coordinates the managers.

Do not change application behavior.

------------------------------------------------------------------------

# Files that must NOT be modified

-   FirebaseManager
-   TelemetryEngine
-   GpsCollector
-   BatteryCollector
-   VoiceMessage
-   Firebase message format
-   Conversation state machine

------------------------------------------------------------------------

# Add section headers

Use section comments such as:

``` kotlin
// =====================================================
// INITIALIZATION
// =====================================================

// =====================================================
// ROLE MANAGEMENT
// =====================================================

// =====================================================
// MESSAGING
// =====================================================

// =====================================================
// TELEMETRY
// =====================================================

// =====================================================
// NETWORK
// =====================================================
```

------------------------------------------------------------------------

# Workflow

After EACH phase:

1.  Fix compilation errors.
2.  Verify imports.
3.  Verify Gradle build.
4.  Commit to Git with a meaningful commit message.
5.  Continue only after the project compiles successfully.

------------------------------------------------------------------------

# Deliverables

Return the complete Android Studio project.

Requirements:

-   Builds successfully.
-   No TODO placeholders.
-   No pseudocode.
-   Production-ready Kotlin.
-   Application behavior remains identical.
