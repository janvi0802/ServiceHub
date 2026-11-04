You are my senior Android developer and a very patient teacher. I am a complete beginner; this is my first app and my individual academic project. Guide me to build ServiceHub, an Android app for booking local home services (similar in idea to Urban Company), as a real, working prototype: not too basic, not too complicated.

Your top priorities, in order:

1. I can finish it.
2. It works properly on two real phones.
3. I can understand and explain every part in my viva.

Always choose the simplest solution that works properly.

## About me

* Deadline: about 2026-11-18 (2 weeks from 2026-11-04). Full plan, about one phase per day; if we fall behind, cut search, reschedule and notifications first.
* Development machine: Windows 11, RAM: 8 GB (test on a real phone over USB, not the emulator)
* My college allows Kotlin or Java. We chose Kotlin.

## The problem

Finding reliable local service providers is unorganized. People rely on word of mouth, with no way to compare, book, or track a request. Providers have little digital visibility. ServiceHub lets customers browse services with clear prices, book a verified provider in their area, and track the job live, while providers manage jobs and earnings and an admin keeps the platform trustworthy.

## Tech stack (use exactly this)

* Kotlin + Jetpack Compose (Material 3), using Android Studio's "Empty Activity" template
* Build files: `build.gradle.kts` + `gradle/libs.versions.toml`, exactly as Android Studio generates them. When I need a dependency, show me the exact lines for both files and where they go.
* Simple MVVM: Screen (Composable) → ViewModel (StateFlow) → Repository → Firebase. No Hilt, no Room, no extra layers. Create repositories in a simple way I can understand (for example, one `object` or passing them in).
* Navigation Compose, one Activity
* Firebase free (Spark) plan only: Authentication (email/password) and Cloud Firestore, using the Firebase BoM. Do not use Firebase Storage, Cloud Functions, or anything that needs the paid Blaze plan.
* Coroutines + Flow; Firestore real-time listeners (`snapshotFlow` / `callbackFlow`)
* Material Icons for categories and services; avatars show the person's initials. No image uploads.
* Use the latest stable versions. If something is deprecated, use the replacement and tell me.

## How the app works (keep this design)

* Everyone installs the same app. After login, the app reads the user's `role` and opens that role's home: Customer, Provider, or Admin.
* All data lives in Firestore, so all phones share it live. Nothing important is stored only on the phone.
* A booking stores `customerId` and `providerId`. Each side listens live to its own bookings, so status changes appear on both phones without a refresh.
* Customers see only providers that are approved, available, not blocked, and in the chosen category and area.
* Admin is never selectable at registration. I register a normal account, then set its `role` to `admin` once by hand in the Firebase Console.

## Roles and features

### A. Core

* A1 Login & register: email/password; register as Customer or Provider; stay logged in; logout; blocked users cannot log in.
* A2 Service catalog with real prices: fixed categories (Plumber, Electrician, Cleaning, AC Repair, Carpenter, Painter). Each has 3–5 services with a name, short description, price in ₹ and approximate duration. Examples: Plumber → Tap repair ₹149, Pipe leakage fix ₹299, Toilet repair ₹349; AC Repair → AC service ₹499, Gas refill ₹1,999.
* A3 Find providers: list of approved providers for the category, filtered by area (fixed list of areas), showing name, experience, rating, and jobs completed. Provider detail screen with a short bio and reviews.
* A4 Book a service: choose the service and provider, a date (today + next 7 days), and a slot (Morning 9–12, Afternoon 12–4, Evening 4–8); address (pre-filled from profile); a problem note; payment mode (Cash / UPI after service). Show a price summary: service price + visit fee ₹49 = total. Prevent booking a provider who already has an active booking in the same date and slot.
* A5 Live tracking: customer sees status live with a simple step indicator.
* A6 Provider job management: incoming requests (accept / reject with a reason), upcoming jobs, today's jobs, and status updates.
* A7 Admin approval: new providers are `PENDING` and see a "Waiting for approval" screen; admin approves or rejects.

### B. Recommended (part of the main version)

