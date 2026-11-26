# ServiceHub

An Android app for booking local home services (plumber, electrician, cleaning, AC repair,
carpenter, painter, and any category the admin adds). Customers see clear fixed prices, book a
verified professional in their area, and follow the job live. Providers manage requests, jobs,
payments and earnings. An admin approves providers and keeps the platform trustworthy.

Individual academic project · Kotlin · Jetpack Compose · Firebase (free Spark plan) · free Vercel server.

## Features

| Role | Features |
|---|---|
| **Customer** | Register / login / forgot password / change email · Home in sections: *Needs your attention* (pay, rate, no answer), next booking with start PIN, **Book a service** guided form (category → service → professional) · search · service details (included / not included) · provider list starting in your area (falls back to all areas), sort, *Available today*, favourites · booking form: date, slot (*Booked* / *On hold* / days off greyed), address (saved addresses), book for someone else, problem note, Cash/UPI · review page with **Offers for you** and coupon code · live status steps and **Next step** box · start PIN · cancel / reschedule · change payment method · **Pay now by UPI** (Razorpay test mode) · call and **chat** with the professional · rate & review · receipt · 7-day **redo guarantee** with redo PIN · report a problem · notifications + **push notifications** that open the right page · help |
| **Provider** | Register (waits for admin approval) · Home in sections: *Needs your attention* (requests, payments to collect, redo requests), next job, today's jobs, earnings (today / this week / to collect, weekly chart), availability switch and weekly days off · requests: accept / reject with reason · "On my way" · start job with the customer's PIN · complete with extra parts (all extras together max ₹50,000, up to 10 items) · **Cash collected** or automatic **UPI payment received** · redo with the customer's redo PIN · chat · job history · notifications + push |
| **Admin** | Dashboard (needs attention, counts, earnings split, charts) with badges · approve / reject providers · users: search, filter, block, reset-password link, **change password** (via the free server) · all bookings with filters (status, expired, redo, unpaid) and a read-only **booking detail** · services: add, edit price/description/included, hide · categories: add new (icon, colour) or hide · **coupons** (₹ or % off, minimum bill, cap, on/off) · complaints · one-time sample data |

## Tech stack

- Kotlin, Jetpack Compose (Material 3), Navigation Compose, one Activity
- MVVM: Screen (Composable) → ViewModel (StateFlow) → Repository (`object`) → Firebase
- Firebase Authentication (email/password), Cloud Firestore with live listeners (`callbackFlow`) and transactions
- Firebase Cloud Messaging (push notifications, free)
- Razorpay Checkout in **test mode** (no real money)
- A tiny **free Vercel server** (`admin-server/`, Firebase Admin SDK) for two things a phone app must not do itself:
  setting another user's password, and sending push notifications
- No paid Firebase features (no Storage, no Cloud Functions, no Blaze plan)

## Project structure

```
app/src/main/java/com/example/servicehub/
├── MainActivity.kt          the only Activity (also receives Razorpay results and push taps)
├── navigation/              Routes, AppNavHost (all screens + role-based start), bottom bars
├── data/
│   ├── model/               User, Provider, Service, Booking, Review, Coupon, ChatMessage, SlotInfo, BookingStatus …
│   ├── repository/          Auth, User, Service, Provider, Booking, Review, Notification, Admin,
│   │                        Complaint, Coupon, Chat
│   ├── FixedData.kt         areas, time slots, visit fee, default categories
│   ├── CategoryStore.kt     built-in + admin-added categories, live
│   ├── SampleData.kt        sample services and providers
│   ├── DateUtils.kt         date helpers
│   └── FirestoreFlows.kt    Firestore listener -> Flow helpers
├── payment/                 RazorpayPayments (UPI test payments)
├── push/                    PushService (FCM), Push (send / show), PushLink (open the right page)
└── ui/
    ├── auth, splash, profile         login, register, splash, profile (change email)
    ├── customer/                     home, book a service, catalog, providers, booking form, my bookings, booking detail, rating, favourites
    ├── provider/                     home, requests, job detail, history, pending approval
    ├── admin/                        dashboard, approvals, users, bookings (+ detail), services, categories, coupons, complaints
    ├── booking/                      shared booking ViewModel, info card, Next step box
    ├── common/                       notifications, chat, receipt, help
    └── components/, theme/           reusable UI (cards, sections, forms), colours and fonts
admin-server/api/            set-password.js, send-push.js (Vercel)
firestore.rules              security rules (commented line by line)
app/src/test/                LogicTest.kt — 50 automatic logic tests
```

