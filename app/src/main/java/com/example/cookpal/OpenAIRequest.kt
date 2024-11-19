data class OpenAIRequest(
    val model: String = "gpt-4o-mini",
    val prompt: String,
    val max_tokens: Int = 800,
    val temperature: Double = 0.7
)
