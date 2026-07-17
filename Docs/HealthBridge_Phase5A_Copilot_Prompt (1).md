# HealthBridge -- Phase 5A (UIManager Refactor)

**Repository:** HB_BCK\
**Branch:** restore-session

## Verify before changing anything

Verify that you are working from the current **restore-session** branch.

Verify that:

**MainActivity.kt** contains: - SpeechManager - MessageManager -
MapManager - TelemetryEngine

**activity_main.xml** contains: - statusText - messageEdit -
speakButton - sendButton - ackButton - ackStatus - map

If any of these are missing, **STOP** and report the mismatch. Do not
generate code.

------------------------------------------------------------------------

## Goal

Create **UIManager.kt**.

Do **NOT** create MainActivityCoordinator.

Move only UI-related responsibilities from MainActivity into UIManager.

Examples: - View binding - Status display - Message display - ACK
display - Button initialization - UI helper methods

Do **NOT** move business logic.

------------------------------------------------------------------------

## Preserve exactly

Do not change: - Firebase paths - Firebase schema - JSON tree -
MessageManager - SpeechManager - MapManager - TelemetryEngine - M1 ↔ M2
messaging - Speech recognition - Text-to-Speech - GPS - Map display

Application behavior must remain unchanged.

------------------------------------------------------------------------

## Deliverables

1.  UIManager.kt
2.  Updated MainActivity.kt
3.  Build report
4.  List of modified files
5.  Number of lines removed from MainActivity
6.  Summary of what was moved
7.  Confirmation that application behavior is unchanged

Create the result as a **GitHub Pull Request**. Do **NOT** commit
directly to restore-session.