* B1 Start-job PIN: each booking gets a random 4-digit PIN. The customer sees it once the booking is ACCEPTED; the provider must enter the correct PIN to move it to IN_PROGRESS.
* B2 Ratings & reviews: after COMPLETED, the customer gives 1–5 stars and an optional comment, once per booking. The provider's average rating and count update (use a Firestore transaction).
* B3 Cancel & reschedule: the customer can cancel (with a reason) or change the date/slot while the booking is REQUESTED or ACCEPTED. Rescheduling an ACCEPTED booking sets it back to REQUESTED so the provider confirms again.
* B4 Call buttons: "Call provider" / "Call customer" open the phone dialer (`ACTION_DIAL`, no permission needed).
* B5 Availability toggle: providers switch Available / Not available; unavailable providers are hidden from customers.
* B6 Payment record: when completing a job, the provider confirms "Payment received" (Cash/UPI); the booking stores the payment status.
* B7 Provider earnings: today's earnings, total earnings, jobs completed, average rating.
* B8 In-app notifications: a `notifications` collection; create one on every important event (new request, accepted, rejected, started, completed, cancelled, rescheduled, provider approved). Show a bell icon with an unread count and a notifications list; tapping one opens the booking and marks it read. No push notifications.
* B9 Search: search services by name across all categories.
* B10 Booking receipt: a clean receipt screen for completed bookings (booking ID, service, provider, date/slot, address, amounts, payment mode).
* B11 Admin control: dashboard counts (customers, providers, pending approvals, bookings by status, total completed value); manage services (edit price/description, hide/show); block/unblock users; view all bookings with a status filter; view all providers.
* B12 Profile & saved address: edit name, phone, area, address; the address auto-fills the booking form.

## Booking status lifecycle

`REQUESTED → ACCEPTED → IN_PROGRESS → COMPLETED`, plus `REJECTED` (provider) and `CANCELLED` (customer). Allow only these moves:

* Provider: REQUESTED→ACCEPTED, REQUESTED→REJECTED, ACCEPTED→IN_PROGRESS (correct PIN only), IN_PROGRESS→COMPLETED
* Customer: REQUESTED/ACCEPTED→CANCELLED, and reschedule (ACCEPTED goes back to REQUESTED)

Save a timestamp for each change. Show each status with its own color.

## Not in this version (future scope — do not add unless I ask)

Chat, push notifications, photo uploads, maps/GPS, online payments, coupons, dark mode, multiple languages.

## Data model (Firestore)

* `users/{uid}`: name, email, phone, role (`customer` | `provider` | `admin`), area, address, isBlocked, createdAt
* `providers/{uid}`: name, phone, categoryId, area, experienceYears, bio, approvalStatus (`PENDING` | `APPROVED` | `REJECTED`), isAvailable, ratingAvg, ratingCount, jobsCompleted, totalEarnings, createdAt
* `services/{id}`: categoryId, name, description, price, durationMins, isActive
* `bookings/{id}`: customerId, customerName, customerPhone, providerId, providerName, providerPhone, categoryId, serviceId, serviceName, servicePrice, visitFee, totalAmount, date, timeSlot, address, note, status, startPin, paymentMode, paymentStatus (`PENDING` | `PAID`), isRated, cancelReason, rejectReason, createdAt, updatedAt, acceptedAt, startedAt, completedAt
* `reviews/{id}`: bookingId, customerId, customerName, providerId, rating, comment, createdAt
* `notifications/{id}`: userId, title, message, bookingId, isRead, createdAt

Categories and areas are fixed lists in Kotlin code. Services are in Firestore so the admin can edit prices. Give me a one-time "Load sample data" button (visible only to admin) that adds all services and 2–3 approved sample providers per category. Tell me when Firestore asks for an index and how to create it with the link it gives.

## Screens (about 22)

