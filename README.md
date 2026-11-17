# ServiceHub

An Android app for booking local home services (plumber, electrician, cleaning, AC repair,
carpenter, painter). Customers browse services with clear prices, book a verified provider in
their area and track the job live. Providers manage requests, jobs and earnings. An admin
approves providers and keeps the platform trustworthy.

Individual academic project · Kotlin · Jetpack Compose · Firebase (free Spark plan).

## Features

| Role | Features |
|---|---|
| Customer | Register/login, categories, search, service prices, provider list with area filter, provider profile with reviews, booking form (date, slot, address, note, Cash/UPI, price summary), live status tracking, start PIN, cancel, reschedule, call provider, rate & review, receipt, notifications, profile with saved address |
| Provider | Register (waits for admin approval), availability switch, new requests (accept/reject with reason), start job with customer's PIN, complete with payment confirmation, today's jobs, earnings, job history, notifications |
| Admin | Dashboard counts, approve/reject providers, block/unblock users, edit service prices and descriptions, hide/show services, all bookings with status filter, all providers, one-time sample data |

## Tech stack

- Kotlin, Jetpack Compose (Material 3), Navigation Compose, one Activity
- MVVM: Screen (Composable) → ViewModel (StateFlow) → Repository (`object`) → Firebase
- Firebase Authentication (email/password) and Cloud Firestore, real-time listeners via `callbackFlow`
- Firestore transactions for every booking status change and for ratings
- No paid Firebase features (no Storage, no Cloud Functions)

## Project structure

```
app/src/main/java/com/example/servicehub/
├── MainActivity.kt            the only Activity
├── navigation/                routes, NavHost, bottom bars per role
├── data/
│   ├── model/                 User, Provider, Service, Booking, Review, AppNotification, BookingStatus
│   ├── repository/            Auth, User, Service, Provider, Booking, Review, Notification, Admin
│   ├── FixedData.kt           categories, areas, time slots, visit fee
│   ├── SampleData.kt          sample services and providers
│   ├── DateUtils.kt           date helpers
│   └── FirestoreFlows.kt      live Firestore -> Flow helpers
└── ui/
    ├── auth, splash, profile  login, register, splash, profile
    ├── customer/              home, catalog, providers, booking, my bookings, rating
    ├── provider/              home, jobs, job detail, history, pending approval
    ├── admin/                 dashboard and control screens
    ├── booking/, common/      shared booking ViewModel, notifications, receipt
    └── components/, theme/    reusable UI pieces, colors
```

## How to run

1. Open the folder in Android Studio and let Gradle sync.
2. `app/google-services.json` must belong to your Firebase project
   (Email/Password sign-in enabled, Firestore created).
3. Connect a phone with USB debugging and press ▶ Run.
4. Register an account, then in the Firebase Console set its `users/{uid}.role` to `admin`.
5. Log in as admin and press **Load sample data**.

Automatic logic tests: `gradlew.bat testDebugUnitTest`

## Security rules

The rules are in [`firestore.rules`](firestore.rules), with a comment on every rule.
To apply them: Firebase Console → Firestore Database → **Rules** → paste the file → **Publish**.

In short: users can edit only their own profile (never their role or blocked status);
only the admin approves providers, blocks users and edits services; only a booking's own
customer or assigned provider can change it; reviews are once per booking and cannot be edited;
users read only their own notifications; blocked users cannot write anything.

## Documentation

- [docs/REPORT_KIT.md](docs/REPORT_KIT.md) – diagrams, booking flow, future scope, viva questions
- [docs/TEST_SCRIPT.md](docs/TEST_SCRIPT.md) – two-phone test script and test-case table
- [PROGRESS.md](PROGRESS.md) – build progress per phase
