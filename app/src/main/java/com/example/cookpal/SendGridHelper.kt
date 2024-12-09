import android.util.Log
import com.google.firebase.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import okhttp3.RequestBody

object SendGridHelper {

    // Function to send OTP email
    fun sendOtpEmail(recipientEmail: String, otp: String): Boolean {
        val client = OkHttpClient()

        val json = JSONObject()
        json.put(
            "personalizations",
            JSONArray().put(
                JSONObject().put(
                    "to",
                    JSONArray().put(JSONObject().put("email", recipientEmail))
                )
            )
        )
        json.put(
            "from",
            JSONObject().put("email", "thecookpalapp@gmail.com")
        )  // Use the app's email
        json.put("subject", "Your OTP Code")
        json.put(
            "content",
            JSONArray().put(JSONObject().put("type", "text/plain").put("value", "Your OTP is $otp"))
        )

        val body = RequestBody.create("application/json".toMediaType(), json.toString())

        val request = Request.Builder()
            .url("https://api.sendgrid.com/v3/mail/send")
            .addHeader(
                "Authorization",
                "Bearer API_KEY"
            )
            .post(body)
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Log.d("SendGridHelper", "OTP sent successfully")
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

    fun sendRatingEmail(userEmail: String, rating: Int, feedback: String): Boolean {
        val client = OkHttpClient()

        val json = JSONObject()
        json.put("personalizations", JSONArray().put(JSONObject().put("to", JSONArray().put(JSONObject().put("email", "thecookpalapp@gmail.com")))))
        json.put("from", JSONObject().put("email", "thecookpalapp@gmail.com"))  // Use your verified email
        json.put("reply_to", JSONObject().put("email", userEmail))  // Set the reply-to as the user's email
        json.put("subject", "User Rating Feedback")
        json.put("content", JSONArray().put(JSONObject().put("type", "text/plain").put("value", "Rating: $rating stars\nFeedback: $feedback")))

        val body = RequestBody.create("application/json".toMediaType(), json.toString())

        val request = Request.Builder()
            .url("https://api.sendgrid.com/v3/mail/send")
            .addHeader("Authorization", "Bearer API_KEY")
            .post(body)
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Log.d("SendGridHelper", "Rating feedback sent successfully to the app's email")
                return true
            } else {
                Log.e("SendGridHelper", "Error sending feedback. Response Code: ${response.code}")
                Log.e("SendGridHelper", "Response Body: ${response.body?.string()}")
                return false
            }
        } catch (e: IOException) {
            Log.e("SendGridHelper", "IOException while sending feedback: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
}