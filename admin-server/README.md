# ServiceHub admin server (free, on Vercel)

One small function: `api/set-password.js`. It lets the **admin** set a new password for a
customer or provider. It checks the caller's Firebase login and that their role is `admin`
before changing anything, then signs the user out everywhere and sends them an in-app notification.

**Never put the service-account key in this folder or on GitHub.** It goes only into Vercel's settings.

## Setup (about 10 minutes, free, no card)

1. **Firebase key**
   Firebase Console → ⚙ **Project settings** → **Service accounts** → **Generate new private key** → **Generate key**.
   A `.json` file downloads. Keep it private (don't email it, don't upload it to GitHub).

2. **Vercel account**
   Go to https://vercel.com → **Sign Up** → **Continue with GitHub** (Hobby plan, free).

3. **Create the project**
   Vercel → **Add New… → Project** → **Import** the `ServiceHub` GitHub repository.
   - **Root Directory:** click **Edit** → choose **`admin-server`**
   - **Framework Preset:** Other
   - Open **Environment Variables** and add:
     - Name: `FIREBASE_SERVICE_ACCOUNT`
     - Value: open the downloaded `.json` file in Notepad → **Ctrl + A**, **Ctrl + C** → paste here
   - Click **Deploy**.

4. **Copy the address**
   When it says *Congratulations*, copy the domain, e.g. `https://servicehub-admin-server.vercel.app`.

5. **Tell the app**
   In the project folder open **`local.properties`** (it is never uploaded) and add a line:
   ```
   ADMIN_API_URL=https://servicehub-admin-server.vercel.app
   ```
   Then build / Run the app again.

## How to check it works
Admin → Users → open a user with a real login → **Change password** → enter a new password twice → **Change**.
Log in as that user with the new password.

## Push notifications (`api/send-push.js`)

The app asks this server to turn each in-app notification into a push on the receiver's phone.
Nothing extra to set up: it uses the same project and the same `FIREBASE_SERVICE_ACCOUNT`.
After pushing the new code to GitHub, Vercel deploys it by itself (check **Deployments** shows *Ready*).

Check it is live: open `https://<your-domain>/api/send-push` in a browser -> `{"error":"Use POST"}`.

Who may push what: the admin anything; everyone else only notifications about a booking they belong to,
sent to the other person on that booking (same rule as `firestore.rules`).
