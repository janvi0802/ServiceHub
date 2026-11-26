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
| TC59 | Days off | Provider Home → tick **Sun** | Saves at once; provider detail shows "Weekly off: Sun" | |
| TC60 | Days off in form | Customer books that provider | Sunday chip shows "· off" and is disabled; form starts on the first working day | |
| TC61 | Off today | On the provider's day off, open the provider list | "Off today" label; "⚡ Available today" chip hides them | |
| TC62 | Existing job stays | Provider has a Sunday job, then ticks Sun | The existing job is still there (only new bookings are blocked) | |
| TC63 | Busy slots | Customer A books Morning; customer B opens the same provider and date | Morning shows "· Booked" and cannot be chosen | |
| TC64 | Own area first | Customer with area Satellite opens a provider list | "Satellite" chip is pre-selected; if empty, "Show all providers" button works | |
| TC65 | Price note | Category services and booking summary | "Price covers labour. Spare parts..." note visible | |
| TC66 | Open in Maps | Provider → active job → Open in Maps | Google Maps opens searching the customer's address | |
| TC67 | Report a problem | Customer → booking → "Report a problem" → text → Send | "Problem reported..." shown; admin dashboard Open complaints = 1 | |
| TC68 | Resolve | Admin → Open complaints → Call buttons, Mark resolved | Complaint moves to Resolved; customer gets a notification and sees "resolved" | |
| TC69 | Add service | Admin → Services → + Add (e.g. Geyser repair, ₹399, 60 min) | Appears for customers in that category and in search | |
| TC71 | Reschedule smart | Reschedule a booking of a provider with Sunday off | Sunday shows "· off", taken slots "· Booked", Save disabled for the same time | |
| TC72 | Late evening | Open a booking form after 8 PM | Form starts on tomorrow (or next working day) | |
| TC73 | Approval withdrawn | Admin → All providers → Reject an approved provider who is using the app | Provider's phone switches to "Application rejected" at once | |
| TC74 | Register step 1 | Log out → Create an account | Two big cards; nothing selected; no form until you choose | |
| TC75 | Register step 2 | Choose "I am a service provider" | Title "Create provider account", category field visible, button "Register as provider (…)"; "Change" goes back | |
| TC76 | Missing provider details | (Console) set a customer's role to provider, then log in as them | "Finish your provider profile" form; after Send, admin sees them in Approvals | |
| TC77 | New category | Admin → Services → Categories → + Add "Yoga Teacher" (yoga icon, green) | Appears on Customer Home ("coming soon") and in provider Register category list | |
| TC78 | New category services | Admin → Services → + Add → category Yoga Teacher (e.g. Home yoga session ₹499) | Customer sees "from ₹499"; can book a Yoga provider once one is approved | |
| TC79 | Hide category | Admin → Categories → switch off Painter | Painter tile and its services vanish for customers (also from search) | |
| TC80 | Customer home | Customer Home | Blue header with name + area + search, trust strip, Quick picks row, "How it works" for a new customer, Book again after a completed job | |
| TC81 | Provider home | Provider Home | Orange header, "Next job" card with Call / Open in Maps, "This week" bar chart | |
| TC82 | Requests badge | Customer books → provider's bottom bar | Red number on the Jobs tab | |
| TC83 | Day off today | Provider with today's weekday as day off → Provider Home | Banner "Today is your weekly day off" with **Work today** | |
| TC84 | Work today | Tap Work today; customer opens that provider | Today is bookable; banner says "open for bookings today" with Undo | |
| TC85 | Off today label | Customer provider list on a provider's day off | "Off today · next available …" under the name | |
| TC86 | Provider profile | Open a provider | Big avatar, Rating / Experience / Jobs, About with area and days off, rating bars | |
| TC87 | My bookings groups | Customer with bookings today and later | "Today", "Upcoming" groups; tab counts | |
| TC88 | Admin dashboard | Admin home | Needs attention list, key numbers, 7-day chart, status counts, earnings by category, recent activity | |
| TC89 | User detail | Admin → Users → tap a user | Details, Call, Block/Unblock (with confirm), bookings and spent/earned | |
| TC90 | Profile | Any role → Profile | Header card, Personal details, Help & safety for that role, Log out asks to confirm | |
| TC91 | Notification icons | Alerts | Each notification has its own icon/colour; grouped Today / Earlier | |
| TC92 | On my way | Provider: accepted job → "I'm on my way" | Customer sees "… is on the way", gets a notification; timeline shows "On the way" | |
| TC93 | Extra parts | Provider: Complete → + Add item "Tap cartridge" ₹180 | Amount to collect updates; receipt lists the item; total = service + item + visit fee | |
| TC94 | Bad extra | Item with no name or ₹0 | Complete button disabled with a message | |
| TC95 | Complaint → job redo | Customer: completed booking (within 7 days) → "Not happy with the work? Get it redone free" → describe the problem → Send complaint | Provider sees it under "Job redo requests" with a badge and a notification; admin dashboard lists it | |
| TC96 | Job redone | Provider → Mark as redone | Customer sees "The job was redone free of charge" + notification; the option is gone (once per booking) | |
| TC112 | Save errors are shown | (Only if rules are old) add a favourite or a saved address | A message explains "permission denied – publish the latest rules" instead of nothing happening | |
| TC97 | Saved addresses | Profile → Saved addresses → + Add "Work" | Booking form shows Profile / Work chips; tapping fills the address | |
| TC98 | Earnings split | Complete a ₹149 job (total ₹198) | Provider earnings +₹149; admin "Earnings split": platform ₹49, providers ₹149 | |
| TC99 | Show password | Login / Register: tap the eye icon in the password box | Password becomes visible; tap again to hide | |
| TC100 | Admin reset password | Admin → Users → key icon on a user with a real email → Send link | Message "reset link sent"; the user receives the email and can set a new password | |
| TC101 | No reset for samples | Admin → Users → a sample provider | No key icon / Reset button (no real inbox) | |
| TC102 | Users by category | Admin → Users → Category: Plumber | Only plumbers are listed | |
| TC103 | Bookings by category | Admin → Bookings → Category: Painter (+ a status) | Only matching bookings; count and completed value update | |
| TC104 | Book a service | Customer Home → "Book a service" | Scrolls to All categories | |
| TC105 | Service details | Category → tap a service card | Details page: price, time, what's included / not included, how it works, promise, Book now bar | |
| TC106 | Book buttons | Service card "Book"; provider card "Book" | Go straight to choosing a provider / to the booking form | |
| TC107 | Review step | Fill the form → Review booking | Summary with Edit links; nothing saved until Confirm booking; Back returns to the form | |
| TC108 | Book for someone else | Booking for: Someone else + name + phone | Summary shows them; provider's job shows "Contact person" and "Call … (at the address)" | |
| TC109 | Favourites | Heart on a provider → Profile → My favourites | Provider listed; Book opens their profile; heart again removes them | |
| TC110 | Help | Profile → Help & support (customer and provider) | Role-specific questions; tap to expand | |
| TC111 | Admin service text | Admin → Services → edit → What's included | New points show on the customer's details page | |
| TC70 | User search | Admin → Users → type part of a name/email/phone | List filters as you type | |

