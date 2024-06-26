package com.woodgrove.android.ui.signup

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.microsoft.identity.nativeauth.UserAttributes
import com.microsoft.identity.nativeauth.statemachine.errors.SignUpError
import com.microsoft.identity.nativeauth.statemachine.results.SignUpResult
import com.microsoft.identity.nativeauth.statemachine.states.SignUpCodeRequiredState
import com.woodgrove.android.R
import com.woodgrove.android.databinding.FragmentSignupEmailPasswordBinding
import com.woodgrove.android.ui.login.LoginActivity
import com.woodgrove.android.utils.AuthClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class SignupEmailPasswordFragment : Fragment() {

    private lateinit var binding: FragmentSignupEmailPasswordBinding
    private val authClient = AuthClient.getAuthClient()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSignupEmailPasswordBinding.inflate(inflater, container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeListeners()
    }

    private fun initializeListeners() {
        binding.signupEmailPasswordNext.setOnClickListener {
            showLoading()
            startSignup()
        }
    }

    private fun showLoading() {
        binding.signupEmailPasswordNext.text = null
        binding.signupNextLoader.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.signupEmailPasswordNext.text = getString(R.string.next_button)
        binding.signupNextLoader.visibility = View.GONE
    }

    private fun startSignup() {
        CoroutineScope(Dispatchers.Main).launch {

            val email = getEmailFieldValue()
            val password = getPasswordFieldValue()
            val name = getNameFieldValue()

            val attributes = UserAttributes.Builder
                .displayName(name)
                .build()

            val actionResult = authClient.signUp(
                username = email,
                password = password,
                attributes = attributes
            )

            when (actionResult) {
                is SignUpResult.CodeRequired -> {
                    hideLoading()
                    navigateNext(actionResult.nextState)
                }
                is SignUpError -> {
                    hideLoading()
                    when {
                        actionResult.isInvalidPassword() ->  {
                            showInvalidPasswordError()
                        }
                        actionResult.isInvalidUsername() -> {
                            showInvalidEmailError()
                        }
                        actionResult.isUserAlreadyExists() -> {
                            showUserAlreadyExistsError(email)
                        } else -> {
                            showGeneralError()
                        }
                    }
                }
                else -> {
                    showGeneralError()
                }
            }
        }
    }

    private fun showInvalidEmailError() {
        binding.emailFieldLayout.error = getString(R.string.invalid_email_error)
    }

    private fun showInvalidPasswordError() {
        binding.passwordFieldLayout.error = getString(R.string.invalid_password_error)
    }

    private fun showUserAlreadyExistsError(username: String) {
        val title = getString(R.string.error_title)
        val message = getString(R.string.user_exists_error_message)
        val builder = AlertDialog.Builder(requireContext())
        val alertDialog = builder
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.go_to_login) { dialog, id ->
                navigateToLogin(username)
            }
            .setNegativeButton(R.string.dismiss) { dialog, id ->
                dialog.dismiss()
            }
            .create()

        alertDialog.show()
    }

    private fun showGeneralError() {
        val title = getString(R.string.error_title)
        val message = getString(R.string.general_error_message)
        showDialog(title, message)
    }

    private fun showDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(requireContext())
        val alertDialog = builder
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton(R.string.dismiss) { dialog, id ->
                dialog.dismiss()
            }
            .create()

        alertDialog.show()
    }

    private fun navigateToLogin(username: String) {
        requireActivity().let { parentActivity ->
            // Start new activity
            startActivity(LoginActivity.getStartIntent(context = requireActivity(), username = username))

            val signUpActivity = try {
                parentActivity as SignupActivity
            } catch (e: java.lang.ClassCastException) {
                return
            }
            // Close existing
            signUpActivity.close()
            parentActivity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
    }

    private fun navigateNext(authState: SignUpCodeRequiredState) {
        val newFragment = SignupCodeFragment.getNewInstance(authState)

        val localFragmentManager = parentFragmentManager
        localFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right_full,
                R.anim.slide_out_left_full
            )
            .replace(R.id.signup_fragmentContainer, newFragment, tag)
            .commitAllowingStateLoss()

        localFragmentManager.executePendingTransactions()
    }

    private fun getEmailFieldValue() = binding.signupEmailField.text.toString()

    private fun getPasswordFieldValue(): CharArray {
        val password = CharArray(binding.signupPasswordField.length())
        binding.signupPasswordField.text?.getChars(0, binding.signupPasswordField.length(), password, 0)
        return password
    }

    private fun getNameFieldValue() = binding.signupNameField.text.toString()
}