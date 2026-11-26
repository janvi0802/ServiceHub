# ServiceHub – Code Guide (for reading and explaining the code)

This guide answers two questions: **"How do I look at the code?"** and **"Which files run when
I tap something?"** Every workflow below is traced from the screen to Firebase, file by file.

---

## 1. Opening the code in Android Studio

1. Start Android Studio → **File → Open…** → choose the `ServiceHub` folder → **OK** (Trust project if asked).
2. On the left, the **Project** panel. At the top of it, switch the view from *Android* to **Project** if you want to see every folder exactly as on disk (`admin-server`, `docs`, `firestore.rules`).
3. The app's code is in **`app → src → main → java → com.example.servicehub`**.
4. Useful shortcuts:
   - **Ctrl + Shift + N** – open a file by name (type `BookingRepository`).
   - **Ctrl + click** on any function name – jump to where it is written.
   - **Alt + F7** – find everywhere a function is used.
   - **Ctrl + F12** – list of functions in the open file.
   - **Ctrl + Shift + F** – search text in the whole project.
5. Every file starts with a comment that says what it does, and important functions have a comment above them.

**Where to start reading (in this order):**
`MainActivity.kt` → `navigation/AppNavHost.kt` → `data/model/Models.kt` → `data/repository/BookingRepository.kt`
→ one screen, e.g. `ui/customer/BookingFormScreen.kt`.

---

## 2. The pattern every feature follows (MVVM)

```
Screen (Composable)        draws the state, sends taps to the ViewModel
   ↓ calls                       e.g. BookingFormScreen.kt
ViewModel (StateFlow)      holds the screen's data, checks input, calls the repository
   ↓ calls                       e.g. BookingFormViewModel (same file)
Repository (object)        the ONLY code that talks to Firebase
   ↓ reads / writes              e.g. BookingRepository.createBooking()
Firebase (Firestore, Auth)  stores the data; live listeners push changes back up
```
Security: `firestore.rules` runs on Google's servers and refuses anything not allowed, even from a modified app.

---

## 3. File map

| Folder / file | What it does |
|---|---|
| `MainActivity.kt` | The only Activity. Starts the category list, receives Razorpay payment results and push taps, shows `AppNavHost`. |
| `navigation/Routes.kt` | Names of all screens (e.g. `booking_detail/{bookingId}`). |
| `navigation/AppNavHost.kt` | Every screen and how to move between them; bottom bar per role; red badges; blocks/approval watch; push permission and "open the right page". |
| `navigation/BottomBar.kt` | Tabs for customer, provider, admin. |
| `data/model/Models.kt` | All data classes (User, Provider, Service, Booking, Coupon, ChatMessage, SlotInfo…) and small pure rules (coupon discount, slot on hold, no answer in 1 hour, redo allowed). |
| `data/model/BookingStatus.kt` | REQUESTED, ACCEPTED, IN_PROGRESS, COMPLETED, REJECTED, CANCELLED with labels and colours. |
| `data/FixedData.kt` | Areas, time slots, visit fee ₹49, default categories, payment modes. |
| `data/CategoryStore.kt` | Built-in + admin-added categories, kept live for the whole app. |
| `data/FirestoreFlows.kt` | `liveList` / `liveObject`: turn Firestore listeners into Kotlin Flows (`callbackFlow`). |
| `data/repository/AuthRepository.kt` | Register, login, logout, reset link, change email. |
| `data/repository/UserRepository.kt` | Read/update users and providers, saved addresses, favourites. |
| `data/repository/ServiceRepository.kt` | Services (catalog). |
| `data/repository/ProviderRepository.kt` | Bookable providers, availability, days off, reviews. |
| `data/repository/BookingRepository.kt` | **The heart:** create booking (slot + coupon), accept, reject, on my way, start (PIN), complete (extras, earnings), cash collected, UPI paid, cancel, reschedule, redo, change payment method, no-answer notice. All in transactions. |
| `data/repository/ReviewRepository.kt` | Rate once, update the average in a transaction. |
| `data/repository/NotificationRepository.kt` | Write an in-app notification, then ask for a push. |
| `data/repository/ChatRepository.kt` | Chat messages per booking. |
| `data/repository/CouponRepository.kt` | Coupons: check a code, list active offers, admin save / on-off. |
| `data/repository/ComplaintRepository.kt` | "Report a problem" and admin resolve. |
| `data/repository/AdminRepository.kt` | Approvals, block, services, categories, sample data, change password via the server. |
| `payment/RazorpayPayments.kt` | Opens Razorpay (test mode) and saves the payment on the booking. |
| `push/PushService.kt` | Push: save the phone's token, ask the server to send, show it, open the right page when tapped. |
| `ui/...` | One file per screen (or group of screens) with its ViewModel. |
| `admin-server/api/set-password.js` | Vercel: admin sets a new password (checks the caller is admin). |
| `admin-server/api/send-push.js` | Vercel: sends a push (checks the sender is on that booking). |
| `firestore.rules` | Who may read / write what. |
| `app/src/test/.../LogicTest.kt` | 50 automatic tests of the pure rules. |

---

## 4. Workflows traced through the files

### 4.1 Start, login and role
1. `MainActivity.onCreate` → `AppNavHost` → `SplashScreen` (`ui/splash`).
2. `SplashViewModel` asks `AuthRepository.currentUserId` and `UserRepository.getUser` for the **role**.
3. Customer → `CUSTOMER_HOME`, provider → `PROVIDER_HOME` (or `PENDING_APPROVAL` if not approved), admin → `ADMIN_DASHBOARD`. Blocked → logged out.
4. `AppNavHost` keeps watching the user: if the admin blocks them, they are logged out at once.

