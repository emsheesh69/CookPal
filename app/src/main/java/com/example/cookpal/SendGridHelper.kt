import android.util.Log
import com.google.firebase.BuildConfig
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import okhttp3.RequestBody

object SendGridHelper {

    // Function to send OTP email
    fun sendOtpEmail(email: String, otp: String): Boolean {
        val client = OkHttpClient()

        val json = JSONObject()
        json.put("personalizations", JSONArray().put(JSONObject().put("to", JSONArray().put(JSONObject().put("email", email)))))
        json.put("from", JSONObject().put("email", "thecookpalapp@gmail.com"))
        json.put("subject", "Your OTP Code")
        json.put("content", JSONArray().put(JSONObject().put("type", "text/plain").put("value", "Your OTP is $otp")))

        val body = RequestBody.create("application/json".toMediaType(), json.toString())

        val request = Request.Builder()
            .url("https://api.sendgrid.com/v3/mail/send")
            .addHeader("Authorization", "Bearer PUT_API_KEY_HERE")
            .post(body)
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                return true
            } else {
                Log.e("SendGridHelper", "Error sending OTP. Response Code: ${response.code}")
                Log.e("SendGridHelper", "Response Body: ${response.body?.string()}")
                return false
            }
        } catch (e: IOException) {
            Log.e("SendGridHelper", "IOException while sending OTP: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
}