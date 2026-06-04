# NativeSDKJava — Java Compatibility Layer

`NativeSDKJava` is the Java-friendly entry point for the Strivacity Native SDK. It wraps the
Kotlin-coroutine-based `NativeSDK` and exposes:

- Async operations as `CompletableFuture`
- Kotlin `StateFlow` properties as `LiveData`
- Lambda callbacks as SAM-compatible interfaces

> For background on the SDK concepts (PKCE flow, `NetworkConfiguration`, `LoginParameters`,
> error types, manifest setup) refer to the [main SDK README](../README.md).

---

## Dependency

Add the compat module alongside the core SDK:

```kotlin
implementation("com.strivacity.android:kotlin_native_sdk_compat:<version>")
```

---

## Instantiation

Use `NativeSDKJava.Builder`. The five required parameters must be provided at construction time;
optional ones are configured via fluent setters before calling `build()`.

```java
Storage storage = new SharedPreferenceStorage(
        getSharedPreferences("session", MODE_PRIVATE));

NativeSDKJava sdk = new NativeSDKJava.Builder(
        "https://your-domain.tld",              // issuer
        "<client-id>",                          // clientId
        "myapp://oauth/login-callback",         // redirectURI
        "myapp://oauth/logout-callback",        // postLogoutURI
        storage                                 // Storage implementation
).build();
```

Optional builder methods:

| Method                                        | Default                  | Purpose                                     |
|-----------------------------------------------|--------------------------|---------------------------------------------|
| `.mode(SdkMode)`                              | `SdkMode.Android`        | Change the SDK mode sent to the backend     |
| `.logging(Logging)`                           | `DefaultLogging`         | Route SDK logs to your own logger           |
| `.networkConfiguration(NetworkConfiguration)` | `NetworkConfiguration()` | Set user-agent and custom `x-sty-*` headers |

See the [main README](../README.md#network-configuration) for `NetworkConfiguration` details.

---

## Session lifecycle

### Initialize on app start

Restore any persisted session and refresh tokens if needed. Call this before checking
authentication state.

```java
// Non-blocking — resolves in the background
sdk.initializeSession();
```

The returned `CompletableFuture<Void>` can be chained if you need to act on completion:

```java
sdk.initializeSession().thenRun(() -> runOnUiThread(this::onSessionReady));
```

### Observe authentication state

`NativeSDKJava` exposes session state as `LiveData` for use with Android Lifecycle components:

```java
// true while a login flow is actively in progress
sdk.getLoginInProgressLiveData().observe(this, inProgress -> { ... });

// non-null when the user is signed in; null when signed out
sdk.getProfileLiveData().observe(this, profile -> {
    if (profile != null) {
        Map<String, Object> claims = profile.getClaims();
        String givenName = (String) claims.get("given_name");
    }
});
```

---

## Login flow

### Starting a login

Call `login()` from any UI context. The SDK internally drives the headless UI via
`HeadlessAdapterJava` callbacks (see below). If the native flow cannot continue,
`fallbackHandler` is invoked with a URL to open in a browser.

Pass an `Activity` context as the last parameter to enable Passkey authentication.

```java
sdk.login(
    uriToLoad -> {
        // Open hosted fallback in a Custom Tab
        new CustomTabsIntent.Builder().build()
                .launchUrl(activity, Uri.parse(uriToLoad));
    },
    () -> { /* onSuccess — login complete */ },
    error -> { /* onError — inspect error type */ },
    new LoginParameters(),  // optional; scopes, hints, etc.
    activity                // optional; required for Passkeys
);
```

### Headless adapter

After `loginInProgressLiveData` emits `true`, create a `HeadlessAdapterJava` to receive and
respond to login screens:

```java
// Implement HeadlessAdapterDelegate in your ViewModel or controller
class LoginViewModel extends ViewModel implements HeadlessAdapterDelegate {

    private HeadlessAdapterJava adapter;
    private final MediatorLiveData<Boolean> isProcessing = new MediatorLiveData<>(false);

    void onLoginStarted() {
        adapter = sdk.createHeadlessAdapter(this);
        adapter.initialize();
        // Forward the adapter's processing state to observers
        isProcessing.addSource(adapter.isProcessingLiveData(), isProcessing::setValue);
    }

    @Override
    public void renderScreen(@NonNull Screen screen) {
        // Show the screen identified by screen.getScreen()
    }

    @Override
    public void refreshScreen(@NonNull Screen screen) {
        // Re-render the current screen (e.g. to display field-level errors)
    }
}
```

### Submitting a form

Use `submitAsync()` to submit a form or navigate to an additional action. The callback receives
`null` on success or the thrown `Throwable` on failure.

```java
adapter.submit(
    "password",
    Map.of("password", enteredPassword),
    throwable -> {
        if (throwable != null) showError(throwable);
    }
);
```

`isProcessingLiveData` on the adapter automatically reflects in-progress state — no manual
tracking required.

---

## Redirect callback handling

If the native flow triggers a hosted redirect, the operating system will re-open your Activity via
the custom URI scheme. Handle the callback in `onResume`:

```java
@Override
protected void onResume() {
    super.onResume();
    if (sdk.isRedirectExpected()) {
        Uri data = getIntent().getData();
        sdk.continueFlow(data != null ? data.toString() : null);
    }
}
```

Pass `null` to `continueFlow` to signal that the user cancelled the hosted flow.

For manifest and `RedirectActivity` setup, see the
[main README](../README.md#register-the-custom-schema).

---

## Cancelling a flow

Cancel the active login flow at any time. Optionally pass an `Error` to propagate it to the
original `onError` callback.

```java
sdk.cancelFlow();          // silent cancel
sdk.cancelFlow(someError); // cancel with error propagation
```

---

## Authenticated session

```java
// Check validity (also refreshes an expiring token)
sdk.isAuthenticated().thenAccept(authenticated -> { ... });

// Retrieve the current access token (refreshes if expired, returns null if signed out)
sdk.getAccessToken().thenAccept(token -> { ... });

// Sign out — clears local session and calls the OIDC end-session endpoint
sdk.logout();

// Revoke the active token server-side and clear the local session
// Prefer this over logout when you do not need the OIDC end-session redirect
sdk.revoke();
```

---

## Entry flow (deep-link challenge)

Use `entry()` when the app is launched via a challenge URI (e.g. from an email link). The flow
behaves identically to `login()` once the session is established.

```java
sdk.entry(
    uri,            // the Uri from the incoming Intent
    uriToLoad -> { /* fallback handler */ },
    () -> { /* onSuccess */ },
    error -> { /* onError */ },
    activity        // optional; required for Passkeys
);
```

