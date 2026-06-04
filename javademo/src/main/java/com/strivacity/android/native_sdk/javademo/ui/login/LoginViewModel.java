package com.strivacity.android.native_sdk.javademo.ui.login;

import android.app.Activity;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.credentials.exceptions.CreateCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.strivacity.android.native_sdk.Error;
import com.strivacity.android.native_sdk.HeadlessAdapterDelegate;
import com.strivacity.android.native_sdk.LoginParameters;
import com.strivacity.android.native_sdk.PlatformError;
import com.strivacity.android.native_sdk.compat.HeadlessAdapterJava;
import com.strivacity.android.native_sdk.compat.NativeSDKJava;
import com.strivacity.android.native_sdk.render.models.Screen;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

public class LoginViewModel extends ViewModel implements HeadlessAdapterDelegate {

    private final MutableLiveData<String> error = new MutableLiveData<>(null);
    private final MediatorLiveData<Boolean> isProcessing = new MediatorLiveData<>(false);
    private final MutableLiveData<Void> onLoginComplete = new MutableLiveData<>();
    private final MutableLiveData<Throwable> submitError = new MutableLiveData<>(null);

    private final MutableLiveData<Screen> currentScreen = new MutableLiveData<>(null);
    private final NativeSDKJava nativeSDK;

    private HeadlessAdapterJava adapter;
    private Observer<Boolean> loginInProgressObserver;

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Void> getOnLoginComplete() {
        return onLoginComplete;
    }

    public LoginViewModel(@NonNull NativeSDKJava nativeSDK) {
        Objects.requireNonNull(nativeSDK);
        this.nativeSDK = nativeSDK;
    }

    public LiveData<Boolean> getIsProcessing() {
        return isProcessing;
    }

    public LiveData<Throwable> getSubmitError() {
        return submitError;
    }

    public void setSubmitError(Throwable throwable) {
        if (throwable instanceof PlatformError) {
            final Throwable cause = throwable.getCause();
            if (cause instanceof GetCredentialCancellationException
                    || cause instanceof CreateCredentialCancellationException) {
                // do nothing when exception is raised because user cancelled passkey operation
                return;
            }
        }
        submitError.postValue(throwable);
    }

    public LiveData<Screen> getCurrentScreen() {
        return currentScreen;
    }

    public void submit(String action, Map<String, Object> body, Consumer<Throwable> callback) {
        adapter.submit(action, body, callback::accept);
    }

    public void startLogin(Activity context) {
        if (Boolean.TRUE.equals(nativeSDK.getLoginInProgressLiveData().getValue())) {
            Log.w("LoginViewModel", "Login is already in progress. Not starting a new one");
            return;
        }

        loginInProgressObserver =
                inProgress -> {
                    if (Boolean.TRUE.equals(inProgress)) {
                        initializeHeadlessAdapter();
                    }
                };
        nativeSDK.getLoginInProgressLiveData().observeForever(loginInProgressObserver);

        nativeSDK.login(
                uriToLoad -> {
                    // Falling back to Hosted experience can be triggered by application or
                    // by cloud side. In both cases, a graceful handling of fallback is to open
                    // a browser and let the user continue the flow in it.
                    final CustomTabsIntent intent = new CustomTabsIntent.Builder().build();
                    intent.launchUrl(context, Uri.parse(uriToLoad));
                },
                this::loginSuccess,
                this::loginError,
                new LoginParameters(
                        null, null, null, List.of("profile", "email", "openid"), null, null),
                context);
    }

    private void initializeHeadlessAdapter() {
        if (adapter != null) return;
        adapter = nativeSDK.createHeadlessAdapter(this);
        adapter.initialize();
        isProcessing.addSource(adapter.isProcessingLiveData(), isProcessing::setValue);
    }

    private void loginSuccess() {
        Log.i("LoginViewModel", "App notified about successful login");
        onLoginComplete.postValue(null);
    }

    private void loginError(@NotNull Error error) {
        Log.e("LoginViewModel", "Login attempt failed", error);
        this.error.postValue(error.getMessage());
        onLoginComplete.postValue(null);
    }

    @Override
    public void renderScreen(@NotNull Screen screen) {
        Log.d("LoginViewModel", "Render screen");
        Objects.requireNonNull(screen.getScreen());
        currentScreen.postValue(screen);
    }

    @Override
    public void refreshScreen(@NotNull Screen screen) {
        Log.d("LoginViewModel", "Refresh screen");
        Objects.requireNonNull(screen.getScreen());
        currentScreen.postValue(screen);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (loginInProgressObserver != null) {
            nativeSDK.getLoginInProgressLiveData().removeObserver(loginInProgressObserver);
            loginInProgressObserver = null;
        }
    }

    public void fallbackToHostedExperience() {
        adapter.triggerFallback();
    }

    public boolean isRedirectExpected() {
        return nativeSDK.isRedirectExpected();
    }

    public void cancelFlow() {
        nativeSDK.cancelFlow();
    }

    public static class LoginViewModelFactory implements ViewModelProvider.Factory {
        private final NativeSDKJava nativeSDKJava;

        public LoginViewModelFactory(NativeSDKJava nativeSDKJava) {
            this.nativeSDKJava = nativeSDKJava;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(LoginViewModel.class)) {
                return Objects.requireNonNull(modelClass.cast(new LoginViewModel(nativeSDKJava)));
            }
            throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
        }
    }
}
