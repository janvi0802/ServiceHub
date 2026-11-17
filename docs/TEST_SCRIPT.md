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

## Automatic tests (no phone needed)

```
gradlew.bat testDebugUnitTest
```

14 logic tests in `app/src/test/java/com/example/servicehub/LogicTest.kt`: status rules,
dates and slots, price total, sample data completeness, provider job lists and earnings,
rating average, admin dashboard counts.
