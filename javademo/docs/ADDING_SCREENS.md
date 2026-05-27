# Adding New Login Screens to javademo

This guide explains how to add support for a new backend-driven screen in the javademo app.

## Background

The Strivacity headless SDK drives the login flow by telling the app which screen to render.
Each screen is identified by a string ID (e.g. `"registration"`, `"passkeyEnroll"`).
The javademo uses a Fragment-per-screen pattern: each screen ID maps to one `ScreenFragment`
subclass that owns its layout, reads widget values from the `Screen` model, and submits user
input back through the `LoginViewModel`.

```
SDK (HeadlessAdapter)
  └─▶ LoginViewModel.renderScreen()    stores Screen in LiveData
        └─▶ LoginFragment              observes LiveData, replaces active fragment
              └─▶ YourNewFragment      renders UI, calls viewModel.submit()
```

On repeat delivery of the same screen ID (e.g. after a validation error), `LoginFragment`
calls `lastDisplayedFragment.refresh(screen)` instead of replacing the fragment, so the UI
stays in place and only errors are updated.

---

## Reference implementations

| Screen ID        | Fragment                 | Notable features                                    |
|------------------|--------------------------|-----------------------------------------------------|
| `identification` | `IdentifierFragment`     | Single text field, simple submit                    |
| `password`       | `PasswordFragment`       | Password input, field-level error                   |
| `passkeyEnroll`  | `PasskeyEnrollFragment`  | Pre-populated field from backend, secondary action  |
| `registration`   | `RegistrationFragment`   | Multiple fields, checkbox, dynamic provider buttons |

---

## Step-by-step

### 1. Identify the screen contract

Open the matching Compose view in `headlessdemo/src/main/java/.../login/`.
Note these things before writing any code:

| What to find             | Where to look in the headless view                              |
|--------------------------|-----------------------------------------------------------------|
| **Screen ID**            | Key inside `when (screenId)` in `LoginScreen.kt`               |
| **Form ID(s)**           | First argument of every `headlessAdapter.submit(...)` call      |
| **Field names**          | Keys in the map passed to `headlessAdapter.submit(...)`         |
| **Pre-populated values** | `screen.forms?.find { it.id == "..." }?.widgets?.find { ... }` |
| **Secondary actions**    | Additional `submit(...)` calls (skip, back, provider buttons)   |

---

### 2. Create the layout XML

Add `fragment_<screen_id>.xml` in `javademo/src/main/res/layout/`.

- Use a `ScrollView` root when the screen contains multiple input fields.
- Use `TextInputLayout` + `TextInputEditText` for every text field — required to show
  inline validation errors via `TextInputLayout.setError()`.
- Add one primary `Button` for the main submit action and a text-style `MaterialButton`
  for each secondary action (skip, back, etc.).
- Give every interactive view a unique `android:id` for view binding.
- Use `@string/...` references — add all new strings to `strings.xml`.
- For variable-count items (e.g. external login providers), add a `LinearLayout` container
  with `android:visibility="gone"` and populate it dynamically in the Fragment.

---

### 3. Create the Fragment

Add `YourNewFragment.java` in `javademo/src/main/java/.../ui/login/my_app_screen/`.

Extend `ScreenFragment` and follow this structure:

```java
public class YourNewFragment extends ScreenFragment {

    private static final String FORM_ID = "<form_id_from_sdk>";

    private Screen screen;
    private FragmentYourNewBinding binding;

    public YourNewFragment(Screen screen) { this.screen = screen; }

    @Override public void setScreen(Screen screen) { this.screen = screen; }

    @Override
    public View onCreateView(...) {
        binding = FragmentYourNewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(...) {
        super.onViewCreated(view, savedInstanceState);
        LoginViewModel viewModel = new ViewModelProvider(requireParentFragment()).get(LoginViewModel.class);

        binding.continueButton.setOnClickListener(v -> {
            viewModel.submit(FORM_ID, Map.of("fieldName", value), throwable -> {
                Log.d("YourNewFragment", "Submit completed", throwable);
                if (throwable != null) viewModel.setSubmitError(throwable);
            });
        });

        setMessages();
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }

    private void setMessages() {
        Optional.ofNullable(screen.getMessages()).ifPresent(messages ->
            binding.yourFieldLayout.setError(
                messages.errorMessageForWidget(FORM_ID, "fieldName"))
        );
    }

    @Override
    public void refresh(Screen screen) {
        super.refresh(screen); // always call super first — it calls setScreen()
        setMessages();
    }
}
```

#### Rules to follow

| Rule                                                                                       | Reason                                                                                                                    |
|--------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| Call `super.onViewCreated()`                                                               | Ensures Fragment lifecycle is properly initialised                                                                        |
| Scope `ViewModelProvider` to `requireParentFragment()`                                     | The `LoginViewModel` is owned by `LoginFragment`; using `requireActivity()` would create a separate, unconnected instance |
| Call `super.refresh()` before anything else                                                | `super` calls `setScreen()`, updating `this.screen`                                                                       |
| Null-check `binding` in helpers called after `onDestroyView()`                             | Async submit callbacks can fire after the view is gone                                                                    |
| Call `viewModel.setSubmitError(throwable)` when the callback receives a non-null throwable | Triggers the error screen in `LoginFragment`; not calling it silently swallows errors                                     |
| Use `Optional.ofNullable(screen.getForms())` when traversing widgets                       | `forms` is nullable — a raw stream call will throw                                                                        |
| Use `Collectors.toList()` not `Stream.toList()`                                            | `Stream.toList()` requires API 34; minSdk is 29                                                                           |
| Guard widget `value()` reads with `instanceof` before casting                              | `Widget.value()` returns `Object?` — an unchecked cast is unsafe                                                          |

---

### 4. Register the screen in LoginFragment

Open `LoginFragment.java` and make two additions:

Add the import alongside the other screen imports:

```java
import com.strivacity.android.native_sdk.javademo.ui.login.my_app_screen.YourNewFragment;
```

Add a case in the `switch (screenToDisplay)` block:

```java
case "<screen_id>":
    fragment = new YourNewFragment(screen);
    break;
```

The switch already handles the update-vs-render distinction automatically.

