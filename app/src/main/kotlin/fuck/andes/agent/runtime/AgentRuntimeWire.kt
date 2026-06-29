package fuck.andes.agent.runtime

import android.os.Bundle
import android.content.ComponentName
import android.content.Intent
import fuck.andes.agent.model.AgentModelClient

/**
 * AgentRuntime 跨进程通信协议。
 *
 * 入口进程（当前是小布 Hook）通过 bind + Messenger 与模块自身进程的
 * [AgentRuntimeService] 通信：发送一次运行请求，接收事件流和最终结果。
 *
 * 不引入 AIDL：消息体只用 [Bundle]（Int/Boolean/String/ArrayList），跨进程传递无序列化门槛。
 */
internal object AgentRuntimeWire {
    /** bind 获取服务端 Messenger 的 Intent action。 */
    const val ACTION_BIND = "fuck.andes.agent.runtime.BIND"

    // Messenger.what
    /** client -> service：开始一次 Agent 运行，[Message.replyTo] 携带 client Messenger。 */
    const val MSG_START_RUN = 1

    /** service -> client：推送一个 [AgentEvent]。 */
    const val MSG_EVENT = 2

    /** service -> client：最终结果。 */
    const val MSG_RESULT = 3

    /** client -> service：取消当前运行。 */
    const val MSG_CANCEL = 4

    /**
     * 允许向 [AgentRuntimeService] 发起运行的宿主包名集合。当前只开放小布 Hook 入口，
     * 其余调用方发送的 Messenger 消息会被忽略。
     */
    val ALLOWED_CALLER_PACKAGES = setOf("com.heytap.speechassist")

    private const val MODULE_PACKAGE = "fuck.andes"
    private const val SERVICE_CLASS = "fuck.andes.agent.runtime.AgentRuntimeService"

    private const val KEY_TYPE = "type"
    private const val KEY_PROMPT = "prompt"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_MODEL = "model"
    private const val KEY_SYSTEM_PROMPT = "system_prompt"
    private const val KEY_TERMINAL_TOOLS = "terminal_tools"
    private const val KEY_IMAGES = "images"
    private const val KEY_DATA_URL = "data_url"
    private const val KEY_MIME_TYPE = "mime_type"
    private const val KEY_BYTES = "bytes"
    private const val KEY_WIDTH = "width"
    private const val KEY_HEIGHT = "height"
    private const val KEY_SOURCE = "source"
    private const val KEY_OK = "ok"
    private const val KEY_CONTENT = "content"
    private const val KEY_ERROR = "error"

    data class RunRequest(
        val prompt: String,
        val config: AgentModelClient.ModelConfig,
        val images: List<AgentModelClient.ModelImage>
    )

    data class RunResult(
        val ok: Boolean,
        val content: String,
        val error: String? = null
    )

    fun serviceIntent(): Intent =
        Intent(ACTION_BIND).setComponent(ComponentName(MODULE_PACKAGE, SERVICE_CLASS))

    fun toBundle(request: RunRequest): Bundle = Bundle().apply {
        putString(KEY_PROMPT, request.prompt)
        putString(KEY_BASE_URL, request.config.baseUrl)
        putString(KEY_API_KEY, request.config.apiKey)
        putString(KEY_MODEL, request.config.model)
        putString(KEY_SYSTEM_PROMPT, request.config.systemPrompt)
        putBoolean(KEY_TERMINAL_TOOLS, request.config.terminalTools)
        putParcelableArrayList(
            KEY_IMAGES,
            ArrayList(request.images.map { image ->
                Bundle().apply {
                    putString(KEY_DATA_URL, image.dataUrl)
                    putString(KEY_MIME_TYPE, image.mimeType)
                    putInt(KEY_BYTES, image.bytes)
                    image.width?.let { putInt(KEY_WIDTH, it) }
                    image.height?.let { putInt(KEY_HEIGHT, it) }
                    putString(KEY_SOURCE, image.source)
                }
            })
        )
    }

    fun runRequestFromBundle(bundle: Bundle): RunRequest =
        RunRequest(
            prompt = bundle.getString(KEY_PROMPT).orEmpty(),
            config = AgentModelClient.ModelConfig(
                baseUrl = bundle.getString(KEY_BASE_URL).orEmpty(),
                apiKey = bundle.getString(KEY_API_KEY).orEmpty(),
                model = bundle.getString(KEY_MODEL).orEmpty(),
                systemPrompt = bundle.getString(KEY_SYSTEM_PROMPT).orEmpty(),
                terminalTools = bundle.getBoolean(KEY_TERMINAL_TOOLS)
            ),
            images = bundle.getParcelableArrayList(KEY_IMAGES, Bundle::class.java).orEmpty().map { image ->
                AgentModelClient.ModelImage(
                    dataUrl = image.getString(KEY_DATA_URL).orEmpty(),
                    mimeType = image.getString(KEY_MIME_TYPE).orEmpty(),
                    bytes = image.getInt(KEY_BYTES),
                    width = image.optionalInt(KEY_WIDTH),
                    height = image.optionalInt(KEY_HEIGHT),
                    source = image.getString(KEY_SOURCE).orEmpty()
                )
            }
        )

    fun toBundle(result: RunResult): Bundle = Bundle().apply {
        putBoolean(KEY_OK, result.ok)
        putString(KEY_CONTENT, result.content)
        putString(KEY_ERROR, result.error)
    }

    fun runResultFromBundle(bundle: Bundle): RunResult =
        RunResult(
            ok = bundle.getBoolean(KEY_OK),
            content = bundle.getString(KEY_CONTENT).orEmpty(),
            error = bundle.getString(KEY_ERROR)
        )

