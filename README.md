![Strivacity Android SDK](https://static.strivacity.com/images/android-native-sdk-banner.png)

See our [Developer Portal](https://www.strivacity.com/learn-support/developer-hub) to get started with developing for the Strivacity product.

# Overview

This SDK allows you to integrate Strivacity's policy-driven journeys into your brand's Android mobile application based on Jetpack Compose UI using native mobile experiences via [Journey-flow API for native clients](https://docs.strivacity.com/reference/journey-flow-api-for-native-clients).

The SDK uses the [PKCE extension to OAuth](https://tools.ietf.org/html/rfc7636) to ensure the secure exchange of authorization codes in public clients.

## How to use

Strivacity SDK for Android with Jetpack Compose UI is available on [MavenCentral](https://search.maven.org/search?q=g:com.strivacity.android%20AND%20a:kotlin_native_sdk).

```groovy
implementation 'com.strivacity.android:kotlin_native_sdk:<version>'
```

## Demo Application

A demo application is available in the `demoapplication` folder.

## Overview

The Strivacity SDK for Android with Jetpack Compose UI provides the possibility to build an application which can communicate with Strivacity using OAuth 2.0 PKCE flow.

## Instantiate Native SDK

First, you must create a NativeSDK instance:

```kotlin
NativeSDK(
     "<issuer-url>",                // specifies authentication server domain, e.g.: https://your-domain.tld
     "<client-id>",                 // specifies OAuth2 client ID
     "<redirect-uri>",              // specifies the redirect uri, e.g.: android://native-flow
     "<post-logout-uri>",           // specifies the post logout uri, e.g.: android://native-flow
     storage                        // provide a `com.strivacity.android.native_sdk.Storage` implementation
 )
```

An example implementation `SharedPreferenceStorage` is given for the Storage interface using `SharedPreferences` as a backend.

## Register the custom schema

The custom schema used in the redirect and post logout uri's needs to be registered for your application.
Create an `intent-filter` xml tag in your `AndroidManifest.xml` file in one of your `activity` tags.
Set the same `schema` and `host` parameters provided in the `NativeSDK`.

For example:
```xml
<activity android:name=".RedirectActivity"
   android:exported="true">
   <intent-filter>
       <action android:name="android.intent.action.VIEW" />

       <category android:name="android.intent.category.DEFAULT" />
       <category android:name="android.intent.category.BROWSABLE" />

       <data android:host="native-flow" android:scheme="android" />
   </intent-filter>
</activity>
```

Create an `Activity` to handle the call from the custom schema and pass the required information back to you primary `Activity`
For example:
```kotlin
class RedirectActivity : ComponentActivity()  {

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      val data = intent.data

      val intent = Intent(this, MainActivity::class.java)
      intent.setData(data)
      intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      startActivity(intent)

      finish()
   }

}

```

In your primary `Activity` provide an implementation for the `onNewIntent` method and set the intent.

```kotlin
override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
   super.onNewIntent(intent, caller)
   setIntent(intent)
}
```

Register for LifecycleEvents and handle the resume events calling the `nativeSDK.continueFlow` method.

```kotlin
    DisposableEffect(Unit) {
        val activity = context as? ComponentActivity
        val lifecycle = activity?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && nativeSDK.isRedirectExpected()) {
                coroutineScope.launch {
                    val uri = when(activity?.intent?.data) {
                        null -> null
                        else -> activity.intent?.data.toString()
                    }
                    nativeSDK.continueFlow(uri)
                }
            }
        }

        lifecycle?.addObserver(observer)

        onDispose {
            lifecycle?.removeObserver(observer)
        }
    }
```

## Initialize Native SDK

Initialize the NativeSDK instance to prepare the SDK internals and load the existing session, if any.
This is an suspended method, and should be treated accordingly.

```kotlin
nativeSDK.initializeSession()
```

This can be done, for example, in a `LaunchedEffect` method on the current view
```kotlin
 LaunchedEffect(Unit) {
     coroutineScope.launch {
         nativeSDK.initializeSession()
         loading = false
     }
 }
```

## Integrate into your view

The SDK can have three states:
1. Account already logged in
    - `nativeSDK.session.profile` is populated
2. Login in progress
    - `nativeSDK.session.loginInProgress` is set to `true`
3. No session
    - otherwise

This can be implemented in the following way:

```kotlin
val loginInProgress by nativeSDK.session.loginInProgress.collectAsState()
val profile by nativeSDK.session.profile.collectAsState()

if (loading) {
   Text("Loading...")
} else {
   if (profile != null) {
     // (1) implement you logged in screens
   } else if (loginInProgress) {
     // (2) login in progress, display login view
   } else {
      // (3) no active session, you can trigger a login from this state
   }
}
```

### How to launch a login flow

This can be done in location (3) using the `login` method on the `nativeSDK` instance.
```kotlin
suspend fun login(
  context: Context,                             // `Context` of your Activity , e.g.: `LocalContext.current`
  onSuccess: () -> Unit,                        // callback method that will be called after a successful login
  onError: (Error) -> Unit,                     // callback method that will be called if an error occures
  loginParameters: LoginParameters? = null      // additional parameters to pass through during login
)
```

The following additional parameters can be set:
```kotlin
data class LoginParameters(
    val prompt: String? = null,             // sets the corresponding parameter in the OAuth2 authorize call
    val loginHint: String? = null,          // sets the corresponding parameter in the OAuth2 authorize call
    val acrValue: String? = null,           // sets the corresponding parameter in the OAuth2 authorize call
    val scopes: List<String>? = null        // sets the corresponding parameter in the OAuth2 authorize call
)
```

Example usage:
```kotlin
var error by remember { mutableStateOf(null as Error?) }
val coroutineScope = rememberCoroutineScope()

Button(onClick = {
  coroutineScope.launch {
      error = null
      nativeSDK.login(
          context,
          {},
          { error = it },
          LoginParameters(scopes = listOf("openid", "profile", "offline"))
      )
  }
}) {
  Text("Login")
}

when (error) {
   null -> {} // no error

   is OidcError -> Text(
      (error as OidcError).errorDescription ?: (error as OidcError).error,
      color = Color.Red
   )

   is HostedFlowCanceledError -> Text("Hosted flow canceled", color = Color.Red)
   is SessionExpiredError -> Text("Session expired", color = Color.Red)

   else -> Text("N/A", color = Color.Red)
}
```

### Display the login view

This can be done in location (2). An example implementation is given in the demo application with the `LoginView` Composable function.

During login, it's possible to programmatically cancel a login flow using the `cancelFlow` method on the `nativeSDK` instance.

For example using a FloatingActionButton:
```kotlin
 val loginInProgress by nativeSDK.session.loginInProgress.collectAsState()

 if (loginInProgress) {
     FloatingActionButton(
         onClick = { nativeSDK.cancelFlow() },
     ) {
         Icon(Icons.Filled.Close, "Cancel login flow")
     }
 }
```

### Handling a logged-in session

The current session information is available in location (1).

The retrieved claims can be accessed in the `nativeSDK.session.profile`.
For example, displaying the `given_name` claim with validation can be done like:

```kotlin
val profile by nativeSDK.session.profile.collectAsState()

Text("Hello ${profile.claims["given_name"]}")
```

The access token can be retrieved using the `getAccessToken` method on the `nativeSDK` instance. Keep in mind that if the access token is expired and a refresh token is available, this method will try to renew the access token.

To validate if the current session's access token is still valid, the `isAuthenticated` method can be called on the `nativeSDK` instance. This call will also try to refresh the access token, if possible.

To trigger a logout the `logout` method can be called on the `nativeSDK` instance.

Example for using the methods above:
```kotlin
 Text("Hello ${profile!!.claims["given_name"]}")
 Button(onClick = {
     coroutineScope.launch {
         nativeSDK.logout()
     }
 }) {
     Text("Logout")
 }

 Button(onClick = {
     coroutineScope.launch {
         try {
             val accessToken = nativeSDK.getAccessToken()
             Toast.makeText(context, accessToken, Toast.LENGTH_LONG).show()
         } catch (e: Exception) {
             Toast.makeText(context, "Unable to fetch access token", Toast.LENGTH_LONG).show()
         }
     }
 }) {
     Text("Get Access Token")
 }

```

## Author

Strivacity: [opensource@strivacity.com](mailto:opensource@strivacity.com)

## License

Strivacity is available under the Apache License, Version 2.0. See the [LICENSE](./LICENSE) file for more info.

## Vulnerability Reporting

The [Guidelines for responsible disclosure](https://www.strivacity.com/report-a-security-issue) details the procedure for disclosing security issues.
Please do not report security vulnerabilities on the public issue tracker.
