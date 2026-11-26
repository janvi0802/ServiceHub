# How to test ServiceHub on your phone (beginner guide)

Hi! This guide is for someone who has **never used Android Studio**. Just follow the steps in order.
It takes about **1 hour the first time** (mostly waiting for downloads). After that, about 2 minutes.

---

## What you need

- A **Windows laptop/PC** with at least **8 GB RAM** and **15 GB free space**
- An **Android phone** (Android 7 or newer)
- A **USB cable that can transfer data** (some cheap cables only charge – if the phone is not detected, try another cable)
- **Internet** (the first time, about 2–3 GB is downloaded. A slow connection can take an hour – a mobile hotspot is often faster)
- The **ServiceHub.zip** file
- The **test login details** (email + password) from Janvi. They are NOT inside the zip.

> **Shortcut:** if Janvi gave you a file called **`app-release.apk`** (or `app-debug.apk`), you don't need
> Android Studio at all. Copy it to your phone, tap it, allow "Install unknown apps" when asked, and skip to **Step 7**.

---

## Step 1 – Install Android Studio (one time, ~20 min)

1. Go to **https://developer.android.com/studio** and click **Download Android Studio**.
2. Run the downloaded file. Click **Next** on every screen (keep the default options) → **Install** → **Finish**.
3. Android Studio opens. Choose **Do not import settings** → **OK**.
4. On the "Setup Wizard": choose **Standard** → **Next** → accept the licenses (tick **Accept** for each item) → **Finish**.
5. Wait until it finishes downloading the "SDK components" → **Finish**.

## Step 2 – Unzip the project

1. Right-click **ServiceHub.zip** → **Extract All…**
2. Extract to a **short, simple place**, for example **`C:\ServiceHub`**
   (avoid OneDrive or very long folder paths – they can cause errors).
3. Open the extracted folder once and check you can see files like `settings.gradle.kts`, `README.md` and a folder named `app`.
   If you only see another folder named `ServiceHub`, use **that inner folder** in the next step.

## Step 3 – Open the project

1. In Android Studio: **File → Open…** (or **Open** on the welcome screen).
2. Select the **ServiceHub** folder (the one that contains `settings.gradle.kts`) → **OK**.
3. If it asks **"Trust and open project?"** → click **Trust Project**.

## Step 4 – Wait for "Gradle sync" (the long part)

**What is Gradle?** It is the tool that downloads everything the app needs and builds it. You don't need to do anything – just wait.

1. Look at the **bottom right** of Android Studio: you'll see progress like *"Gradle: Download…"* or *"Syncing…"*.
2. The first time this can take **10–60 minutes** depending on your internet. **Don't close Android Studio.**
3. It's finished when the progress bar disappears and the **Build** tab at the bottom shows no red errors.

**Important:**
- If a popup says **"Upgrade Gradle / Android Gradle Plugin"** → click **"Don't remind me again"** or close it. **Do NOT upgrade.**
- If you see **"Connect timed out"** or **"Could not download"**: your internet was too slow. Click the 🐘 **elephant icon** at the top right
  ("Sync Project with Gradle Files") to try again. A mobile hotspot usually fixes it.

## Step 5 – Prepare your phone (one time)

1. On the phone: **Settings → About phone** → tap **Build number** **7 times** quickly
   (Samsung: *Settings → About phone → Software information → Build number*;
   Xiaomi/Redmi: *Settings → About phone → MIUI/OS version*).
   You'll see "You are now a developer!". Enter your screen lock PIN if asked.
2. Go back to **Settings → Developer options** (Samsung: at the bottom of Settings; Xiaomi: *Additional settings*).
3. Turn on **USB debugging** → **OK**.
   (Xiaomi/Redmi/POCO only: also turn on **Install via USB**.)
4. Connect the phone to the PC with the USB cable.
5. On the phone, a popup appears: **"Allow USB debugging?"** → tick **Always allow from this computer** → **Allow**.

## Step 6 – Run the app

1. At the **top** of Android Studio, find the **device box** (left of the green ▶ button). Choose **your phone's name**.
   - If your phone is not listed: unplug and plug the cable again, check the "Allow USB debugging" popup on the phone, or try another cable.
