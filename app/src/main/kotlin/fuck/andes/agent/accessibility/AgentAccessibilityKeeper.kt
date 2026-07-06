package fuck.andes.agent.accessibility

import android.content.ComponentName
import android.content.Context
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object AgentAccessibilityKeeper {
    private const val TAG = "AgentAccessibilityKeeper"
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "agent-accessibility-keeper").apply { isDaemon = true }
    }

    fun restoreAsync(
        context: Context,
        reason: String,
        onComplete: (() -> Unit)? = null
    ) {
        val appContext = context.applicationContext
        executor.execute {
            try {
                restore(appContext, reason)
            } finally {
                onComplete?.invoke()
            }
        }
    }

    private fun restore(context: Context, reason: String) {
        val component = ComponentName(
            context,
            AgentAccessibilityService::class.java
        ).flattenToString()
        val shortComponent = ComponentName(
            context,
            AgentAccessibilityService::class.java
        ).flattenToShortString()
        val result = runProcess(buildEnableCommand(component, shortComponent))
        if (result.exitCode == 0) {
            Log.i(TAG, "restore finished, reason=$reason, stdout=${result.stdout}")
        } else {
            Log.w(
                TAG,
                "restore failed, reason=$reason, exit=${result.exitCode}, stderr=${result.stderr}"
            )
        }
    }

    private fun buildEnableCommand(
        component: String,
        shortComponent: String
    ): String =
        """
        target=${shellQuote(component)}
        service_path=${shellQuote(shortComponent)}
        service=${'$'}(dumpsys activity service "${'$'}service_path" 2>/dev/null | grep SERVICE)
        services=${'$'}(settings get secure enabled_accessibility_services 2>/dev/null)
        case ":${'$'}services:" in
          *":${'$'}target:"*) needs_append=0; new_services="${'$'}services" ;;
          "") needs_append=1; new_services="${'$'}target" ;;
          *) needs_append=1; new_services="${'$'}services:${'$'}target" ;;
        esac
        if [ -z "${'$'}service" ] || [ "${'$'}needs_append" -eq 1 ]; then
          settings put secure accessibility_enabled 0
          settings put secure enabled_accessibility_services "${'$'}new_services"
          settings put secure accessibility_enabled 1
          echo restored
        else
          echo noop
        fi
        """.trimIndent()

    private fun runProcess(command: String): ShellResult {
        val process = runCatching {
            ProcessBuilder("su", "-c", command)
                .redirectErrorStream(false)
                .start()
        }.getOrElse {
            return ShellResult(exitCode = -1, stderr = it.message.orEmpty())
        }

        val stdout = process.inputStream.bufferedReader().use { it.readText() }
        val stderr = process.errorStream.bufferedReader().use { it.readText() }
        val finished = process.waitFor(10, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            return ShellResult(exitCode = -2, stderr = "timeout")
        }
        return ShellResult(
            exitCode = process.exitValue(),
            stdout = stdout.trim(),
            stderr = stderr.trim()
        )
    }

    private fun shellQuote(value: String): String =
        "'" + value.replace("'", "'\\''") + "'"

    private data class ShellResult(
        val exitCode: Int,
        val stdout: String = "",
        val stderr: String = ""
    )
}
