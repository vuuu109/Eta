package fuck.andes.core

import android.util.Log

internal interface AgentLogger {
    fun debug(message: String)
    fun info(message: String)
    fun warn(message: String)
    fun error(message: String, throwable: Throwable? = null)
}

internal object AndroidAgentLogger : AgentLogger {
    override fun debug(message: String) {
        if (ModuleConfig.ENABLE_VERBOSE_LOGS) {
            Log.d(ModuleConfig.TAG, message)
        }
    }

    override fun info(message: String) {
        Log.i(ModuleConfig.TAG, message)
    }

    override fun warn(message: String) {
        Log.w(ModuleConfig.TAG, message)
    }

    override fun error(message: String, throwable: Throwable?) {
        if (throwable == null) {
            Log.e(ModuleConfig.TAG, message)
        } else {
            Log.e(ModuleConfig.TAG, message, throwable)
        }
    }
}
