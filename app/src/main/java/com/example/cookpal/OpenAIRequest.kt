data class OpenAIRequest(
    val model: String,
    val messages: List<RequestMessage>,
    val max_tokens: Int,
    val temperature: Float,
    val top_p: Float
)

data class RequestMessage(
    val role: String,
    val content: String
)
