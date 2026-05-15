# Grama-Yatri Project Requirements

## 1. Project Overview
Grama-Yatri is an Android app for bus route discovery and live bus tracking with role-based capabilities for **users**, **drivers**, and **admins**.

## 2. Target Platform and Stack
- Android app using Kotlin + Jetpack Compose (Material 3)
- Minimum Android SDK: **24**
- Target/Compile SDK: **36**
- Backend services: **Firebase Authentication (Email/Password)** and **Cloud Firestore**
- Location source: **Google Play Services Fused Location Provider**

## 3. User Roles
- **User**: Can log in, browse routes, and track bus location updates.
- **Driver**: User capabilities + live location sharing for a selected route.
- **Admin**: Same app permissions as driver for route creation and live sharing.

## 4. Functional Requirements
1. The app must support authentication flows:
   - Login with email/password
   - Create account with role selection (User/Driver/Admin)
   - Forgot password (email reset link)
2. The app must check whether an account exists for entered email and guide the user to account creation when not found.
3. After successful authentication, the app must open route browsing.
4. The app must show available routes from Firestore.
5. Selecting a route must open a tracker view showing:
   - Route start and stop stations
   - Route stop timeline
   - Estimated arrival times per stop based on configured travel times
6. Drivers/admins must be able to enter a location-sharing mode and publish live location updates for the selected route.
7. Drivers/admins must be able to add routes with:
   - Route name (required)
   - Optional description
   - Stops input in line format: `Stop Name|Minutes from previous|Latitude(optional)|Longitude(optional)`
8. Route creation must validate:
   - At least 2 stops
   - Stop name presence
   - Integer minutes for non-first stops
   - Valid numeric latitude/longitude when provided

## 5. Data Requirements (Firestore)
- `users/{uid}`:
  - `uid`, `email`, `role` (`user`, `driver`, `admin`), `createdAt`
- `routes/{routeId}`:
  - `id`, `name`, `description`, `stops[]`
- `locations/{routeId}`:
  - `id`, `routeId`, `latitude`, `longitude`, `speed`, `heading`, `updatedAtMillis`, `driverId`, `driverName`

## 6. Security and Access Requirements
1. Authentication must be required for app data access.
2. Firestore rules must allow:
   - Authenticated users to read routes and locations
   - Authenticated users to create/update their own user profile document
   - Driver/admin role users to create routes and publish location updates
3. No anonymous auth flow is required.

## 7. Device and Permission Requirements
- Internet permission is required.
- Fine or coarse location permission is required to enable driver live-location sharing.

## 8. Reliability and UX Requirements
- Clear user-facing error messages are required for network, permission, and Firebase configuration/database issues.
- Route stream should retry on transient backend/network failures.
- Loading states must be shown during auth and route creation operations.

## 9. External Configuration Requirements
1. Firebase project must be connected to the Android app package `com.gramayatri.app`.
2. `google-services.json` must be present under `app/`.
3. Firebase Email/Password provider must be enabled.
4. Firestore database must be created and rules configured for the role model above.

## 10. Current Scope Boundaries
- In-app map visualization is not currently required.
- OTP/phone-based authentication is not required.
- Push notifications are not required.

