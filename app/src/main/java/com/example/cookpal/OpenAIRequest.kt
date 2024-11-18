data class OpenAIRequest(
    val model: String = "gpt-3.5-turbo",
    val prompt: String,
    val max_tokens: Int = 150,
    val temperature: Double = 0.7
)
