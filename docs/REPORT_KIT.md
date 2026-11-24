# ServiceHub – Report Kit

Diagrams are written in **Mermaid**. GitHub shows them as pictures automatically.
For your report: open this file on GitHub (or paste a diagram into https://mermaid.live),
then take a screenshot or export PNG.

---

## 1. Problem and solution (for the introduction)

Finding reliable local service providers is unorganized: people rely on word of mouth, cannot
compare prices, and cannot track a request. Providers have little digital visibility.
ServiceHub lets customers browse services with fixed prices, book an admin-verified provider in
their area and follow the job live, while providers manage requests and earnings and an admin
keeps the platform trustworthy.

## 2. Architecture (MVVM)

```mermaid
flowchart LR
    subgraph Phone["Android app (one Activity)"]
        UI["Screens<br/>(Jetpack Compose)"] -->|user actions| VM["ViewModels<br/>(StateFlow)"]
        VM -->|state| UI
        VM --> REPO["Repositories<br/>Auth · User · Service · Provider<br/>Booking · Review · Notification · Admin"]
    end
    REPO -->|login / logout| AUTH["Firebase Authentication"]
    REPO -->|read, write, transactions| FS[("Cloud Firestore")]
    FS -->|live updates (snapshot listeners)| REPO
    RULES["Security rules"] -.protect.- FS
```

**Explanation:** a screen only draws the state it gets from its ViewModel. The ViewModel holds
the screen's data in a StateFlow and calls a repository. Repositories are the only code that
talks to Firebase. Firestore pushes every change to all listening phones, so both phones stay in
sync without a refresh.

## 3. Firestore structure

```mermaid
erDiagram
    USERS ||--o| PROVIDERS : "same uid if role = provider"
    USERS ||--o{ BOOKINGS : "customerId"
    PROVIDERS ||--o{ BOOKINGS : "providerId"
    SERVICES ||--o{ BOOKINGS : "serviceId"
    CATEGORIES ||--o{ SERVICES : "categoryId"
    CATEGORIES ||--o{ PROVIDERS : "categoryId"
    BOOKINGS ||--o| REVIEWS : "review id = booking id"
    BOOKINGS ||--o| SLOTS : "active booking holds one slot"
    BOOKINGS ||--o{ COMPLAINTS : "reported problems"
    PROVIDERS ||--o{ REVIEWS : "providerId"
    USERS ||--o{ NOTIFICATIONS : "userId"

    USERS {
        string id PK
        string name
        string email
        string phone
        string role "customer | provider | admin"
        string area
        string address
        bool isBlocked
        long createdAt
    }
    PROVIDERS {
        string id PK "= user uid"
        string categoryId
        string area
        int experienceYears
        string bio
        string approvalStatus "PENDING | APPROVED | REJECTED"
        bool isAvailable
        double ratingAvg
        int ratingCount
        int jobsCompleted
        int totalEarnings
    }
    SERVICES {
        string id PK
        string categoryId
        string name
        string description
        int price
        int durationMins
        bool isActive
    }
    BOOKINGS {
        string id PK
        string customerId FK
        string providerId FK
        string serviceId FK
        int servicePrice
        int visitFee
        int totalAmount
        string date "yyyy-MM-dd"
        string timeSlot
        string status
        string startPin
        string paymentMode "Cash | UPI"
        string paymentStatus "PENDING | PAID"
        bool isRated
    }
    REVIEWS {
        string id PK "= bookingId"
        string providerId FK
        string customerId FK
        int rating "1-5"
        string comment
    }
    NOTIFICATIONS {
        string id PK
        string userId FK
        string title
        string message
        string bookingId
        bool isRead
    }
    COMPLAINTS {
        string id PK
        string bookingId FK
        string customerId FK
        string providerId FK
        string message
        string status "OPEN | RESOLVED"
    }
    CATEGORIES {
        string id PK "built-in (plumber...) or new"
        string name
        string iconKey "yoga, beauty, tutor..."
        long colorHex
        bool isActive
        int order
    }
    SLOTS {
        string id PK "providerId_date_slotNumber"
        string providerId FK
        string customerId FK
        string bookingId FK
        string date
        string timeSlot
    }
```

**Slots:** each active booking owns one `slots` document whose id is
`providerId_date_slotNumber`. Only one document can have that id, so only one booking can hold
a provider's slot. It is created in the same transaction as the booking and deleted when the
booking is rejected, cancelled or completed (or moved when rescheduled). It contains no address
or phone numbers, so bookings themselves stay private.

Areas and time slots are fixed lists in Kotlin code (`FixedData.kt`). Categories start with 6
built-in defaults in code; the admin can add new ones (e.g. Yoga Teacher) or hide any, stored in
`categories` and merged live by `CategoryStore`, so the platform can grow without an app update.
Names and phones are copied into each booking, so a booking still shows correct details even if
someone later edits their profile.

## 4. Use-case diagram

```mermaid
flowchart LR
    C(("Customer"))
    P(("Provider"))
    A(("Admin"))

    subgraph ServiceHub
        UC1["Register / Login / Profile"]
        UC2["Browse & search services"]
        UC3["Find provider by area"]
        UC4["Book a service"]
        UC5["Track booking live"]
        UC6["Cancel / Reschedule"]
        UC7["Rate & review, view receipt"]
        UC8["Accept / Reject request"]
        UC9["Start job with PIN"]
        UC10["Complete job & record payment"]
        UC11["Set availability, view earnings"]
        UC12["Approve / reject providers"]
        UC13["Block / unblock users"]
        UC14["Manage services & prices"]
        UC15["View dashboard & all bookings"]
        UC16["View notifications"]
    end

    C --- UC1 & UC2 & UC3 & UC4 & UC5 & UC6 & UC7 & UC16
    P --- UC1 & UC8 & UC9 & UC10 & UC11 & UC16
    A --- UC12 & UC13 & UC14 & UC15
```

## 5. Booking status flow

```mermaid
stateDiagram-v2
    [*] --> REQUESTED : customer books
    REQUESTED --> ACCEPTED : provider accepts
    REQUESTED --> REJECTED : provider rejects (reason)
    REQUESTED --> CANCELLED : customer cancels (reason)
    ACCEPTED --> IN_PROGRESS : provider enters correct PIN
    ACCEPTED --> CANCELLED : customer or provider cancels (reason)
    ACCEPTED --> REQUESTED : customer reschedules
    REQUESTED --> REQUESTED : customer reschedules
    IN_PROGRESS --> COMPLETED : provider confirms payment received
    COMPLETED --> [*] : customer rates (once)
    REJECTED --> [*]
    CANCELLED --> [*]
```

Every move happens inside a **Firestore transaction** (`BookingRepository.changeBooking`): it
reads the current status and refuses any move that is not in the diagram.

**Expired:** a booking still REQUESTED after its date/slot has passed is shown as "Expired"
(grey), moves to Past/History and can no longer be accepted. The customer can reschedule,
cancel, or find another provider.

## 6. Booking sequence (two phones)

```mermaid
sequenceDiagram
    actor C as Customer (Phone A)
    participant F as Firestore
    actor P as Provider (Phone B)

    C->>F: create booking (REQUESTED, random 4-digit PIN)
    F-->>P: live update + notification "New booking request"
    P->>F: accept (transaction)
    F-->>C: live update: ACCEPTED, PIN visible
    Note over C,P: provider arrives, customer tells the PIN
    P->>F: start with PIN (transaction checks PIN)
    F-->>C: live update: IN_PROGRESS
    P->>F: complete + payment received (transaction also adds earnings)
    F-->>C: live update: COMPLETED + notification
    C->>F: rate 1-5 stars (transaction updates provider average)
```

## 7. Screenshot checklist

Take these on a real phone (Power + Volume down):

- [ ] Splash · Login · Register (customer) · Register (provider fields)
- [ ] Waiting for approval · Admin approvals list
- [ ] Customer Home (categories) · Search results · Category services
- [ ] Provider list with area chips · Provider detail with reviews
- [ ] Booking form with price summary · Booking success
- [ ] My Bookings (Active / Past) · Booking detail: Requested, Accepted with PIN, Completed
- [ ] Reschedule dialog · Cancel dialog
- [ ] Provider Home (availability + earnings) · Jobs (requests) · Job detail: PIN entry, Complete dialog
- [ ] Notifications with unread badge
- [ ] Rate & review · Receipt
- [ ] Admin dashboard · Users (block) · Manage services (edit price) · All bookings filter
- [ ] Firebase Console: Firestore collections · Authentication users list · Rules page

## 8. Future scope

- Push notifications (Firebase Cloud Messaging)
- Online payments (UPI intent / payment gateway)
- In-app chat between customer and provider
- Photos of the problem and provider ID verification documents
- Maps and GPS: show distance, live provider location
- Email verification at registration and phone OTP login
- Admin sets a new password for a user directly: needs the Firebase Admin SDK on a server
  (a Cloud Function on the Blaze plan) that checks the caller is the admin, sets the password,
  signs the user out everywhere and emails them. Today the admin sends a secure reset link instead,
  so nobody (not even the admin) ever knows another person's password.
- Server-side checks with Cloud Functions (needs the paid Blaze plan)
- Dark mode, Hindi/Gujarati languages, coupons and offers
- Provider working hours and custom slots

## 8b. What makes ServiceHub different (for the "unique features" section)

- **Race-proof booking:** a slot is claimed in the same transaction as the booking, so two
  customers can never get the same provider at the same time.
- **Start PIN:** the job can only start when the provider is really at the customer's home.
- **Live timeline with real times** (requested, accepted, started, completed) on both phones.
- **Trust signals:** admin-verified providers, "Top rated" only with 4.5★ from 3+ reviews,
  one review per completed booking.
- **Fair handling of no-shows:** unanswered requests expire automatically, late jobs show as
  "Overdue", and either side can cancel with a reason that the other side sees.
- **Transparent prices:** fixed service price + ₹49 visit fee shown before booking, a clear
  "spare parts are extra" note, receipt after, shareable on WhatsApp/SMS.
- **No trial and error:** taken slots and the provider's weekly days off are greyed out before
  the customer chooses; "⚡ Available today" finds someone for urgent jobs.
- **Weekly days off designed for urgency:** a day off belongs to one provider and only blocks
  NEW bookings; other providers stay bookable, and existing jobs are never cancelled silently.
- **7-day service guarantee:** if the problem returns within 7 days, the customer asks for a free
  revisit in one tap; the provider sees it with a badge and marks it done.
- **Honest billing:** spare parts are added as line items at completion, so the receipt shows
  exactly what was paid; the ₹49 visit fee is ServiceHub's platform income, providers keep the rest.
- **"On my way":** the customer knows the professional is coming before the PIN step.
- **Complaints with admin follow-up:** customers can report a problem on any booking; the admin
  calls both sides, marks it resolved, and the customer is notified.

## 8c. User analysis: why people would (and would not) use it

| Person | Why they would use it | Why they might not (and future fix) |
|---|---|---|
| Customer | Fixed prices, verified providers, PIN safety, live timeline, free-slot view, receipts, complaints | Must register before browsing; alerts only inside the app (push = future) |
| Provider | Free customers in their area, rating builds reputation, earnings, days off, Open in Maps | Can miss requests when the app is closed (push = future); prices are set by the platform |
| Admin | Approvals, blocking, prices and new services, complaints, live dashboard and top providers | No ID document checks (photo upload = future) |

## 9. Likely viva questions (25) with short answers

1. **What problem does ServiceHub solve?** It replaces word-of-mouth with one app where customers see fixed prices, book an admin-verified provider in their area and track the job live.
2. **Why Kotlin and Jetpack Compose?** Kotlin is Google's preferred Android language (null safety, short code). Compose builds UI with Kotlin functions instead of XML and redraws automatically when state changes.
3. **Explain your architecture.** MVVM: Composable screen → ViewModel (holds StateFlow state) → Repository (talks to Firebase) → Firebase. Each layer has one job, so code is easier to test and change.
4. **What is a ViewModel?** A class that keeps a screen's data and logic. It survives screen rotation, so data is not lost, and the screen only displays its state.
5. **What is StateFlow?** A Kotlin Flow that always holds the latest value. The screen collects it and redraws whenever it changes.
6. **Why Firebase?** It gives login and a real-time cloud database without writing a server, and the Spark plan is free.
7. **Why Firestore and not Realtime Database?** Firestore has better queries, document/collection structure, transactions and clearer security rules.
8. **How do two phones stay in sync?** Each screen listens with a Firestore snapshot listener (wrapped in `callbackFlow`). When one phone writes, Firestore pushes the change to every listening phone in about a second.
9. **What is `callbackFlow`?** It converts a callback-based listener into a Kotlin Flow; `awaitClose` removes the listener when the screen no longer needs it.
10. **How does "stay logged in" work, and what if a user forgets the password?** Firebase Auth stores the session on the phone; the Splash screen checks `currentUser` and opens the right home. "Forgot password?" calls `sendPasswordResetEmail`, and Firebase emails a reset link (free, no server code).
11. **How are roles handled?** `users/{uid}.role` is customer, provider or admin. After login the app reads it and opens the matching home with its own bottom bar.
12. **Why can't someone register as admin?** Admin is not offered at registration and the security rules refuse role "admin" on create. The admin role is set by hand in the Firebase Console.
13. **How does provider approval work?** New providers are saved with `approvalStatus = PENDING` and see a waiting screen. The admin approves or rejects; only approved, available, unblocked providers are shown to customers.
14. **How do you stop double booking?** Each active booking claims a `slots` document with id `providerId_date_slotNumber`. The claim and the booking are saved in one transaction that first checks the slot document does not exist. If two customers confirm at the same moment, only one transaction succeeds; the other gets "already booked". The slot is freed when the booking is rejected, cancelled or completed.
15. **What is a Firestore transaction and where do you use it?** A group of reads and writes that succeeds or fails together, retrying if data changed meanwhile. Used for every status change, for completing a job (booking + earnings) and for ratings (review + average + isRated).
16. **Why the start PIN?** It proves the provider is actually at the customer's home before starting. The customer sees it only after acceptance; the job starts only with the correct PIN.
17. **How is the average rating calculated?** newAverage = (oldAverage × oldCount + newRating) / (oldCount + 1), inside a transaction so two reviews at once cannot give a wrong result.
18. **How do you prevent rating twice?** The review's document id is the booking id (only one can exist) and the booking gets `isRated = true`; the transaction refuses if either is already set.
19. **How do notifications work without push?** Important events write a document in `notifications` for the other person. Their app listens live and shows an unread badge; tapping marks it read.
20. **What happens when the admin blocks a user?** `isBlocked = true`: they are logged out immediately (the app listens to their own document), cannot log in again, blocked providers are hidden, and security rules refuse their writes.
21. **What do your security rules do?** Users edit only their own profile (never role or blocked status), only admin approves, blocks and edits services, bookings are readable and changeable only by their own customer, provider or admin, reviews are once and cannot be edited, notifications are private, blocked users cannot write.
22. **Why store customer and provider names inside the booking?** So a booking and receipt always show correct details without extra reads, even if someone changes their profile later (denormalization).
23. **How did you test the app?** 29 automatic logic tests (JUnit), Android lint (0 errors), a release build check, and a 70-case manual test script on two real phones.
24. **What are the limitations?** No online payment, no push notifications, no GPS, no photos; business rules are checked in the app and by security rules, but there is no server code (Cloud Functions need the paid plan).
25. **What would you add next?** Push notifications, UPI payment, chat, maps, provider ID verification, and Cloud Functions for server-side checks.
