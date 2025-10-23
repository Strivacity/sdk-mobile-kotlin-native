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

We support two different login views:
* SDK Provided Login View
   * This is provided by the SDK using the `LoginView` Composable function.
   * In this mode you are responsible for rendering specific widget types.
   * Customization options:
      * Per widget type customization
      * Customize the layout for specific screens
   * This mode will track server side configuration changes (e.g.: new input fields, new screens, etc.)
* Headless
   * This option lets you take full control over the rendering of the login view
   * In this mode you are responsible for rendering the login view and handling the login flow based on the screens provided
   * This mode will **not** track server side configuration changes by default (e.g.: new input fields, new screens, etc.)

#### SDK Provided Login View

This can be done in location (2). An example implementation is given in the demo application with the `LoginView` Composable function.

#### Headless

For this operation mode we provide a `HeadlessAdapter` class. This class takes a delegate that will receive the screens that need to be rendered.
An example implementation is given in the `headlessdemo` application with the `LoginScreen` Composable function.

```kotlin
interface HeadlessAdapterDelegate {
  fun renderScreen(screen: Screen)

  fun refreshScreen(screen: Screen)
}
```

The `renderScreen` method will be called when a new screen is available.
The `refreshScreen` method will be called when a screen needs to be refreshed, for example, when there is an error message to display.

Based on the screen type available in the `screen` property of the `Screen` class, you will need to render the corresponding view.

Example usage:
```kotlin
@Composable
fun LoginScreen(nativeSDK: NativeSDK) {

  val loginScreenModel by remember { mutableStateOf(LoginScreenModel()) }

  val headlessAdapter by remember { mutableStateOf(HeadlessAdapter(nativeSDK, loginScreenModel)) }

  headlessAdapter.initialize()

  val screen by loginScreenModel.screen.collectAsState()

  if (screen == null) {
    Text("Loading")
  } else {
    when (screen!!.screen) {
      "identification" -> {
        IdentificationView(screen!!, headlessAdapter)
      }
      "password" -> {
        PasswordView(screen!!, headlessAdapter)
      }
      else -> {
        Text("Unknown screen")
      }
    }
  }
}

class LoginScreenModel : HeadlessAdapterDelegate {
  private val _screen = MutableStateFlow<Screen?>(null)
  val screen: StateFlow<Screen?> = _screen

  override fun renderScreen(screen: Screen) {
    _screen.value = screen
  }

  override fun refreshScreen(screen: Screen) {
    _screen.value = screen
  }
}
```

**Rendering the screens:**

Information about what need to be rendered can be retrieved from the `forms` property of the `Screen` class.

To check if a specific field has an error, you can use the `messages` function on the `HeadlessAdapter` instance.
```kotlin
fun messages(): StateFlow<Messages?>
```

To submit the form, you can use the `submit` function on the `HeadlessAdapter` instance.
```kotlin
suspend fun submit(formId: String, body: Map<String, Any>)
```

Example for a password screen,
Keep in mind that this is a simplified example that will not handle dynamic changes to the screen.

```kotlin
@Composable
fun PasswordView(screen: Screen, headlessAdapter: HeadlessAdapter) {
  val messages by headlessAdapter.messages().collectAsState()

  val coroutineScope = rememberCoroutineScope()

  var password by remember { mutableStateOf("") }
  var keepMeLoggedIn by remember {
    mutableStateOf(
        screen.forms
            ?.find { it.id == "password" }
            ?.widgets
            ?.find { it.id == "keepMeLoggedIn" }
            ?.value() as Boolean?
            ?: false)
  }

  val identifierWidget =
      screen.forms?.find { it.id == "reset" }?.widgets?.find { it.id == "identifier" }

  val identifier =
      when (identifierWidget) {
        is StaticWidget -> {
          identifierWidget.value
        }
        else -> ""
      }

  Column {
    Text("Enter password")

    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(identifier)
      TextButton(onClick = { coroutineScope.launch { headlessAdapter.submit("reset", mapOf()) } }) {
        Text("Not you?")
      }
    }

    TextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Enter your password") },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    )
    val errorMessage = messages?.errorMessageForWidget("password", "password")
    if (errorMessage != null) {
      Text(errorMessage, color = Color.Red)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
      Checkbox(keepMeLoggedIn, { keepMeLoggedIn = it })
      Text("Keep me logged in")
    }

    Button(
        onClick = {
          coroutineScope.launch {
            headlessAdapter.submit(
                "password", mapOf("password" to password, "keepMeLoggedIn" to keepMeLoggedIn))
          }
        }) {
          Text("Continue")
        }

    TextButton(
        onClick = {
          coroutineScope.launch {
            headlessAdapter.submit("additionalActions/forgottenPassword", mapOf())
          }
        }) {
          Text("Forgot your password?")
        }

    TextButton(onClick = { coroutineScope.launch { headlessAdapter.submit("reset", mapOf()) } }) {
      Text("Back to login")
    }
  }
}
```

#### Cancel the active flow

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
