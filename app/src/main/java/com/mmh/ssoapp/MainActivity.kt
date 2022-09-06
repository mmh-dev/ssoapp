package com.mmh.ssoapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import com.mmh.ssoapp.databinding.ActivityMainBinding
import net.openid.appauth.*
import org.json.JSONException


class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private var authState: AuthState = AuthState()
    private lateinit var authService: AuthorizationService
    private lateinit var authServiceConfig: AuthorizationServiceConfiguration
    private lateinit var serviceConfig: AuthorizationServiceConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        restoreAuthState()

        binding.tokenTv.setOnClickListener {
            callWithAppAuth()
        }
    }

    private fun restoreAuthState() {
        val jsonString = applicationContext.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE).getString(AUTH_STATE_SHARED_PREF, null)

        if (jsonString != null && !TextUtils.isEmpty(jsonString)) {
            try {
                authState = AuthState.jsonDeserialize(jsonString)

            } catch (jsonException: JSONException) {
            }
        }
    }

    private fun saveAuthState() {
        applicationContext.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE).edit()
            .putString(AUTH_STATE_SHARED_PREF, authState.jsonSerializeString()).apply()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == APP_AUTH_CODE) {
            val authResponse = AuthorizationResponse.fromIntent(data!!)
            val authError = AuthorizationException.fromIntent(data)
            authState = AuthState(authResponse, authError)
            val tokenExchangeRequest = authResponse?.createTokenExchangeRequest()
            tokenExchangeRequest?.let {
                authService.performTokenRequest(it) { response, exception ->
                    if (exception != null) {
                        authState = AuthState()
                    } else {
                        if (response != null) {
                            binding.tokenTv.text = response.refreshToken
                            authState.update(response, exception)
                        }
                    }
                    saveAuthState()
                }
            }

        }
    }

    private fun callWithAppAuth() {
        serviceConfig = AuthorizationServiceConfiguration(Uri.parse(AUTH_URL), Uri.parse(TOKEN_URL))

        val redirectUri = Uri.parse(CALLBACK_URL)
        val builder = AuthorizationRequest.Builder(serviceConfig, CLIENT_ID, ResponseTypeValues.CODE, redirectUri)

        val authRequest = builder.build()

        authService = AuthorizationService(this)
        val authIntent = authService.getAuthorizationRequestIntent(authRequest)
        startActivityForResult(authIntent, APP_AUTH_CODE)
    }
}