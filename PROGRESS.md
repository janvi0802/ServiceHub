# ServiceHub – Progress

Deadline: about 2026-11-18. Plan: about one phase per day.

## Phases

| Phase | Status |
|---|---|
| 0 – Setup | ✅ Done (builds) |
| 1 – Skeleton | ✅ Done (tested on phone) |
| 2 – Auth & profile | ✅ Done (tested on phone) |
| 3 – Admin & approval | ✅ Done (tested on phone) |
| 4 – Catalog & search | ✅ Done |
| 5 – Providers & availability | ✅ Done |
| 6 – Booking | ✅ Done (tested: request → accept → PIN → complete) |
| 7 – Customer tracking | ✅ Done (cancel/reschedule/reject still to re-check in Phase 12 test) |
| 8 – Provider jobs | ✅ Done |
| 9 – Notifications | ⏳ Next |
| 10–13 | Not started |

## What works

- Project builds with `gradlew.bat assembleDebug` (Gradle 9.6.0, AGP 9.5.0, Kotlin 2.4.21).
- Firebase connected: `google-services.json` in `app/`, Firebase BoM 35.1.0 (Auth + Firestore).
- Firebase project `servicehub-j0802`: Email/Password login on, Firestore (test mode) created.
- Phase 1: theme, data models, BookingStatus enum, fixed categories/areas/slots,
  all routes as placeholder screens, bottom bar for each role.
- Phase 2: Splash checks login and role; Login; Register (customer or provider, provider
  starts PENDING); blocked users are logged out; Waiting-for-approval screen; Profile edit
  (also updates providers/{uid}); Logout. Repositories: AuthRepository, UserRepository.
- Phase 3: Admin dashboard (live pending count, logout), Load sample data (22 services,
  15 approved sample providers with fixed ids, so it's safe to press twice), live Pending
  providers list with Approve / Reject. AdminRepository + SampleData.
  Sample providers have no login (they exist only to be booked).
- Phases 4–8 (built together):
  - Customer Home (category grid + search bar), Search, Category services (price, duration).
  - Provider list (approved + available + not blocked, area filter chips), Provider detail
    with bio and reviews.
  - Booking form: date (today + 7 days), slot (finished slots today disabled), address
    pre-filled, note, Cash/UPI, price summary (+₹49 visit fee), slot-clash check, 4-digit PIN.
  - My Bookings (Active/Past tabs), Booking detail: live status steps, PIN shown when
    ACCEPTED, call provider, cancel with reason, reschedule (ACCEPTED -> back to REQUESTED).
  - Provider Home (availability switch, today/total earnings, jobs, rating, today's jobs),
    Jobs tab (new requests + upcoming), Job detail (accept, reject with reason, start with
    PIN, complete with "payment received"), History.
  - All status changes go through Firestore transactions in BookingRepository (only the
    allowed moves work). Completing a job updates provider jobsCompleted + totalEarnings
    in the same transaction.
  - Rate & Receipt buttons exist but open placeholders until Phase 10.

## Known issues / notes

- Fixed: after logout + login as another person, tabs showed the old person's data.
  Now every logout starts a fresh navigation session (AppNavHost key(session)).
- Automatic logic tests: app/src/test/.../LogicTest.kt (12 tests, run `gradlew.bat testDebugUnitTest`).

- Internet is slow: the first build took over an hour of downloads. Later builds take minutes.
- Don't accept Android Studio's "upgrade Gradle/AGP" prompts (they trigger big downloads).
- Kotlin was raised from 2.2.10 to 2.4.21 because Firebase 35.1.0 needs it.
- Areas are Ahmedabad areas (change in `FixedData.kt` if needed).
- Firestore test mode ends after 30 days; real rules come in Phase 12.

## Next step

Two-phone test of Phases 4–8, commit, then Phase 9 (notifications) and Phase 10 (reviews, receipt).