## Automatic tests (no phone needed)

```
gradlew.bat testDebugUnitTest
```

29 logic tests in `app/src/test/java/com/example/servicehub/LogicTest.kt`: status rules,
dates and slots, price total, sample data completeness, provider job lists and earnings,
rating average, admin dashboard counts and top providers, busy-slot ids, expired and overdue
bookings, top-rated badge, avatar colours, provider sorting, timeline, next booking and rate
reminder, "from ₹" prices, search, receipt text, weekdays and days off, admin user search.

## Make a signed release APK (to share or install without Android Studio)

1. Android Studio → **Build → Generate Signed App Bundle or APK…** → choose **APK** → Next.
2. **Key store path → Create new…**: save it OUTSIDE the project folder (e.g. `Documents\servicehub-key.jks`),
   choose a password, alias `servicehub`, your name. **Keep this file and password safe; never upload them to GitHub.**
3. Next → choose **release** → **Create**.
4. When it finishes, click **locate**: the file is `app\release\app-release.apk` (about 6 MB).
   Send it to any Android phone and install it ("allow from this source" if asked).

## New cases (UPI test payment, admin password)

| TC | Steps | Expected |
|---|---|---|
| 113 | Provider completes a UPI job, leaves "Already received" unticked | Booking COMPLETED, payment pending; customer sees **Pay now** |
| 114 | Customer → Pay now → UPI `success@razorpay` | Booking PAID, Payment ID shown, provider gets "Payment received" |
| 115 | Customer → Pay now → UPI `failure@razorpay` (or close) | Message shown, payment stays pending |
| 116 | Admin → Users → a real customer → Change password → new password twice | Success message; user is signed out and gets a notification; new password logs in |
| 117 | Change password with mismatching boxes / under 6 characters | Change button disabled, error text shown |

## New cases (payment step, redo PIN, easier navigation)

| TC | Steps | Expected |
|---|---|---|
| 118 | Provider completes a **Cash** job | Job screen: "Collect ₹X in cash"; customer sees "Please pay ₹X in cash" |
| 119 | Provider taps **Cash collected** → Yes | Booking PAID; customer gets "Cash payment confirmed"; provider Today earnings go up |
| 120 | Provider completes a **UPI** job, customer pays (`success@razorpay`) | Provider screen changes by itself to "UPI payment received" with Payment ID, plus notification |
| 121 | UPI job, customer pays cash instead | Provider taps **Customer paid cash instead** → PAID (Cash) |
| 122 | Customer asks for a redo | Customer sees a **Redo PIN**; provider sees a PIN box under the complaint |
| 123 | Provider enters a wrong redo PIN | "Wrong redo PIN", stays requested; correct PIN → "Job redone" |
| 124 | Home → **Book a service** → Category → Service → Choose professional | Price preview shown; provider list opens filtered to your area |
| 125 | Unpaid finished booking | Customer Home shows "Payment due"; booking stays in **Active** until paid |
| 126 | Provider with an unpaid job | Home shows "₹X to collect"; Requests tab lists **Payments to collect**; tab badge counts it |
| 127 | Provider has Morning and Evening jobs on the same day | Morning is listed first |
| 128 | Admin: a provider registers / a complaint is sent | Red number on **Approvals** / **Dashboard** tab |
| 129 | Admin dashboard → "job redo requests" / "expired" rows | All bookings opens already filtered (Redo / Expired); "Unpaid" filter also available |
| 130 | Admin taps logout icon | "Log out?" confirmation first |
| 131 | Booking / job screen in each status | A "NEXT STEP" box says exactly what to do now |

