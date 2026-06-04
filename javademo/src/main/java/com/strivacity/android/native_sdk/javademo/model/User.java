package com.strivacity.android.native_sdk.javademo.model;

import androidx.annotation.Nullable;

public class User {
    private final String username;
    @Nullable private final String givenName;
    @Nullable private final String familyName;
    @Nullable private final String email;

    public User(
            String username,
            @Nullable String givenName,
            @Nullable String familyName,
            @Nullable String email) {
        this.username = username;
        this.givenName = givenName;
        this.familyName = familyName;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    @Nullable
    public String getGivenName() {
        return givenName;
    }

    @Nullable
    public String getFamilyName() {
        return familyName;
    }

    @Nullable
    public String getEmail() {
        return email;
    }

    /** Returns a display name preferring given + family name, falling back to username. */
    public String getDisplayName() {
        if (givenName != null && familyName != null) {
            return givenName + " " + familyName;
        } else if (givenName != null) {
            return givenName;
        }
        return username;
    }
}
