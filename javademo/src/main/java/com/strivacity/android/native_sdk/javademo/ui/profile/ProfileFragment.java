package com.strivacity.android.native_sdk.javademo.ui.profile;

import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.strivacity.android.native_sdk.javademo.R;
import com.strivacity.android.native_sdk.javademo.databinding.FragmentProfileBinding;
import com.strivacity.android.native_sdk.javademo.ui.AppViewModel;
import com.strivacity.android.native_sdk.javademo.ui.home.HomeFragment;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Session state — Activity-scoped, shared across all fragments.
        AppViewModel appViewModel =
                new ViewModelProvider(requireActivity()).get(AppViewModel.class);

        // Profile screen actions — Fragment-scoped, survives configuration changes.
        ProfileViewModel viewModel =
                new ViewModelProvider(this, appViewModel.createProfileViewModelFactory())
                        .get(ProfileViewModel.class);

        // User display is driven by AppViewModel — the single source of truth for session state.
        appViewModel
                .getUser()
                .observe(
                        getViewLifecycleOwner(),
                        user -> {
                            if (user == null) {
                                requireActivity()
                                        .getSupportFragmentManager()
                                        .beginTransaction()
                                        .replace(R.id.fragment_container, new HomeFragment())
                                        .commit();
                                return;
                            }

                            binding.profileDisplayName.setText(user.getDisplayName());

                            String initial =
                                    user.getDisplayName().trim().isEmpty()
                                            ? "?"
                                            : String.valueOf(user.getDisplayName().trim().charAt(0))
                                                    .toUpperCase(Locale.getDefault());
                            binding.profileAvatar.setText(initial);

                            if (user.getEmail() != null) {
                                binding.profileEmail.setText(user.getEmail());
                                binding.profileEmailRow.setVisibility(View.VISIBLE);
                            } else {
                                binding.profileEmailRow.setVisibility(View.GONE);
                            }

                            binding.profileSubject.setText(user.getUsername());
                        });

        viewModel
                .getIdToken()
                .observe(
                        getViewLifecycleOwner(),
                        idToken -> {
                            if (idToken == null) return;
                            try {
                                String payload = idToken.split("\\.")[1];
                                String decoded =
                                        new String(
                                                Base64.decode(
                                                        payload,
                                                        Base64.URL_SAFE | Base64.NO_PADDING));
                                binding.profileClaimsValue.setText(formatJson(decoded));
                            } catch (Exception e) {
                                binding.profileClaimsValue.setText(idToken);
                            }
                        });

        viewModel
                .getAccessToken()
                .thenAcceptAsync(
                        token ->
                                binding.profileAccessTokenValue.setText(token != null ? token : ""),
                        ContextCompat.getMainExecutor(requireContext()))
                .exceptionally(
                        throwable -> {
                            Toast.makeText(
                                            getContext(),
                                            "Failed to fetch access token: "
                                                    + throwable.getMessage(),
                                            Toast.LENGTH_SHORT)
                                    .show();
                            return null;
                        });

        binding.profileRevokeButton.setOnClickListener(v -> viewModel.revoke());
        binding.profileLogoutButton.setOnClickListener(v -> viewModel.logout());
    }

    private String formatJson(String text) {
        StringBuilder json = new StringBuilder();
        String indent = "";
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '{':
                case '[':
                    json.append("\n").append(indent).append(c).append("\n");
                    indent += "\t";
                    json.append(indent);
                    break;
                case '}':
                case ']':
                    indent = indent.replaceFirst("\t", "");
                    json.append("\n").append(indent).append(c);
                    break;
                case ',':
                    json.append(c).append("\n").append(indent);
                    break;
                default:
                    json.append(c);
            }
        }
        return json.toString();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
