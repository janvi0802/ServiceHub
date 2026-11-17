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
| 9 – Notifications | 🟡 Built, waiting for phone test |
| 10 – Reviews & receipt | 🟡 Built, waiting for phone test |
| 11 – Admin control | 🟡 Built, waiting for phone test |
| 12 – Finish (rules, polish, test script) | 🟡 Rules written (`firestore.rules`), icon done, test script in `docs/TEST_SCRIPT.md`. To do: publish rules in Console + final phone test |
| 13 – Report kit | ✅ `README.md`, `docs/REPORT_KIT.md` (diagrams, future scope, 25 viva Q&A) |

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
- Phases 9–11 (built together):
  - Notifications: NotificationRepository; created on new request, accepted, rejected,
    started, completed, cancelled, rescheduled, provider approved/rejected. Alerts tab
    (customer + provider) with unread badge; tap opens the booking and marks read; mark all read.
  - Rate & review: 1–5 stars + comment, one per booking (review id = booking id). One
    transaction writes review + provider ratingAvg/ratingCount + booking.isRated.
  - Receipt screen for completed bookings (customer and provider).
  - Admin: dashboard counts (customers, providers, pending, blocked, bookings by status,
    completed value), Users (filter, block/unblock with confirm), All providers (filter,
    approve/reject), Manage services (edit price/description, hide/show), All bookings
    (status filter).
  - No placeholder screens left.

## Known issues / notes

- Fixed: after logout + login as another person, tabs showed the old person's data.
  Now every logout starts a fresh navigation session (AppNavHost key(session)).
- Automatic logic tests: app/src/test/.../LogicTest.kt (14 tests, run `gradlew.bat testDebugUnitTest`).
- Self-check (17 Nov): lint 0 errors (warnings harmless: Firebase StaticFieldLeak is the
  normal singleton pattern, unused template colors, newer library versions not applied on
  purpose). Fixed: booking now re-checks provider approved/available/not blocked and
  service active at save time (uses latest price); blocked users are logged out live.

- Internet is slow: the first build took over an hour of downloads. Later builds take minutes.
- Don't accept Android Studio's "upgrade Gradle/AGP" prompts (they trigger big downloads).
- Kotlin was raised from 2.2.10 to 2.4.21 because Firebase 35.1.0 needs it.
- Areas are Ahmedabad areas (change in `FixedData.kt` if needed).
- Firestore test mode ends after 30 days; real rules come in Phase 12.

- Improvements (after Phase 13): busy-slot documents claimed in a transaction (race-proof
  double-booking check; bookings now private in rules), forgot password, confirm popups for
  Accept/Start, Book again, recent reviews on Provider Home. 15 logic tests, lint 0 errors.
  Note: test bookings made BEFORE this change have no slot document, so they don't block
  their slot. New bookings do.

## Next step

1. Publish `firestore.rules` in Firebase Console (Firestore → Rules → paste → Publish).
2. Run `docs/TEST_SCRIPT.md` on two phones (35 cases). Fix anything that fails.
3. Take screenshots from the checklist in `docs/REPORT_KIT.md`.
