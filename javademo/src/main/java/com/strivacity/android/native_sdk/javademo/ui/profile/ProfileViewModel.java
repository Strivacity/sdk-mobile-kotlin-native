package com.strivacity.android.native_sdk.javademo.ui.profile;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.strivacity.android.native_sdk.compat.NativeSDKJava;
import java.util.concurrent.CompletableFuture;

/** ViewModel for the Profile screen. Owns profile-specific data and session actions. */
public class ProfileViewModel extends ViewModel {

    private final LiveData<String> idToken;
    private final NativeSDKJava nativeSDK;

    public ProfileViewModel(@NonNull NativeSDKJava nativeSDK) {
        this.nativeSDK = nativeSDK;
        this.idToken =
                Transformations.map(
                        nativeSDK.getProfileLiveData(),
                        profile -> profile != null ? profile.getIdToken() : null);
    }

    public LiveData<String> getIdToken() {
        return idToken;
    }

    public CompletableFuture<String> getAccessToken() {
        return nativeSDK.getAccessToken();
    }

    public void logout() {
        nativeSDK.logout();
    }

    public void revoke() {
        nativeSDK.revoke();
    }

    public static class ProfileViewModelFactory implements ViewModelProvider.Factory {

        private final NativeSDKJava nativeSDK;

        public ProfileViewModelFactory(NativeSDKJava nativeSDK) {
            this.nativeSDK = nativeSDK;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(ProfileViewModel.class)) {
                return (T) new ProfileViewModel(nativeSDK);
            }
            throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
        }
    }
}
