# ServiceHub – Progress

Deadline: about 2026-11-18. Plan: about one phase per day.

## Phases

| Phase | Status |
|---|---|
| 0 – Setup | ✅ Done (builds) |
| 1 – Skeleton | ✅ Done (tested on phone) |
| 2 – Auth & profile | ✅ Done (tested on phone) |
| 3 – Admin & approval | ⏳ Next |
| 4–13 | Not started |

## What works

- Project builds with `gradlew.bat assembleDebug` (Gradle 9.6.0, AGP 9.5.0, Kotlin 2.4.21).
- Firebase connected: `google-services.json` in `app/`, Firebase BoM 35.1.0 (Auth + Firestore).
- Firebase project `servicehub-j0802`: Email/Password login on, Firestore (test mode) created.
- Phase 1: theme, data models, BookingStatus enum, fixed categories/areas/slots,
  all routes as placeholder screens, bottom bar for each role.
- Phase 2: Splash checks login and role; Login; Register (customer or provider, provider
  starts PENDING); blocked users are logged out; Waiting-for-approval screen; Profile edit
  (also updates providers/{uid}); Logout. Repositories: AuthRepository, UserRepository.

## Known issues / notes

- Internet is slow: the first build took over an hour of downloads. Later builds take minutes.
- Don't accept Android Studio's "upgrade Gradle/AGP" prompts (they trigger big downloads).
- Kotlin was raised from 2.2.10 to 2.4.21 because Firebase 35.1.0 needs it.
- Areas are Ahmedabad areas (change in `FixedData.kt` if needed).
- Firestore test mode ends after 30 days; real rules come in Phase 12.

## Next step

Phone test of Phase 2, commit, then Phase 3 (admin account, sample data, approvals).
