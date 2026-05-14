package io.sc.eppCordova.utils

object Constants {
    const val BASE_URL = "https://epeek.mahabhumi.gov.in/api/" // Mocked in interceptor
    /*
     * BACKEND_BASE_URL — krishi-core backend
     *
     * Emulator mode:   http://10.0.2.2:5000/
     * Physical device: http://<YOUR_LOCAL_IP>:5000/
     *   (e.g. http://192.168.137.32:5000/)
     *
     * Switch between the two depending on your test setup.
     */

    const val BACKEND_BASE_URL = "http://192.168.137.32:5000/"
    const val PREFS_NAME = "user_prefs"
    const val PREF_TOKEN = "USER_TOKEN"
    const val PREF_USER_ID = "USER_ID"
    const val PREF_NAME = "USER_NAME"
    const val PREF_MOBILE = "USER_MOBILE"
    const val PREF_LANGUAGE = "LANGUAGE"
    
    const val DB_NAME = "epeek_database"
}
