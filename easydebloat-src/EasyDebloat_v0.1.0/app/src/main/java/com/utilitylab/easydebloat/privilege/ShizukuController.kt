package com.utilitylab.easydebloat.privilege

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import com.utilitylab.easydebloat.BuildConfig

class ShizukuController(private val context: Context) {
    enum class Status { NOT_INSTALLED, NOT_RUNNING, PERMISSION_REQUIRED, CONNECTING, READY, ERROR }

    data class State(
        val status: Status = Status.NOT_RUNNING,
        val serverUid: Int? = null,
        val serverVersion: Int? = null,
        val serviceUid: Int? = null,
        val message: String? = null
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    @Volatile
    private var service: IEasyDebloatService? = null

    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(ComponentName(context, EasyDebloatUserService::class.java))
            .daemon(false)
            .processNameSuffix("privileged")
            .tag("easy-debloat")
            .version(1)
            .debuggable(BuildConfig.DEBUG)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val remote = IEasyDebloatService.Stub.asInterface(binder)
            service = remote
            val uid = runCatching { remote.uid() }.getOrNull()
            _state.value = _state.value.copy(status = Status.READY, serviceUid = uid, message = null)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            _state.value = _state.value.copy(status = Status.CONNECTING, message = "Servizio privilegiato disconnesso")
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener { refreshAndBind() }
    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        service = null
        _state.value = State(status = if (isManagerInstalled()) Status.NOT_RUNNING else Status.NOT_INSTALLED)
    }
    private val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_CODE) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) bindUserService()
            else _state.value = currentServerState(Status.PERMISSION_REQUIRED, "Permesso Shizuku non concesso")
        }
    }

    fun start() {
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        refreshAndBind()
    }

    fun requestPermission() {
        if (!Shizuku.pingBinder()) {
            refreshAndBind()
            return
        }
        runCatching { Shizuku.requestPermission(REQUEST_CODE) }
            .onFailure { _state.value = currentServerState(Status.ERROR, it.message) }
    }

    fun retry() = refreshAndBind()

    suspend fun exec(command: String): ShellCommandResult = withContext(Dispatchers.IO) {
        val remote = service ?: return@withContext ShellCommandResult(-1, "Shizuku UserService non connesso")
        runCatching { ShellCommandResult.decode(remote.exec(command)) }
            .getOrElse { ShellCommandResult(-1, "${it.javaClass.simpleName}: ${it.message.orEmpty()}") }
    }

    private fun refreshAndBind() {
        if (!isManagerInstalled()) {
            _state.value = State(Status.NOT_INSTALLED)
            return
        }
        if (!Shizuku.pingBinder()) {
            _state.value = State(Status.NOT_RUNNING)
            return
        }

        val permission = runCatching { Shizuku.checkSelfPermission() }.getOrDefault(PackageManager.PERMISSION_DENIED)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            _state.value = currentServerState(Status.PERMISSION_REQUIRED)
            return
        }
        bindUserService()
    }

    private fun bindUserService() {
        if (service != null) {
            _state.value = currentServerState(Status.READY).copy(serviceUid = runCatching { service?.uid() }.getOrNull())
            return
        }
        _state.value = currentServerState(Status.CONNECTING)
        runCatching { Shizuku.bindUserService(userServiceArgs, serviceConnection) }
            .onFailure { _state.value = currentServerState(Status.ERROR, it.message) }
    }

    private fun currentServerState(status: Status, message: String? = null): State {
        val uid = runCatching { Shizuku.getUid() }.getOrNull()
        val version = runCatching { Shizuku.getVersion() }.getOrNull()
        return State(status = status, serverUid = uid, serverVersion = version, serviceUid = _state.value.serviceUid, message = message)
    }

    @Suppress("DEPRECATION")
    private fun isManagerInstalled(): Boolean = try {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getPackageInfo(
                MANAGER_PACKAGE,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            context.packageManager.getPackageInfo(MANAGER_PACKAGE, 0)
        }
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    companion object {
        const val MANAGER_PACKAGE = "moe.shizuku.privileged.api"
        private const val REQUEST_CODE = 51051
    }
}
