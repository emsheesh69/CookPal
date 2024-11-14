import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Header

interface OpenAIService {
    @POST("v1/chat/completions")
    fun getRecipeSuggestions(
        @Header("Authorization") apiKey: String,
        @Body request: OpenAIRequest
    ): Call<OpenAIResponse>
}
