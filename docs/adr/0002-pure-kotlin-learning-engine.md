# 0002 – Keep the learning engine in pure Kotlin modules

**Status:** accepted · 2026

## Context
In ProjectShaco all logic lived in Activities and a Room DAO, so it could not be tested without a device.
The interesting part of Engram is pedagogical: scheduling, exercise choice, answer checking and session
flow. It should be easy to test and to reuse.

## Decision
`:core:srs`, `:core:model` and `:core:learning` are `kotlin("jvm")` modules with **no Android dependency**
(only `java.time` and the Kotlin stdlib). `StudySession` is a plain state machine. The ViewModel persists
its results and drives the UI.

## Consequences
- 75 engine tests run on the JVM in under a second. CI runs them as a separate fast job.
- The engine could move to Kotlin Multiplatform (iOS/desktop/web) with little change.
- Android concerns (Room, DataStore, TTS, WorkManager) stay in `:core:data` behind interfaces
  (`StudyRepository`, `Speaker`, …), which also makes ViewModels testable with fakes.
