package fuck.andes.agent.runtime

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import fuck.andes.core.AgentLogger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * 入口进程侧的 Runtime 客户端。
 *
 * 它只负责把一次 Agent 请求交给模块进程，并把事件/结果带回入口适配层；
 * 不执行模型、不执行工具、不渲染 UI。
 */
internal class AgentRuntimeClient(
    private val context: Context,
    private val logger: AgentLogger
) {
    fun run(
        request: AgentRuntimeWire.RunRequest,
        onEvent: (AgentEvent) -> Unit
    ): AgentRuntimeWire.RunResult {
        val connectedLatch = CountDownLatch(1)
        val resultLatch = CountDownLatch(1)
        val receivedRuntimeResult = AtomicBoolean(false)
        val resultRef = AtomicReference<AgentRuntimeWire.RunResult?>()
        val clientMessenger = Messenger(
            ClientHandler(
                onEvent = onEvent,
                onResult = { result ->
                    receivedRuntimeResult.set(true)
                    resultRef.set(result)
                    resultLatch.countDown()
                }
            )
        )

        var serviceMessenger: Messenger? = null
        var bound = false
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                serviceMessenger = Messenger(binder)
                connectedLatch.countDown()
                runCatching {
                    val msg = Message.obtain(null, AgentRuntimeWire.MSG_START_RUN)
                    msg.replyTo = clientMessenger
                    msg.data = AgentRuntimeWire.toBundle(request)
                    serviceMessenger?.send(msg)
                }.onFailure { throwable ->
                    resultRef.set(
                        AgentRuntimeWire.RunResult(
                            ok = false,
                            content = "",
                            error = throwable.message ?: throwable.javaClass.simpleName
                        )
                    )
                    resultLatch.countDown()
                }
            }

            override fun onServiceDisconnected(name: ComponentName) {
                serviceMessenger = null
                connectedLatch.countDown()
                if (resultRef.get() == null) {
                    resultRef.set(
                        AgentRuntimeWire.RunResult(
                            ok = false,
                            content = "",
                            error = "Agent Runtime 服务连接已断开"
                        )
                    )
                    resultLatch.countDown()
                }
            }

            override fun onNullBinding(name: ComponentName) {
                connectedLatch.countDown()
                resultRef.set(
                    AgentRuntimeWire.RunResult(
                        ok = false,
                        content = "",
                        error = "Agent Runtime 服务拒绝绑定"
                    )
                )
                resultLatch.countDown()
            }
        }

        try {
            bound = context.bindService(
                AgentRuntimeWire.serviceIntent(),
                connection,
                Context.BIND_AUTO_CREATE or Context.BIND_IMPORTANT or Context.BIND_INCLUDE_CAPABILITIES
            )
            if (!bound) {
                return AgentRuntimeWire.RunResult(false, "", "Agent Runtime 服务绑定失败")
            }
            if (!connectedLatch.await(BIND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                return AgentRuntimeWire.RunResult(false, "", "Agent Runtime 服务连接超时")
            }
            resultLatch.await()
            return resultRef.get() ?: AgentRuntimeWire.RunResult(false, "", "Agent Runtime 未返回结果")
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
            runCatching { serviceMessenger?.send(Message.obtain(null, AgentRuntimeWire.MSG_CANCEL)) }
            return AgentRuntimeWire.RunResult(false, "", "Agent Runtime 等待被中断")
        } finally {
            val delayMs = if (receivedRuntimeResult.get()) UNBIND_DELAY_MS else 0L
            Handler(Looper.getMainLooper()).postDelayed({
                runCatching {
                    if (bound) context.unbindService(connection)
                }.onFailure { throwable ->
                    logger.warn("Agent runtime: 解绑服务失败: ${throwable.message ?: throwable.javaClass.simpleName}")
                }
            }, delayMs)
        }
    }

    private class ClientHandler(
        private val onEvent: (AgentEvent) -> Unit,
        private val onResult: (AgentRuntimeWire.RunResult) -> Unit
    ) : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                AgentRuntimeWire.MSG_EVENT -> {
                    AgentRuntimeWire.eventFromBundle(msg.data ?: return)?.let(onEvent)
                }

                AgentRuntimeWire.MSG_RESULT -> {
                    onResult(AgentRuntimeWire.runResultFromBundle(msg.data ?: return))
                }
            }
        }
    }

    private companion object {
        const val BIND_TIMEOUT_SECONDS = 8L
        const val UNBIND_DELAY_MS = 3_200L
    }
}
