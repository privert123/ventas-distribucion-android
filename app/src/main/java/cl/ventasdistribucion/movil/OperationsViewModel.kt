package cl.ventasdistribucion.movil

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.*
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URI

fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
class ApiFailure(val code: Int, message: String): Exception(message)
class Repository(app: Application) {
    private val prefs = app.getSharedPreferences("demo-cache", 0)
    var token = ""
    var userId = ""
    var baseUrl = "http://10.0.2.2:8000"
    suspend fun request(path: String, method: String = "GET", body: JSONObject? = null): JSONObject = withContext(Dispatchers.IO) {
        val connection = URI(baseUrl.trimEnd('/') + path).toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 5000; connection.readTimeout = 5000
            connection.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toString().toByteArray()) }
            }
            val code = connection.responseCode
            val text = (if (code in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() } ?: "{}"
            val result = JSONObject(text)
            if (code !in 200..299) throw ApiFailure(code, result.optString("error", "Error de API"))
            result
        } finally { connection.disconnect() }
    }
    fun cache(data: JSONObject) { prefs.edit().putString("snapshot-$userId", data.toString()).apply() }
    fun cached(): JSONObject? = prefs.getString("snapshot-$userId", null)?.let { runCatching { JSONObject(it) }.getOrNull() }
    fun clear() { prefs.edit().remove("snapshot-$userId").apply(); token = ""; userId = "" }
}
class OperationsViewModel(app: Application): AndroidViewModel(app) {
    private val repo = Repository(app)
    var user by mutableStateOf<JSONObject?>(null); private set
    var data by mutableStateOf<JSONObject?>(null); private set
    var busy by mutableStateOf(false); private set
    var offline by mutableStateOf(false); private set
    var message by mutableStateOf(""); private set
    var page by mutableStateOf("Resumen")
    private fun task(block: suspend () -> Unit) {
        if (busy) return
        viewModelScope.launch {
            busy = true; message = ""
            try { block() }
            catch (e: Exception) {
                if (e is ApiFailure && e.code == 401) { repo.clear(); user = null; data = null }
                message = e.message ?: "No se pudo completar la operación"
            } finally { busy = false }
        }
    }
    fun login(url: String, username: String, password: String) = task {
        require(url.startsWith("http://") || url.startsWith("https://")) { "URL inválida" }
        repo.baseUrl = url
        val response = repo.request("/login", "POST", JSONObject().put("username", username.trim()).put("password", password))
        repo.token = response.getString("token"); user = response.getJSONObject("user"); repo.userId = user!!.getString("id")
        page = "Resumen"; refreshData()
    }
    private suspend fun refreshData() {
        try {
            data = repo.request("/snapshot"); repo.cache(data!!); user = data!!.getJSONObject("user"); offline = false
        } catch (e: java.io.IOException) {
            data = repo.cached(); offline = true
            message = if (data == null) "Sin conexión y sin datos guardados. Reintenta al recuperar la red." else "Sin conexión: consulta de la última sincronización. Los cambios requieren conexión."
        }
    }
    fun refresh() = task { refreshData() }
    fun mutate(path: String, method: String, body: JSONObject) = task {
        try { repo.request(path, method, body) }
        catch (e: java.io.IOException) { offline = true; throw Exception("Conexión interrumpida. Actualiza para comprobar si se guardó antes de reintentar.") }
        refreshData()
        if (!offline) message = "Cambio guardado correctamente"
    }
    fun logout() = task {
        try { repo.request("/logout", "POST", JSONObject()) } catch (_: Exception) { }
        repo.clear(); user = null; data = null; offline = false; page = "Resumen"
    }
}
