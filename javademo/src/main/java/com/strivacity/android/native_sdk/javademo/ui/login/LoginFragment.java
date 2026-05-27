package com.strivacity.android.native_sdk.javademo.ui.login;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import com.strivacity.android.native_sdk.javademo.R;
import com.strivacity.android.native_sdk.javademo.databinding.FragmentLoginBinding;
import com.strivacity.android.native_sdk.javademo.ui.AppViewModel;
import com.strivacity.android.native_sdk.javademo.ui.login.my_app_screen.IdentifierFragment;
import com.strivacity.android.native_sdk.javademo.ui.login.my_app_screen.PasskeyEnrollFragment;
import com.strivacity.android.native_sdk.javademo.ui.login.my_app_screen.PasswordFragment;
import com.strivacity.android.native_sdk.javademo.ui.login.my_app_screen.RegistrationFragment;
import java.util.Objects;
import java.util.Optional;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private LoginViewModel viewModel;

    private boolean loginCompleted = false;
    private ScreenFragment lastDisplayedFragment;
    private String lastDisplayedScreen;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            // The login flow cannot survive process death — the SDK state is gone.
            // Remove any child fragments the system restored so they don't render with a null
            // screen.
            for (Fragment f : getChildFragmentManager().getFragments()) {
                getChildFragmentManager().beginTransaction().remove(f).commitNow();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // AppViewModel is pre-created by JavaDemoActivity — get the SDK from it.
        AppViewModel appViewModel =
                new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        viewModel =
                new ViewModelProvider(this, appViewModel.createLoginViewModelFactory())
                        .get(LoginViewModel.class);

        viewModel
                .getCurrentScreen()
                .observe(
                        getViewLifecycleOwner(),
                        screen -> {
                            if (screen == null) return;
                            final String screenToDisplay =
                                    Objects.requireNonNull(screen.getScreen());
                            if (screenToDisplay.equals(lastDisplayedScreen)) {
                                // update
                                lastDisplayedFragment.refresh(screen);
                            } else {
                                ScreenFragment fragment;
                                // render
                                switch (screenToDisplay) {
                                    case "identification":
                                        fragment = new IdentifierFragment();
                                        break;
                                    case "password":
                                        fragment = new PasswordFragment();
                                        break;
                                    case "passkeyEnroll":
                                        fragment = new PasskeyEnrollFragment();
                                        break;
                                    case "registration":
                                        fragment = new RegistrationFragment();
                                        break;
                                    default:
                                        getChildFragmentManager()
                                                .beginTransaction()
                                                .replace(
                                                        R.id.login_fragment_container,
                                                        ErrorFragment.forUnknownScreen(screen))
                                                .commit();
                                        return;
                                }
                                fragment.setScreen(screen);
                                FragmentManager fm = getChildFragmentManager();
                                fm.beginTransaction()
                                        .replace(R.id.login_fragment_container, fragment)
                                        .commit();
                                lastDisplayedFragment = fragment;
                                lastDisplayedScreen = screenToDisplay;
                            }
                        });

        viewModel
                .getError()
                .observe(
                        getViewLifecycleOwner(),
                        err ->
                                Optional.ofNullable(err)
                                        .ifPresent(
                                                s ->
                                                        Toast.makeText(
                                                                        getContext(),
                                                                        s,
                                                                        Toast.LENGTH_SHORT)
                                                                .show()));

        viewModel
                .getSubmitError()
                .observe(
                        getViewLifecycleOwner(),
                        throwable -> {
                            FragmentManager fm = getChildFragmentManager();
                            if (throwable != null) {
                                fm.beginTransaction()
                                        .replace(
                                                R.id.login_fragment_container,
                                                ErrorFragment.from(throwable))
                                        .commit();
                            } else if (lastDisplayedFragment != null) {
                                fm.beginTransaction()
                                        .replace(
                                                R.id.login_fragment_container,
                                                lastDisplayedFragment)
                                        .commit();
                            }
                        });

        viewModel
                .getOnLoginComplete()
                .observe(
                        getViewLifecycleOwner(),
                        ignored -> {
                            loginCompleted = true;
                            requireActivity().getSupportFragmentManager().popBackStack();
                        });
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!loginCompleted && !viewModel.isRedirectExpected()) {
            viewModel.cancelFlow();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        viewModel.startLogin(requireActivity());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
