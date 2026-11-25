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
- Workflow fixes: expired requests (grey "Expired", can't be accepted, move to Past/History),
  provider can cancel an ACCEPTED job (reason prefixed "Provider: ", customer notified),
  providers edit bio + experience in Profile, "Find another provider" after reject/expiry,
  version on Profile. 16 logic tests, lint 0 errors, release build OK (6.2 MB).
  Signed APK steps in docs/TEST_SCRIPT.md (user creates the key).
- Polish round: fixed dark status-bar icons in dark mode, keyboard covering fields (imePadding),
  half-made accounts on failed registration (deleted so user can retry), "Overdue" accepted
  jobs, expired counted separately for admin. Added colourful category tiles with "from ₹",
  next-booking card + rate reminder on Customer Home, coloured avatars, Verified/Top rated
  badges, friendlier empty screens, booking timeline with times, share booking/receipt,
  provider sorting, search by category, new-review notification, admin top providers.
  26 logic tests, lint 0 errors, release build OK. Test script now 58 cases.
- User-analysis round: weekly days off (providers.offDays; only blocks NEW bookings; checked on
  booking and reschedule; "Off today" label + "⚡ Available today" filter), taken slots greyed
  out live in the booking form, provider list starts in the customer's own area with
  "Show all providers", "spare parts extra" price note, Open in Maps (geo: link, no API key),
  Report a problem (complaints collection) + admin Complaints screen with call buttons and
  Mark resolved (customer notified), admin Add service, admin user search.
  29 logic tests. Rules updated (offDays, complaints) — must be published again.
- Full re-check: every app write verified against firestore.rules (all allowed, nothing extra).
  Fixed: reschedule popup now greys out days off and taken slots; booking form starts on the
  first day with a free slot (not "today" after 8 PM); server refuses past dates/finished
  slots for booking and reschedule; provider whose approval is withdrawn is moved to the
  waiting screen live; rate reminder only for 7 days; rejected providers can "Check again".
  30 logic tests, lint 0 errors.
- Known prototype limits (documented): any active user can create a notification for anyone
  (needed so customers can notify providers; real fix = Cloud Functions, paid plan);
  a customer could change any provider's rating fields via rules (same reason).

- Growth round: admin-managed categories (built-in 6 + `categories` collection, CategoryStore,
  21 icons, 10 colours, hide/show; hidden category hides its services), 2-step registration
  (big role cards, nothing pre-selected, "Register as provider (X)"), "Finish your provider
  profile" for provider accounts with no details, richer Customer Home (blue header with area +
  search, trust strip, How it works, Quick picks, Book again), Provider Home (orange header,
  Next job with Call/Maps, This-week bar chart), requests badge on Jobs tab, brand logo on Login.
  Rules changed (categories) — publish again.
- Customer booking round: clear Book buttons (Home "Book a service", service cards, provider
  cards, quick picks), Service details page (services.included / notIncluded, sample text for
  all 22, admin can edit, defaults when empty), Review-before-confirm step (BackHandler, Edit
  links, saved only on Confirm), Book for someone else (bookings.contactName / contactPhone,
  provider "Call … at the address"), Favourite professionals (users.favoriteProviderIds, heart on
  cards/profile, My favourites screen), Help & support FAQs (customer/provider). Show/hide
  password eye; admin "Send password reset" link; category filter on admin Users + Bookings.
  Admin-sets-password-directly = future scope (needs Cloud Functions / Blaze).
  Rules updated (favoriteProviderIds) — publish again.
- Real-world flows round: "I'm on my way" (bookings.onTheWayAt, no status change, customer
  banner + notification + timeline), extra parts/work at completion (bookings.extraItems /
  extraAmount, total recalculated, itemized booking/receipt/share), 7-day service guarantee
  (revisitStatus REQUESTED/DONE, customer "Free revisit", provider "Free revisits" section +
  badge, admin attention item), multiple saved addresses (users.savedAddresses, Profile section,
  one-tap chips in booking form), earnings split (visit fee = platform income; provider earnings
  = total − visit fee; admin "Earnings split" card). Rules updated (savedAddresses) — publish again.
- Design round (17 Nov): new design system (refined palette, typography, shapes, white cards on
  grey, soft status pills, icon tiles, section headers, fixed bottom action bars, helpful empty
  states with actions) — no emoji decoration anywhere. Redesigned: booking cards, provider profile
  (stats header, about, rating summary bars, reviews), booking form bottom bar, My Bookings
  (counts + Today/Upcoming/Needs attention), Provider Home (clean header, day-off banner with
  "Work today", requests banner, stat tiles), Admin dashboard (Needs attention, key numbers,
  7-day bookings chart, bookings by status, earnings by category, top providers, recent activity),
  Admin user detail (contact, role, history, spent/earned, block), Profile (sections, role-specific
  help & safety, logout confirm), Notifications (icon per type, Today/Earlier).
  Days off: providers.extraWorkDates ("Work today" opens one day off), "Off today · next
  available" labels, clear booking-form banner. Rules updated (extraWorkDates) — publish again.
