data class OpenAIRequest(
    val model: String,
    val messages: List<RequestMessage>,
    val max_tokens: Int,
    val temperature: Double,
    val top_p: Double
)

data class RequestMessage(
    val role: String,
    val content: String
)