    /** 将 [AgentEvent] 打包为可跨进程传递的 [Bundle]。 */
    fun eventToBundle(event: AgentEvent): Bundle = Bundle().apply {
        when (event) {
            is AgentEvent.RunStarted -> {
                putString(KEY_TYPE, "run_started")
                putInt("initial_images", event.initialImages)
                putInt("initial_image_bytes", event.initialImageBytes)
                putInt("tool_count", event.toolCount)
                putBoolean("terminal_tools", event.terminalTools)
            }

            is AgentEvent.RoundStarted -> {
                putString(KEY_TYPE, "round_started")
                putInt("round", event.round)
                putInt("message_count", event.messageCount)
            }

            is AgentEvent.ProviderRequestStarted -> {
                putString(KEY_TYPE, "provider_request_started")
                putInt("round", event.round)
            }

            is AgentEvent.ProviderResponseStarted -> {
                putString(KEY_TYPE, "provider_response_started")
                putInt("round", event.round)
                putInt("http_code", event.httpCode)
            }

            is AgentEvent.AssistantTextDelta -> {
                putString(KEY_TYPE, "assistant_text_delta")
                putInt("round", event.round)
                putInt("delta_chars", event.deltaChars)
            }

            is AgentEvent.ProviderToolCallDelta -> {
                putString(KEY_TYPE, "provider_tool_call_delta")
                putInt("round", event.round)
                putInt("index", event.index)
                putString("name", event.name)
                putInt("arguments_chars", event.argumentsChars)
            }

            is AgentEvent.AssistantReceived -> {
                putString(KEY_TYPE, "assistant_received")
                putInt("round", event.round)
                putInt("content_chars", event.contentChars)
                putStringArrayList("tool_names", ArrayList(event.toolNames))
            }

            is AgentEvent.ToolStarted -> {
                putString(KEY_TYPE, "tool_started")
                putInt("round", event.round)
                putString("name", event.name)
                putString("args_preview", event.argsPreview)
            }

            is AgentEvent.ToolFinished -> {
                putString(KEY_TYPE, "tool_finished")
                putInt("round", event.round)
                putString("name", event.name)
                putString("result_summary", event.resultSummary)
                putInt("image_count", event.imageCount)
                putInt("image_bytes", event.imageBytes)
            }

            is AgentEvent.ToolImagesAttached -> {
                putString(KEY_TYPE, "tool_images_attached")
                putInt("round", event.round)
                putString("tool_name", event.toolName)
                putInt("image_count", event.imageCount)
                putInt("image_bytes", event.imageBytes)
            }

            is AgentEvent.RunFinished -> {
                putString(KEY_TYPE, "run_finished")
                putInt("round", event.round)
                putInt("content_chars", event.contentChars)
            }

            is AgentEvent.RunFailed -> {
                putString(KEY_TYPE, "run_failed")
                putString("reason", event.reason)
            }
        }
    }

    /** 将 [Bundle] 还原为 [AgentEvent]，无法识别时返回 null。 */
    fun eventFromBundle(bundle: Bundle): AgentEvent? = when (bundle.getString(KEY_TYPE)) {
        "run_started" -> AgentEvent.RunStarted(
            initialImages = bundle.getInt("initial_images"),
            initialImageBytes = bundle.getInt("initial_image_bytes"),
            toolCount = bundle.getInt("tool_count"),
            terminalTools = bundle.getBoolean("terminal_tools"),
        )

        "round_started" -> AgentEvent.RoundStarted(
            round = bundle.getInt("round"),
            messageCount = bundle.getInt("message_count"),
        )

        "provider_request_started" -> AgentEvent.ProviderRequestStarted(
            round = bundle.getInt("round"),
        )

        "provider_response_started" -> AgentEvent.ProviderResponseStarted(
            round = bundle.getInt("round"),
            httpCode = bundle.getInt("http_code"),
        )

        "assistant_text_delta" -> AgentEvent.AssistantTextDelta(
            round = bundle.getInt("round"),
            deltaChars = bundle.getInt("delta_chars"),
        )

        "provider_tool_call_delta" -> AgentEvent.ProviderToolCallDelta(
            round = bundle.getInt("round"),
            index = bundle.getInt("index"),
            name = bundle.getString("name"),
            argumentsChars = bundle.getInt("arguments_chars"),
        )

        "assistant_received" -> AgentEvent.AssistantReceived(
            round = bundle.getInt("round"),
            contentChars = bundle.getInt("content_chars"),
            toolNames = bundle.getStringArrayList("tool_names").orEmpty(),
        )

        "tool_started" -> AgentEvent.ToolStarted(
            round = bundle.getInt("round"),
            name = bundle.getString("name").orEmpty(),
            argsPreview = bundle.getString("args_preview").orEmpty(),
        )

        "tool_finished" -> AgentEvent.ToolFinished(
            round = bundle.getInt("round"),
            name = bundle.getString("name").orEmpty(),
            resultSummary = bundle.getString("result_summary").orEmpty(),
            imageCount = bundle.getInt("image_count"),
            imageBytes = bundle.getInt("image_bytes"),
        )

        "tool_images_attached" -> AgentEvent.ToolImagesAttached(
            round = bundle.getInt("round"),
            toolName = bundle.getString("tool_name").orEmpty(),
            imageCount = bundle.getInt("image_count"),
            imageBytes = bundle.getInt("image_bytes"),
        )

        "run_finished" -> AgentEvent.RunFinished(
            round = bundle.getInt("round"),
            contentChars = bundle.getInt("content_chars"),
        )

        "run_failed" -> AgentEvent.RunFailed(
            reason = bundle.getString("reason").orEmpty(),
        )

        else -> null
    }

    private fun Bundle.optionalInt(key: String): Int? =
        if (containsKey(key)) getInt(key) else null
}
