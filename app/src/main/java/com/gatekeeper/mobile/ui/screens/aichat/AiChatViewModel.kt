package com.gatekeeper.mobile.ui.screens.aichat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatekeeper.mobile.data.repository.AiChatRepository
import com.gatekeeper.mobile.data.repository.FirewallRepository
import com.gatekeeper.mobile.data.repository.DnsRepository
import com.gatekeeper.mobile.data.db.dao.IpRuleDao
import com.gatekeeper.mobile.data.db.entity.DnsEntry
import com.gatekeeper.mobile.data.db.entity.IpRule
import com.gatekeeper.mobile.domain.model.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val chatRepository: AiChatRepository,
    private val firewallRepository: FirewallRepository,
    private val dnsRepository: DnsRepository,
    private val ipRuleDao: IpRuleDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                role = "assistant",
                content = "Hello! I'm GateKeeper AI 🛡️\n\nI can help you manage your network security. Try commands like:\n• \"Block Instagram\"\n• \"Show blocked apps\"\n• \"Block facebook.com\"\n• \"What's my network status?\""
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isServerOnline = MutableStateFlow(false)
    val isServerOnline: StateFlow<Boolean> = _isServerOnline.asStateFlow()

    init {
        checkServerHealth()
    }

    fun sendMessage(text: String) {
        val userMessage = ChatMessage(role = "user", content = text)
        _messages.value = _messages.value + userMessage
        _isLoading.value = true

        viewModelScope.launch {
            val result = chatRepository.sendMessage(text)
            result.onSuccess { response ->
                val assistantMessage = ChatMessage(
                    role = "assistant",
                    content = response.response,
                    toolCalls = response.toolCalls
                )
                _messages.value = _messages.value + assistantMessage
                
                // Process local execution steps
                executeLocalOperations(response.operationResults)
            }.onFailure { error ->
                val errorMessage = ChatMessage(
                    role = "assistant",
                    content = "⚠️ Error: ${error.message ?: "Failed to reach AI server"}.\n\nMake sure the desktop GateKeeper is running and the server IP is configured in Settings."
                )
                _messages.value = _messages.value + errorMessage
            }
            _isLoading.value = false
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            chatRepository.clearHistory()
            _messages.value = listOf(
                ChatMessage(role = "assistant", content = "Chat cleared. How can I help?")
            )
        }
    }

    fun checkServerHealth() {
        viewModelScope.launch {
            _isServerOnline.value = chatRepository.checkHealth()
        }
    }

    private fun resolvePackageName(appName: String): String {
        if (appName.contains(".")) return appName
        try {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (appInfo in packages) {
                val label = pm.getApplicationLabel(appInfo).toString()
                if (label.equals(appName, ignoreCase = true)) {
                    return appInfo.packageName
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return appName
    }

    private fun executeLocalOperations(operations: List<Map<String, Any>>?) {
        if (operations == null) return
        
        viewModelScope.launch {
            for (op in operations) {
                val operationName = op["operation"] as? String ?: continue
                val payloadMap = op["payload"] as? Map<*, *> ?: continue
                
                try {
                    when (operationName) {
                        "block_process" -> {
                            val target = payloadMap["process_name"] as? String ?: payloadMap["path"] as? String
                            if (target != null) {
                                val pkgName = resolvePackageName(target)
                                firewallRepository.toggleBlock(pkgName, target, true)
                            }
                        }
                        "unblock_process" -> {
                            val target = payloadMap["process_name"] as? String ?: payloadMap["path"] as? String
                            if (target != null) {
                                val pkgName = resolvePackageName(target)
                                firewallRepository.toggleBlock(pkgName, target, false)
                            }
                        }
                        "block_website" -> {
                            val domain = payloadMap["domain"] as? String
                            if (domain != null) dnsRepository.addDomain(domain, "blacklist", "ai")
                        }
                        "unblock_website" -> {
                            val domain = payloadMap["domain"] as? String
                            if (domain != null) dnsRepository.removeDomain(domain, "blacklist")
                        }
                        "block_ip" -> {
                            val ip = payloadMap["ip"] as? String
                            if (ip != null) ipRuleDao.insert(IpRule(ip = ip, ruleType = "blacklist", source = "ai"))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