### 4.2 Book a service (customer)
1. **Home** (`CustomerHomeScreen.kt`) → *Book a service* or a category tile → `BookServiceScreen.kt` (choose category and service).
2. *Choose professional* → `ProviderListScreen` (`customer/ProviderScreens.kt`) → `ProviderRepository.bookableProviders` (approved, available, not blocked); starts in your area, or all areas if nobody is there.
3. *Book* → `BookingFormScreen.kt`. `BookingFormViewModel.busySlots` listens to `BookingRepository.busySlots` → slots show *Booked* (accepted) or *On hold* (requested < 1 hour).
4. *Review booking* → summary; **Offers for you** comes from `CouponRepository.active()` ranked by `rankOffers` (Models.kt); *Apply* → `CouponRepository.check`.
5. *Confirm booking* → `BookingRepository.createBooking`: re-reads provider, service and coupon, then **one transaction** claims the `slots/{provider_date_slot}` document and saves the booking (REQUESTED, random start PIN). → `NotificationRepository.send` to the provider → push.

### 4.3 Provider answers
1. Provider's **Home / Requests** (`provider/ProviderScreens.kt`) read `BookingRepository.providerBookings` (live); sorted by date and slot order (`ProviderJobsViewModel.timeOrder`).
2. **Job details** (`JobDetailScreen.kt`) → *Accept* → `BookingRepository.accept` (transaction: still REQUESTED, not expired, slot not taken by someone else) → slot becomes `accepted = true` (*Booked*). *Reject* → reason → slot freed.
3. **No answer in 1 hour** (`isNoAnswerAt` in Models.kt): the hold ends (`heldUntil`), others may book that slot; the customer's app sends them a "No answer" notice once (`notifyNoAnswer`) with *Other provider / Change time*.

### 4.4 Doing the job
1. *I'm on my way* → `markOnTheWay` (status stays ACCEPTED, customer is told).
2. Customer sees the **start PIN** (booking detail and Home). Provider types it → `startJob` checks it in a transaction → IN_PROGRESS.
3. *Complete job* → popup for extra parts (each ≥ ₹1, all together ≤ ₹50,000, max 10) → `BookingRepository.complete`: one transaction sets COMPLETED, new total, frees the slot and adds +1 job and the provider's share (total − ₹49 visit fee + coupon) to their totals.

### 4.5 Payment (after completion)
- **Cash:** provider taps *Cash collected* → `markCashCollected` → PAID, customer notified.
- **UPI:** customer taps *Pay now* (`BookingDetailScreen.kt`) → `RazorpayPayments.start` → Razorpay → `MainActivity.onPaymentSuccess` → `RazorpayPayments.onSuccess` → `BookingRepository.markPaidOnline` (PAID + payment ID) → provider sees *UPI payment received* live and gets a push.
- Before paying, the customer can switch Cash ↔ UPI (`changePaymentMode`). After the job both ways always work.
- Earnings on provider Home count **paid** jobs only (`ProviderJobsViewModel.todaysEarnings`, `earningsLast7Days`); unpaid are under *To collect*.

### 4.6 After the job
- **Rate** → `RateReviewScreen` → `ReviewRepository.submit` (transaction: review id = booking id, new average, `isRated`).
- **Receipt** → `ReceiptScreen.kt` (service, visit fee, extras, coupon, payment ID).
- **Redo (7 days)** → `requestRevisit` creates a **redo PIN** → provider fixes it and enters the PIN → `markRevisitDone`.
- **Report a problem** → `ComplaintRepository.report` → admin *Complaints*.

### 4.7 Cancel / reschedule
- Customer while REQUESTED/ACCEPTED: `cancel` (frees the slot) or `reschedule` (moves the slot in one transaction, back to REQUESTED, new 1-hour window). Provider can cancel an accepted job with a reason (`cancelByProvider`).
- A slot is freed only if it still belongs to that booking (`ownsSlot`), so an old booking can never free someone else's slot.

### 4.8 Chat, notifications, push
- *Chat* button → `ChatScreen.kt` → `ChatRepository.liveMessages` / `send` (`bookings/{id}/messages`), open until 7 days after the job.
- Every event → `NotificationRepository.send` writes `notifications/{id}` → bell badge (`AppNavHost`) and **Alerts** screen.
- Then `Push.send` asks `admin-server/api/send-push.js`, which checks the sender and sends the push to the receiver's token.
- Tapping a push → `MainActivity` → `PushLink` → `AppNavHost.linkRoute` → chat (messages) or booking / job details.

### 4.9 Admin
- Dashboard (`AdminDashboardScreen.kt`, `AdminDashboardViewModel.kt`) counts everything live; rows open filtered lists.
- Approvals → `AdminRepository.setApproval`. Users → block, reset link, *Change password* → `AdminRepository.changeUserPassword` → `set-password.js`.
- Bookings → filters and read-only `AdminBookingDetailScreen.kt`. Services, Categories, **Coupons** (`AdminCouponsScreen.kt`), Complaints.

---

## 5. Known limits (honest, for the viva)
- The provider's app downloads the booking including the PINs (hidden on screen). Fully hiding them needs server code.
- The coupon **amount** is calculated in the app; the rules only check the coupon exists and is on. A server payment/price check would close this.
- The 1-hour "no answer" notice is created when the customer opens the app (a timed push needs a scheduled server).
- Photos (Firebase Storage) and live maps (Maps SDK) need paid plans, so they are future scope.
