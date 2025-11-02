package utar.edu.my.fyp.petschedule.adapters

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth

object UserSessionManager {
    private const val PREF_NAME = "pet_schedule_user_session"
    private const val KEY_USER_ID = "current_user_id"
    private const val KEY_REMEMBER_ME = "remember_me"
    private const val KEY_SAVED_EMAIL = "saved_email"
    private const val KEY_SAVED_PASSWORD = "saved_password"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun setCurrentUserId(context: Context, userId: String) {
        getSharedPreferences(context).edit()
            .putString(KEY_USER_ID, userId)
            .apply()
    }

    fun getCurrentUserId(context: Context): String? {
        // First try to get from SharedPreferences
        val storedUserId = getSharedPreferences(context).getString(KEY_USER_ID, null)

        // If not found, try to get from Firebase Auth
        if (storedUserId == null) {
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            firebaseUser?.uid?.let { uid ->
                // Save it for future use
                setCurrentUserId(context, uid)
                return uid
            }
        }

        return storedUserId
    }

    fun clearCurrentUser(context: Context) {
        getSharedPreferences(context).edit()
            .remove(KEY_USER_ID)
            .apply()
    }

    fun isUserLoggedIn(context: Context): Boolean {
        return getCurrentUserId(context) != null
    }

    // Remember Me functionality
    fun setRememberMe(context: Context, remember: Boolean, email: String = "", password: String = "") {
        val editor = getSharedPreferences(context).edit()
        editor.putBoolean(KEY_REMEMBER_ME, remember)

        if (remember) {
            editor.putString(KEY_SAVED_EMAIL, email)
            editor.putString(KEY_SAVED_PASSWORD, password)
        } else {
            editor.remove(KEY_SAVED_EMAIL)
            editor.remove(KEY_SAVED_PASSWORD)
        }

        editor.apply()
    }

    fun isRememberMeEnabled(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(KEY_REMEMBER_ME, false)
    }

    fun getSavedEmail(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_SAVED_EMAIL, null)
    }

    fun getSavedPassword(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_SAVED_PASSWORD, null)
    }

    fun clearRememberedCredentials(context: Context) {
        getSharedPreferences(context).edit()
            .remove(KEY_REMEMBER_ME)
            .remove(KEY_SAVED_EMAIL)
            .remove(KEY_SAVED_PASSWORD)
            .apply()
    }
}