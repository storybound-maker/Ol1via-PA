package com.liv.ol1viapa

import android.app.Activity
import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

class LeauAuth(context: Context) {
    private val appContext = context.applicationContext
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    fun currentUser(): FirebaseUser? = if (FirebaseConfig.initialize(appContext)) auth.currentUser else null

    fun signUp(email: String, password: String, callback: (Result<FirebaseUser>) -> Unit) {
        if (!ready(callback)) return
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    callback(Result.success(user))
                } else {
                    callback(Result.failure(IllegalStateException("Firebase created the account without returning a user.")))
                }
            }
            .addOnFailureListener { error -> callback(Result.failure(error)) }
    }

    fun signIn(email: String, password: String, callback: (Result<FirebaseUser>) -> Unit) {
        if (!ready(callback)) return
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    callback(Result.success(user))
                } else {
                    callback(Result.failure(IllegalStateException("Firebase signed in without returning a user.")))
                }
            }
            .addOnFailureListener { error -> callback(Result.failure(error)) }
    }

    fun sendPasswordReset(email: String, callback: (Result<Unit>) -> Unit) {
        if (!ready(callback)) return
        auth.sendPasswordResetEmail(email.trim())
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { error -> callback(Result.failure(error)) }
    }

    fun signOut() {
        if (FirebaseConfig.initialize(appContext)) auth.signOut()
    }

    fun deleteAccount(callback: (Result<Unit>) -> Unit) {
        if (!ready(callback)) return
        val user = auth.currentUser
            ?: return callback(Result.failure(IllegalStateException("No signed-in account.")))
        user.delete()
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { error -> callback(Result.failure(error)) }
    }

    fun googleSignInIntent(activity: Activity): android.content.Intent? {
        if (!FirebaseConfig.initialize(appContext)) return null
        val clientId = activity.getString(com.liv.ol1viapa.R.string.firebase_web_client_id)
        if (clientId.isBlank() || clientId.startsWith("REPLACE_")) return null

        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(clientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(activity, options).signInIntent
    }

    fun finishGoogleSignIn(data: android.content.Intent?, callback: (Result<FirebaseUser>) -> Unit) {
        if (!ready(callback)) return
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                callback(Result.failure(IllegalStateException("Google Sign-In did not return an ID token.")))
                return
            }

            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnSuccessListener { result ->
                    val user = result.user
                    if (user != null) {
                        callback(Result.success(user))
                    } else {
                        callback(Result.failure(IllegalStateException("Firebase signed in without returning a user.")))
                    }
                }
                .addOnFailureListener { error -> callback(Result.failure(error)) }
        } catch (e: Exception) {
            callback(Result.failure(e))
        }
    }

    fun linkCurrentUserWithPassword(password: String, callback: (Result<Unit>) -> Unit) {
        val user = currentUser()
            ?: return callback(Result.failure(IllegalStateException("No signed-in account.")))
        val email = user.email
            ?: return callback(Result.failure(IllegalStateException("This account has no email.")))
        user.linkWithCredential(EmailAuthProvider.getCredential(email, password))
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { error -> callback(Result.failure(error)) }
    }

    private fun <T> ready(callback: (Result<T>) -> Unit): Boolean {
        if (FirebaseConfig.initialize(appContext)) return true
        callback(Result.failure(IllegalStateException("Firebase is not configured. Add the Leau Firebase project values to gradle.properties.")))
        return false
    }
}
