# ServiceHub – Final Test Script and Test-Case Table

## Before you start

1. Firestore rules published (see `README.md` → "Security rules").
2. Install the latest build on both phones (Android Studio ▶ Run, or share `app-debug.apk`).
3. Accounts:
   - **Admin**: your admin account (role set to `admin` in the Console).
   - **Customer (Phone A)**: a customer account.
   - **Provider (Phone B)**: a provider you registered yourself (sample providers cannot log in).
4. Admin has pressed **Load sample data** once.

Mark each row ✅ or ❌ and note what you saw.

## Test cases

| ID | Area | Steps | Expected result | Result |
|---|---|---|---|---|
| TC01 | Register | Register a new customer with valid details | Opens Customer Home | |
| TC02 | Validation | Register with 9-digit phone / 5-char password | Clear error message, nothing saved | |
| TC03 | Register | Register as provider (category, experience, bio) | "Waiting for approval" screen | |
| TC04 | Login | Wrong password | "Wrong email or password." | |
| TC05 | Session | Close app and reopen while logged in | Opens home directly (no login) | |
| TC06 | Admin | Admin → Approvals → Approve the new provider | Card disappears; provider gets "Account approved" alert | |
| TC07 | Approval | Provider taps "Check again" / logs in again | Provider Home opens | |
| TC08 | Catalog | Customer → Plumber | 4 services with ₹ price and minutes | |
| TC09 | Search | Search "tap" | "Tap repair" shown; empty search shows hint | |
| TC10 | Area filter | Provider list → tap an area chip | Only providers in that area | |
| TC11 | Availability | Provider switches **Not available** | Provider disappears from the customer list | |
| TC12 | Booking | Book provider: date, slot, address pre-filled, Cash | Price summary = price + ₹49; "Booking requested!" | |
| TC13 | Past slot | Choose Today after 12 PM | Morning slot is disabled | |
| TC14 | Slot clash | Book same provider, same date and slot again | "already booked for this slot" | |
| TC15 | Notification | Phone B Alerts tab | Red badge; "New booking request"; tap opens job; badge clears | |
| TC16 | Accept | Phone B → Accept | Phone A updates live to **Accepted** and shows 4-digit PIN | |
| TC17 | Wrong PIN | Phone B enters a wrong PIN | "Wrong PIN" and status stays Accepted | |
| TC18 | Start | Phone B enters correct PIN → Start | Both phones show **In progress** | |
| TC19 | Complete | Phone B → Complete → tick "Payment received" | **Completed**; provider Today/Total earnings + Jobs done increase | |
| TC20 | Rate | Phone A → Rate 4 stars + comment | Rate button disappears; provider shows ★ 4.0 (1) and the review | |
| TC21 | Rate once | Open the same booking again | No Rate button ("already rated") | |
| TC22 | Receipt | Phone A and Phone B → View receipt | Booking ID, amounts, PAID, payment mode | |
| TC23 | Reschedule | New booking → Accept → customer reschedules | Status goes back to **Requested**; provider alert "rescheduled" | |
| TC24 | Cancel | Customer cancels with a reason | **Cancelled**; provider alert with reason | |
| TC25 | Reject | Provider rejects a request with a reason | Customer sees **Rejected** + reason | |
| TC26 | Not allowed | Try to cancel a Completed booking | No cancel button (status rules) | |
| TC27 | Call | Tap "Call provider" / "Call customer" | Phone dialer opens with the number | |
| TC28 | Admin price | Admin edits Tap repair to ₹159 | Customer sees ₹159 | |
| TC29 | Hide service | Admin hides a service | It disappears from category list and search | |
| TC30 | Block | Admin blocks the customer while they use the app | Customer is logged out; login says "blocked" | |
| TC31 | Block provider | Admin blocks the provider | Provider hidden from customers | |
| TC32 | Dashboard | Admin dashboard | Counts match; completed value = sum of completed bookings | |
| TC33 | Bookings filter | Admin → Bookings → filter Completed | Only completed bookings | |
| TC34 | Profile | Edit address → Save → new booking | New address pre-filled | |
| TC35 | Security | (After rules) customer tries nothing special – just use the app normally | Everything above still works (rules allow normal use) | |
| TC36 | Forgot password | Login → type email → "Forgot password?" | Message "reset link has been sent"; email arrives (check spam); new password works | |
| TC37 | Confirm popups | Provider taps Accept / Start job | "Are you sure?" popup first; Back does nothing | |
| TC38 | Book again | Completed or cancelled booking → "Book again" | Booking form opens with the same provider and service | |
| TC39 | Provider reviews | After TC20, open Provider Home | "Recent reviews" shows the customer's stars and comment | |
| TC40 | Same-moment booking | Two customer phones book the same provider, date and slot and tap Confirm together | One succeeds, the other gets "already booked" | |
| TC41 | Expired request | Book today's earliest open slot; don't accept it until the slot ends (or use an older test booking) | Grey "Expired" chip; it moves to Past / History; provider sees "expired" and no Accept button | |
| TC42 | Find another provider | Open a rejected or expired booking → "Find another provider" | Provider list for the same service opens | |
| TC43 | Provider cancels | Provider accepts → "Can't come? Cancel this job" + reason | Customer sees Cancelled, "The provider cancelled this job", and gets a notification; slot is free again | |
| TC44 | Provider profile | Provider → Profile → change bio and experience → Save | Customer sees the new bio and years on the provider page | |
| TC45 | Version | Profile → bottom | "ServiceHub version 1.0" | |
| TC46 | Dark mode | Turn phone dark mode ON, open the app | Clock/battery icons at the top are still visible (dark) | |
| TC47 | Keyboard | Register / Booking form: tap the last field | Screen moves up; field and button stay visible above the keyboard | |
| TC48 | Category tiles | Customer Home | Each category has its own colour and "from ₹..." price | |
| TC49 | Next booking card | Customer with an open booking → Home | "Your next booking" card with status (and PIN once accepted); tap opens it | |
| TC50 | Rate reminder | After a completed, unrated job → Home | "How was your ...?" card; tap opens Rate screen; disappears after rating | |
| TC51 | Badges | Provider list | "✔ Verified" on every provider; "★ Top rated" only with 4.5+ from 3+ reviews | |
| TC52 | Sort | Provider list → Sort: Experience / Jobs done | Order changes accordingly | |
| TC53 | Timeline | Open a completed booking (both phones) | Timeline shows Requested → Accepted → Started → Completed with times | |
| TC54 | Share | Booking detail share icon; Receipt share icon | Android share menu opens with booking / receipt text | |
| TC55 | Search category | Search "plumber" | All plumber services shown | |
| TC56 | Review alert | Customer rates a job | Provider gets "New review ★★★★" notification | |
| TC57 | Overdue | Accepted job from a past day, not started | Orange "Overdue" chip and message on both sides | |
| TC58 | Admin insights | Admin dashboard | "Expired" count row and "Top providers" list | |

## Automatic tests (no phone needed)

```
gradlew.bat testDebugUnitTest
```

26 logic tests in `app/src/test/java/com/example/servicehub/LogicTest.kt`: status rules,
dates and slots, price total, sample data completeness, provider job lists and earnings,
rating average, admin dashboard counts and top providers, busy-slot ids, expired and overdue
bookings, top-rated badge, avatar colours, provider sorting, timeline, next booking and rate
reminder, "from ₹" prices, search, receipt text.

## Make a signed release APK (to share or install without Android Studio)

1. Android Studio → **Build → Generate Signed App Bundle or APK…** → choose **APK** → Next.
2. **Key store path → Create new…**: save it OUTSIDE the project folder (e.g. `Documents\servicehub-key.jks`),
   choose a password, alias `servicehub`, your name. **Keep this file and password safe; never upload them to GitHub.**
3. Next → choose **release** → **Create**.
4. When it finishes, click **locate**: the file is `app\release\app-release.apk` (about 6 MB).
   Send it to any Android phone and install it ("allow from this source" if asked).