- Firestore security rules PUBLISHED in the Console (test-mode expiry of 4 Dec 2026 no longer applies).

## Next step

1. Publish `firestore.rules` in Firebase Console (Firestore → Rules → paste → Publish).
2. Run `docs/TEST_SCRIPT.md` on two phones (35 cases). Fix anything that fails.
3. Take screenshots from the checklist in `docs/REPORT_KIT.md`.

## Round: UPI test payment + admin password change (2026-11-25)

- Customer **Pay now** (Razorpay TEST mode, no real money) on a completed booking whose payment is pending.
  Booking becomes PAID with Payment ID; provider is notified; receipt shows the Payment ID.
- Provider can complete a UPI job without "already received", leaving payment pending for the customer.
- Admin **Change password** (User details) through a free Vercel server in `admin-server/` (Firebase Admin SDK).
  The user is signed out everywhere and gets an in-app notification. "Reset link" still works without the server.
- Setup needed in `local.properties` (never uploaded): `RAZORPAY_KEY_ID=rzp_test_...` and `ADMIN_API_URL=https://...`.
  Without them the buttons are shown disabled with a hint.
- Test: Pay now → UPI ID `success@razorpay` (passes) / `failure@razorpay` (fails, stays pending).

## Round: payment step, redo PIN, easier navigation (2026-11-25)

- Payment is a separate step after Complete: Cash -> provider taps "Cash collected"; UPI -> customer Pay now, provider sees it automatically.
- UPI payment is saved by the app itself (not the screen), so it cannot be lost if the screen closes.
- Redo needs the customer's 4-digit Redo PIN (proof the provider came back).
- Earnings: Today / Total count only money received; "to collect" banner and Requests section for unpaid jobs.
- Home "Book a service" opens a 2-step form (category -> service -> professional).
- "NEXT STEP" box on customer booking and provider job screens; Payment due card on customer Home.
- Admin: badges on Approvals (pending providers) and Dashboard (open complaints), logout confirmation, dashboard rows open filtered bookings (Redo / Expired / Unpaid).
- Fixed: jobs on the same day were sorted alphabetically (Evening before Morning).
- Security rules tightened (each side changes only its own booking fields; earnings only go up; ratings 1-5; notifications only about your own bookings). **Publish firestore.rules again.**
- Known gaps: tapping a booking in Admin -> All bookings does nothing (no admin booking detail screen).
  The provider's app technically downloads the PINs with the booking (hidden on screen); real protection would need a server (future scope).

## Round: clean Home screens, 1-hour rule, payment switch, admin detail (2026-11-25)

- Customer and provider Home rebuilt in clear sections with one shared style (SectionLabel, ActionList rows, small SoftIcon). Trust strip removed.
- Slots: "On hold" (requested) vs "Booked" (accepted). An unanswered request holds the slot for 1 hour, then others may take it.
  Customer is told once ("No answer from …") when the app is open; push would need a paid server.
- Payment method can be switched Cash <-> UPI until paid; after the job the customer can always Pay now and the provider can always mark cash.
- Earnings use one rule: Today / This week = money kept from PAID jobs; To collect = what customers still owe.
- Review booking page: one summary card + price card with Cash/UPI choice.
- Admin: read-only booking detail (tap any booking in Bookings).
- **Publish firestore.rules again** (slot hold/accept rules, new booking fields).

## Round: coupons (2026-11-25)

- Admin creates coupons (₹ off or % off, minimum bill, max discount, on/off). Customer applies a code on the review page; booking stores couponCode and discount.
- The discount is ServiceHub's offer: provider share unchanged; platform earnings = visit fee − discount.
- Rules: coupons readable by logged-in users, written only by admin; a booking may have a discount only with an active coupon. **Publish firestore.rules again.**
- Next: booking chat, then push notifications.

## Round: booking chat + push notifications (2026-11-25)

- Chat: bookings/{id}/messages, only the booking's customer and provider (rules), open until 7 days after the job; each message also creates a notification.
- Push: Firebase Cloud Messaging (free). Phone token saved on users/{uid}; each notification asks the Vercel server (api/send-push.js) to push it; the server checks the sender is on that booking. Needs ADMIN_API_URL and the pushed server code.
- Limit: the 1-hour 'no answer' alert is still created when the customer's app is opened (a timed push would need a scheduled server).
- **Publish firestore.rules again** (messages, fcmToken).
