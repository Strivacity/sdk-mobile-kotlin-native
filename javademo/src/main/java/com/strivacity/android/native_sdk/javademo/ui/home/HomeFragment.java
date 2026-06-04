package com.strivacity.android.native_sdk.javademo.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.strivacity.android.native_sdk.javademo.R;
import com.strivacity.android.native_sdk.javademo.databinding.FragmentHomeBinding;
import com.strivacity.android.native_sdk.javademo.ui.AppViewModel;
import com.strivacity.android.native_sdk.javademo.ui.login.LoginFragment;
import com.strivacity.android.native_sdk.javademo.ui.profile.ProfileFragment;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // AppViewModel is pre-created by JavaDemoActivity — no SDK reference needed here.
        AppViewModel appViewModel =
                new ViewModelProvider(requireActivity()).get(AppViewModel.class);

        appViewModel
                .getUser()
                .observe(
                        getViewLifecycleOwner(),
                        user -> {
                            if (user != null) {
                                requireActivity()
                                        .getSupportFragmentManager()
                                        .beginTransaction()
                                        .replace(R.id.fragment_container, new ProfileFragment())
                                        .commit();
                            } else {
                                binding.loginButton.setVisibility(View.VISIBLE);
                            }
                        });

        binding.loginButton.setOnClickListener(
                v ->
                        requireActivity()
                                .getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, new LoginFragment())
                                .addToBackStack(null)
                                .commit());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