A file-by-file guide with each workflow traced through the code is in [docs/CODE_GUIDE.md](docs/CODE_GUIDE.md).

## How to run

1. Open the folder in Android Studio (**File → Open → ServiceHub**) and let Gradle sync.
2. `app/google-services.json` must belong to your Firebase project (Email/Password sign-in on, Firestore created).
3. Optional keys in `local.properties` (never uploaded):
   `RAZORPAY_KEY_ID=rzp_test_…` (UPI test payments) and `ADMIN_API_URL=https://…vercel.app` (password change + push).
   Without them the app still works; those buttons are shown disabled.
4. Publish [`firestore.rules`](firestore.rules): Firebase Console → Firestore → **Rules** → paste → **Publish**.
5. Connect a phone with USB debugging and press ▶ **Run**.
6. Register an account, then in the Firebase Console set its `users/{uid}.role` to `admin`. Log in as admin → **Load sample data**.

Automatic tests: `gradlew.bat testDebugUnitTest`. Server setup: [admin-server/README.md](admin-server/README.md).

## Security rules in short

Users edit only their own profile (never role or blocked status). Only the admin approves providers,
blocks users, edits services, categories and coupons. A booking is readable only by its customer, its
provider and the admin, and each side may change only its own fields (a customer can never mark a job
completed or cash collected). Slots stop double booking. Earnings can only go up with a completed job;
ratings stay 1–5, once per booking. Chat and notifications are limited to the people on that booking.
Blocked users cannot write anything.

## What this repository contains

| Folder / file | What it is |
|---|---|
| `app/` | The Android app: Kotlin source code (`src/main/java`), screens, resources, and 50 logic tests (`src/test`) |
| `admin-server/` | Small free Vercel server (Node.js): admin password change and push notifications |
| `firestore.rules` | Firebase security rules (who can read / change what) |
| `docs/PROJECT_REPORT.pdf` / `.docx` | Project report (college format) |
| `docs/report_assets/` | Screenshots, college logo and diagrams used in the report (ER, activity, use case, status flow) |
| `docs/CODE_GUIDE.md` | How to read the code: every file and every workflow |
| `docs/REPORT_KIT.md` | Diagrams, future scope and viva questions |
| `docs/TEST_SCRIPT.md` | Two-phone test script (163 test cases) |
| `docs/GIT_HISTORY.md`, `docs/GITHUB_HISTORY.pdf` | Commit and push history (proof of progress) |
| `HOW_TO_TEST.md` | Beginner guide to install, run and test the app |
| `PROGRESS.md` | What was built in each phase |
| `build.gradle.kts`, `gradle/`, `gradlew*` | Build setup (Gradle) |

Secret keys (`local.properties`, signing keys, the Firebase service account) are **not** in this repository.

## Documentation

- [docs/PROJECT_REPORT.pdf](docs/PROJECT_REPORT.pdf) ([Word version](docs/PROJECT_REPORT.docx)) – the project report (college format)
- [docs/CODE_GUIDE.md](docs/CODE_GUIDE.md) – how to read the code: every file and every workflow
- [docs/REPORT_KIT.md](docs/REPORT_KIT.md) – diagrams, booking flow, future scope, viva questions
- [docs/TEST_SCRIPT.md](docs/TEST_SCRIPT.md) – two-phone test script (163 cases)
- [docs/GIT_HISTORY.md](docs/GIT_HISTORY.md) – commit history as proof of progress
- [HOW_TO_TEST.md](HOW_TO_TEST.md) – beginner guide: install, run, test
- [PROGRESS.md](PROGRESS.md) – what was built in each phase and round
