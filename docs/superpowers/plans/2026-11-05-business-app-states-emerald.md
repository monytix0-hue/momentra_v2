# Business App States in Emerald Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement and refine the business app states in Emerald across Android (Jetpack Compose) and iOS (SwiftUI) matching the Figma design (`https://www.figma.com/design/TzLvwVwlPbeVB8ug1zB3GM/momentra?node-id=2852-39645`).

**Architecture:** Add an Emerald accent mode (`BusinessAppStateAccent.Emerald` in Android and corresponding Swift tokens in iOS) and ensure all 8 core app states (Pulse, Moments, Life, Memory, Hub, ViewAll, NoCompany, Offline) render correctly with Emerald theme tokens, loading skeletons, error diagnostics, and empty states.

**Tech Stack:** Jetpack Compose, Kotlin, SwiftUI, Swift.

**Spec:** Figma node 2852-39645 (Business App States in Emerald).

## Global Constraints

- Accent hex: `#10B981` (Emerald), secondary/end `#059669`.
- Skeletons, error diagnostics, and offline resilience must match the established Business v3 app state specifications.
- Never render missing figures as `0` — use "Not recorded", "None yet", or "Quiet".

## Review Focus

1. **Emerald Token Propagation:** Ensure all components referencing `BusinessAppStateAccent` correctly inherit `#10B981` when Emerald is selected.
2. **Offline & Error Resilience:** Verify error state cards correctly render error codes and recovery actions.
3. **Typography & Spacing:** Check Plus Jakarta Sans typography weights and 24dp screen horizontal padding.

---

### Task 1: Add Emerald Accent to Android App State Tokens

**Files:**
- Modify: `apk/app/src/main/java/com/example/momentra/ui/shell/business/appstate/BusinessAppStateTokens.kt`
- Modify: `apk/app/src/main/java/com/example/momentra/ui/shell/business/appstate/BusinessActiveMomentsDirectory.kt`

**Interfaces:**
- Consumes: Existing `BusinessAppStateAccent` enum.
- Produces: `BusinessAppStateAccent.Emerald(Color(0xFF10B981))` and bucket mapper.

- [ ] **Step 1: Add `Emerald` to `BusinessAppStateAccent` enum in `BusinessAppStateTokens.kt`**
- [ ] **Step 2: Update `businessDirectoryBucketFor` in `BusinessActiveMomentsDirectory.kt` to support Emerald**
- [ ] **Step 3: Build and test Android compilation**
- [ ] **Step 4: Commit changes**

### Task 2: Add Emerald Theme Support to iOS Business Shell

**Files:**
- Modify: `momentra/momentra/Shell/` (check matching Swift files for accent tokens)

**Interfaces:**
- Consumes: Swift business tokens.
- Produces: Emerald accent support in iOS app states.

- [ ] **Step 1: Add Emerald color constant in Swift tokens**
- [ ] **Step 2: Verify SwiftUI compilation**
- [ ] **Step 3: Commit changes**

---

Plan complete and saved. Please review the plan. Does it capture what you want? Which execution approach would you prefer (Subagent-driven vs Native)?