## New cases (clean Home screens, 1-hour rule, payment switch, admin detail)

| TC | Steps | Expected |
|---|---|---|
| 132 | Customer A requests a slot; customer B opens the same provider/date | B sees that slot as **"· On hold"** with a short explanation |
| 133 | Provider accepts A's request; B looks again | Slot shows **"· Booked"** |
| 134 | Provider does not answer for over 1 hour; B looks again | Slot is free again; B can book it |
| 135 | A opens the app after that hour | Notification "No answer from …"; booking shows **Other provider / Change time** |
| 136 | Provider accepts A's old request after B took the slot | Refused: "slot was taken … choose a new time" |
| 137 | Customer: booking with Cash → "switch to UPI" (and back) | Payment method changes; provider gets a notification and sees it live |
| 138 | Completed Cash booking | Customer can still **Pay now** by UPI; provider can still tap **Cash collected**; whichever is first wins |
| 139 | Review booking page | One summary card (what / when / where) + Price card with Cash / UPI choice |
| 140 | Customer Home | Sections: Needs your attention, Your next booking (PIN box), Book a service, Categories, Book again, Popular services |
| 141 | Provider Home | Sections: Needs your attention, Next job, Today's jobs, Earnings (3 equal tiles), Availability (days off in a popup), Reviews |
| 142 | Admin → Bookings → tap a booking | Read-only detail with Call customer / Call provider, reasons and timeline |

## New cases (coupons)

| TC | Steps | Expected |
|---|---|---|
| 143 | Admin → Services → **Coupons** → + Add `FIRST50`, ₹ off, 50, min 199 | Coupon listed with an on/off switch |
| 144 | Customer books a ₹299 service → Review → coupon `first50` → Apply | "FIRST50 applied: ₹50 off"; total 299 + 49 − 50 = ₹298 |
| 145 | Coupon on a bill under the minimum, or a wrong code | Clear error message, no discount |
| 146 | Admin turns the coupon off, customer confirms a booking that had it applied | Booking refused with "no longer active" |
| 147 | Booking with a coupon: detail, receipt, provider Complete popup | "Coupon FIRST50 − ₹50" line shown; provider's earning still bill − ₹49 + discount |

## New cases (chat, push notifications)

| TC | Steps | Expected |
|---|---|---|
| 148 | Customer opens an active booking → **Chat** → sends "Ring gate 2" | Message on the right in blue; provider gets "New message from …" |
| 149 | Provider opens the job → **Chat with …** → replies | Customer sees it live on the left |
| 150 | Completed booking older than 7 days, or a cancelled one | No Chat button; old chat shows "Chat is closed" |
| 151 | First login on Android 13+ | "Allow notifications?" asked once |
| 152 | Close the app on phone B; on phone A accept / send a message | Phone B shows a push in the notification bar; tapping opens the app |
| 153 | Log in to a second account on the same phone | Pushes for the first account no longer arrive on this phone |

## New cases (area fallback, push taps, change email, layout)

| TC | Steps | Expected |
|---|---|---|
| 154 | Customer whose area has no provider of that category opens the provider list | All areas shown, with "No professionals in … yet – showing all areas" |
| 155 | App closed; tap a "New booking request" push (provider) | App opens straight on that **Job details** |
| 156 | Tap a "New message" push / alert | Opens the **Chat** of that booking |
| 157 | Tap "Booking accepted" push (customer) while the app is open | Opens that **Booking details** |
| 158 | Profile → **Change email** → new email + current password → Send link | "Link sent"; after opening the email link and logging in with the new email, Profile shows it |
| 159 | Change email with a wrong password / an email already used | Clear error, nothing changes |
| 160 | Provider job screen (Accepted) | PIN box and Start button right under the status steps |
| 161 | Customer Home → tap the **Plumber** tile | Book a service form opens with Plumber already chosen |
| 162 | Provider Complete popup: add items totalling over ₹50,000 (e.g. 30,000 + 25,000) | Red note 'All extras together can be at most ₹50,000'; Complete disabled. Max 10 items. |
| 163 | Admin has FIRST50 (min 199) and BIG200 (min 999); customer reviews a ₹348 booking | 'Offers for you': FIRST50 first with BEST OFFER and Apply; BIG200 greyed 'Add ₹651 more to use'; booking form shows '1 offer available' |