2. Click the green **▶ Run** button (or press **Shift + F10**).
3. Wait 1–3 minutes. If the phone asks to install the app → **Install** / **Allow**.
4. **ServiceHub opens on your phone.** 🎉 It also stays installed, so next time you can just open it from the phone.

## Step 7 – Log in and test

Use the logins Janvi gave you. There are three kinds of account:

| Account | What you can do |
|---|---|
| **Customer** | Browse services, book a professional, track the booking, see the PIN, pay, rate, request a revisit |
| **Provider** (professional) | Accept / reject requests, "I'm on my way", start with the customer's PIN, complete the job, see earnings |
| **Admin** | Approve providers, block users, edit prices, add categories, see the dashboard and complaints |

You can also **create your own account** (Login → "New here? Create an account"). A new **provider** account must be **approved by the admin** before customers can see it.

> Sample providers like "Ramesh Patel" have **no login** – they are only there to be booked.
> To test the full flow, book a provider account that someone can log in to.

### A quick test (about 10 minutes)

Best with **two phones** (one customer, one provider). With one phone, log out and in between accounts (*Profile → Log out*).

1. **Customer:** Home → **Book a service** (or tap a category) → choose the service → **Choose professional** → **Book** → pick date and slot → **Review booking** → (optional) **Apply** an offer → **Confirm booking**.
2. **Provider:** the **Requests** tab shows a red number (and a push arrives) → open the request → **Accept**.
3. **Customer:** the booking now shows **Accepted** and a **4-digit PIN** (also on Home). Try **Chat**.
4. **Provider:** tap **I'm on my way** → enter the customer's PIN at the top → **Start job**.
5. **Provider:** **Complete job** → (optional) add an extra part like "Tap cartridge ₹180" → **Complete**.
6. **Payment:** Cash → provider taps **Cash collected**. UPI → customer taps **Pay now** and uses the test UPI id `success@razorpay` (no real money).
7. **Customer:** **Rate service** → **View receipt** → try **Share**.
8. **Admin:** open the **Dashboard** and check the numbers changed; tap a booking under **Bookings** to see its details.

The full list of **163 test cases** is in **`docs/TEST_SCRIPT.md`** (open it in Android Studio – it's next to this file).

## Step 8 – How to report a problem

For anything that looks wrong, send Janvi:
1. **What you did** (e.g. "Customer → booked Tap repair for tomorrow morning")
2. **What happened** vs **what you expected**
3. A **screenshot** (phone: **Power + Volume down** at the same time)
4. If known, the **test case number** from `docs/TEST_SCRIPT.md` (e.g. "TC63")

---

## Common problems and fixes

| Problem | Fix |
|---|---|
| "Connect timed out" / "Could not download" during sync | Slow internet. Click the 🐘 elephant icon (Sync) again. Try a mobile hotspot. |
| Popup asking to upgrade Gradle / AGP / Kotlin | Close it / "Don't remind me again". **Never upgrade.** |
| "SDK location not found" | **File → Project Structure → SDK Location** → click the suggested Android SDK path → **OK**. Then Sync again. |
| Phone not in the device list | Replug the cable, accept "Allow USB debugging" on the phone, turn USB debugging off and on, try another cable. |
| "Install failed" / "INSTALL_FAILED_USER_RESTRICTED" | Xiaomi/Redmi: turn on **Install via USB** in Developer options. Others: accept the install prompt on the phone. |
| App shows "No internet" / nothing loads | The phone needs internet (Wi-Fi or mobile data). The data is online in Firebase. |
| "Your account is blocked" | The admin blocked that account. Ask Janvi. |
| Provider account stuck on "Waiting for approval" | The admin must approve it (Admin → Approvals). |
| Android Studio is very slow | Close other programs. 8 GB RAM works but is slow; don't use the emulator, use a real phone. |
| You changed something by mistake and it won't build | Close Android Studio, delete the folder, extract the zip again, and start from Step 3. |

That's it – thank you for testing ServiceHub!
