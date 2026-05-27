package com.strivacity.android.native_sdk.javademo.ui.login;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.strivacity.android.native_sdk.javademo.databinding.FragmentErrorBinding;
import com.strivacity.android.native_sdk.javademo.exceptions.UnknownScreenException;
import com.strivacity.android.native_sdk.render.models.Screen;

public class ErrorFragment extends Fragment {

    private static final String ARG_MESSAGE = "errorMessage";
    private static final String ARG_CAUSE = "errorCause";
    private static final String ARG_FALLBACK_URL = "fallbackUrl";

    private LoginViewModel viewModel;

    private FragmentErrorBinding binding;

    /**
     * Creates an {@code ErrorFragment} for a screen type that is not recognised by this app.
     *
     * @param screen the raw screen identifier returned by the backend
     */
    public static ErrorFragment forUnknownScreen(@NonNull Screen screen) {
        return from(new UnknownScreenException(screen));
    }

    /**
     * Creates an {@code ErrorFragment} populated with details from the given throwable.
     *
     * @param throwable the error that triggered this screen; must not be null
     */
    public static ErrorFragment from(@NonNull Throwable throwable) {
        Log.w("ErrorFragment", "Displaying error to user", throwable);
        Bundle args = new Bundle();
        args.putString(ARG_MESSAGE, throwable.getLocalizedMessage());
        args.putString(ARG_CAUSE, throwable.getClass().getSimpleName());
        if (throwable instanceof UnknownScreenException) {
            args.putString(ARG_FALLBACK_URL, ((UnknownScreenException) throwable).getFallbackUrl());
        }
        ErrorFragment fragment = new ErrorFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentErrorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireParentFragment()).get(LoginViewModel.class);

        Bundle args = requireArguments();
        String cause = args.getString(ARG_CAUSE, "");
        String message = args.getString(ARG_MESSAGE, "");
        String fallbackUrl = args.getString(ARG_FALLBACK_URL, null);
        if (fallbackUrl != null) {
            binding.triggerFallbackButton.setOnClickListener(
                    ignored -> viewModel.fallbackToHostedExperience());
            binding.triggerFallbackButton.setVisibility(View.VISIBLE);
        }
        binding.errorMessage.setText(cause.isEmpty() ? message : cause + ": " + message);
        binding.tryAgainButton.setOnClickListener(v -> viewModel.setSubmitError(null));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
