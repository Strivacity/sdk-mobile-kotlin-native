package com.strivacity.android.native_sdk.javademo.ui;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.strivacity.android.native_sdk.Profile;
import com.strivacity.android.native_sdk.compat.NativeSDKJava;
import com.strivacity.android.native_sdk.javademo.model.User;
import com.strivacity.android.native_sdk.javademo.ui.login.LoginViewModel;
import com.strivacity.android.native_sdk.javademo.ui.profile.ProfileViewModel;
import java.util.Locale;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public class AppViewModel extends ViewModel {

    private final NativeSDKJava nativeSDK;
    private final LiveData<User> user;

    public AppViewModel(@NonNull NativeSDKJava nativeSDK) {
        this.nativeSDK = nativeSDK;
        user = Transformations.map(nativeSDK.getProfileLiveData(), AppViewModel::toAppUser);
    }

    public LiveData<User> getUser() {
        return user;
    }

    public LoginViewModel.LoginViewModelFactory createLoginViewModelFactory() {
        return new LoginViewModel.LoginViewModelFactory(nativeSDK);
    }

    public ProfileViewModel.ProfileViewModelFactory createProfileViewModelFactory() {
        return new ProfileViewModel.ProfileViewModelFactory(nativeSDK);
    }

    @Nullable
    private static User toAppUser(@Nullable Profile profile) {
        if (profile == null) {
            Log.d("AppViewModel", "Session change: null");
            return null;
        }
        final Map<String, Object> claims = profile.getClaims();
        final String sub = claims.containsKey("sub") ? (String) claims.get("sub") : "Unknown";
        final String givenName = (String) claims.get("given_name");
        final String familyName = (String) claims.get("family_name");
        final String email = (String) claims.get("email");
        Log.d("AppViewModel", String.format(Locale.US, "Session change: %s", sub));
        return new User(sub, givenName, familyName, email);
    }

    public static class AppViewModelFactory implements ViewModelProvider.Factory {

        private final NativeSDKJava nativeSDK;

        public AppViewModelFactory(NativeSDKJava nativeSDK) {
            this.nativeSDK = nativeSDK;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(AppViewModel.class)) {
                return (T) new AppViewModel(nativeSDK);
            }
            throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
        }
    }
}