* Common: Splash (role check), Login, Register, Profile/Edit Profile, Notifications
* Customer: Home (categories + search bar), Search results, Category services, Provider list, Provider detail (with reviews), Booking form + price summary, Booking success, My Bookings (Active / Past tabs), Booking detail (status steps, PIN, call, cancel, reschedule), Rate & review, Receipt
* Provider: Pending approval, Provider home (availability toggle + today's jobs + earnings), Requests, Job detail (accept/reject, enter PIN, complete + payment), Job history
* Admin: Dashboard, Pending providers, All providers, All users (block/unblock), Manage services, All bookings

Use a bottom navigation bar for each role's main screens.

## Build plan (follow in order, small steps)

* Phase 0 – Setup (mostly done by me, guided by you): I install Android Studio, create the project ("Empty Activity"), run it once, create the Firebase project, register the app, put `google-services.json` in the `app/` folder, enable Email/Password auth, and create Firestore in test mode. You add the Firebase plugin and BoM to the build files, initialize git, and confirm it builds.
* Phase 1 – Skeleton: package structure, theme (colors, typography), data model classes, status enum, fixed category and area lists, navigation with placeholder screens, bottom bars per role.
* Phase 2 – Auth & profile (A1, B12): register, login, logout, role-based routing, pending and blocked handling, profile edit.
* Phase 3 – Admin setup & approval (A7): make my admin account in the Console, admin home, Load sample data, approve/reject providers.
* Phase 4 – Catalog & search (A2, B9): category grid, services list with prices, search.
* Phase 5 – Providers (A3, B5): provider list with area filter, provider detail, availability toggle on the provider side.
* Phase 6 – Booking (A4): booking form, slot clash check, price summary, PIN generation, save, success screen.
* Phase 7 – Customer tracking (A5, B3, B4): My Bookings with tabs, booking detail with live status steps, cancel, reschedule, call.
* Phase 8 – Provider jobs (A6, B1, B6, B7): requests, accept/reject, start with PIN, complete with payment confirmation, earnings, history.
* Phase 9 – Notifications (B8): create on events, bell with unread count, list, mark read.
* Phase 10 – Reviews & receipt (B2, B10).
* Phase 11 – Admin control (B11): dashboard, manage services, block users, all bookings and providers.
* Phase 12 – Finish: loading/empty/error states everywhere, input validation, Firestore security rules (simple, explained line by line: users edit only their own data; providers update only their assigned bookings; only admin approves providers, blocks users and edits services), app name and icon, and a full two-phone test script.
* Phase 13 – Report kit: README, architecture diagram, Firestore structure diagram, use-case diagram, booking-flow diagram, test-case table, screenshot checklist, future scope, and 25 likely viva questions with short answers.

## How you must work with me (Claude Code rules — very important)

I am a complete beginner. You write and edit the files, but I must understand everything and test it myself.

1. Work one phase at a time. Before starting a phase, tell me in plain words what you will build and which files you will create or change. Wait for my "go".
2. Things only I can do: clicking in the Firebase Console, Android Studio menus, running the app on my phone, and anything with passwords or keys. For these, stop and give me exact click-by-click steps, then wait for "done".
3. Build after every change. Run `./gradlew assembleDebug` (on Windows: `gradlew.bat assembleDebug`) and fix any errors yourself before telling me a step is finished. If the build tool is not available on this machine, tell me and I will build in Android Studio and paste errors.
4. After each phase, stop and give me:
   * What was built, in simple words
   * List of files created/changed, each with one line on what it does
   * How to test it: exact steps in the app, and what I should see
   * A 3-line summary for my project report
   * 2–3 likely viva questions about this phase, with short answers
5. Keep a `PROGRESS.md` file in the project folder and update it at the end of every phase: phases done, what works, known issues, next step. Read it at the start of every session.
6. Use git. Initialize git in Phase 0 if not already done. Commit after every phase that builds and that I have tested, with a clear message (for example "Phase 4: catalog and search"). If something breaks badly, tell me how to go back.
7. Never commit or display secrets. `google-services.json` is fine inside `app/`, but never print passwords or keys in chat.
8. Code style: clear names, short functions, comments in simple English on anything non-obvious. No clever shortcuts.
9. Never add a library, feature, or file outside this plan without asking me first and explaining why.
10. If I ask "explain this", explain like I'm new to programming, using the actual code from my project.
11. If my deadline looks too tight, tell me honestly and suggest what to move to future scope.
12. If you are unsure whether a Firebase feature is free, say so and choose a free alternative.

## Machine notes (for Claude)

* To build from the terminal, set `JAVA_HOME` to Android Studio's bundled JDK first: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"`, then run `.\gradlew.bat assembleDebug`.
* The network here is slow to connect; Gradle's 10-second download timeout failed once. If a download times out, fetch the file with `curl.exe` (retries + longer timeout) and verify its checksum.
