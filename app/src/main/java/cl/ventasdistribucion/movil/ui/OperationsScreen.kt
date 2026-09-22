package cl.ventasdistribucion.movil.ui

import cl.ventasdistribucion.movil.OperationsViewModel
import cl.ventasdistribucion.movil.objects

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

fun money(n: Int): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CL")).format(n)
@Composable fun OperationsApp(vm: OperationsViewModel = viewModel()) {
    val user = vm.user
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Spacer(Modifier.height(8.dp))
        Text("GESTIÓN COMERCIAL", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text("Ventas y distribución", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Operación móvil · Datos ficticios", style = MaterialTheme.typography.bodySmall)
        if (vm.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (vm.message.isNotEmpty()) Text(vm.message, color = MaterialTheme.colorScheme.primary)
        if (user == null) { Login(vm); return@Column }
        val role = user.getString("role")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(role, style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = vm::logout, enabled = !vm.busy) { Text("Salir") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = vm::refresh, enabled = !vm.busy) { Text("Actualizar") }
            Text(if (vm.offline) "Sin conexión" else "Sesión activa", Modifier.padding(top = 12.dp))
        }
        val tabs = listOf("Resumen", "Pedidos", "Clientes", "Productos", "Rutas", "Ventas") + if (role == "Administrador") listOf("Usuarios") else emptyList()
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tabs.forEach { FilterChip(selected = vm.page == it, onClick = { vm.page = it }, label = { Text(it) }) }
        }
        val data = vm.data
        if (data == null) { Text("Pulsa Actualizar para consultar la operación."); return@Column }
        Text("Última actualización: ${data.optString("syncedAt").take(19).replace('T',' ')} UTC", style = MaterialTheme.typography.labelSmall)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            when (vm.page) {
                "Resumen" -> {
                    val metrics = data.getJSONObject("metrics")
                    item { InfoCard("Ventas entregadas", money(metrics.getInt("sales"))) }
                    item { InfoCard("Meta de prueba", "${money(metrics.getInt("goal"))} · ${metrics.getInt("sales") * 100 / metrics.getInt("goal")}% de cumplimiento") }
                    item { InfoCard("Entregas", "${metrics.getInt("delivered")} realizadas · ${metrics.getInt("pending")} pendientes") }
                    items(listOf("Pendiente", "En ruta", "Entregado", "Incidencia")) { status -> InfoCard(status, "${data.getJSONArray("orders").objects().count { it.getString("status") == status }} pedidos") }
                }
                "Pedidos" -> {
                    if (role in listOf("Administrador", "Vendedor")) item { NewOrder(vm, data) }
                    if (data.getJSONArray("orders").length() == 0) item { Text("No hay pedidos asignados.") }
                    items(data.getJSONArray("orders").objects(), key = { it.getString("id") }) { OrderCard(vm, it, data, role) }
                }
                "Clientes" -> items(data.getJSONArray("clients").objects()) { InfoCard(it.getString("name"), it.getString("address")) }
                "Productos" -> items(data.getJSONArray("products").objects()) { InfoCard(it.getString("name"), money(it.getInt("price"))) }
                "Rutas" -> items(data.getJSONArray("routes").objects()) {
                    InfoCard(it.getString("name"), "${it.getString("branch")}\nVehículo ${it.getString("vehicle")} · Repartidor #${it.getString("driverId")}\n${it.getString("schedule")}\n${it.getString("location")}\nPedidos: " + data.getJSONArray("orders").objects().filter { o -> o.getString("routeId") == it.getString("id") }.joinToString { o -> "#${o.getString("id")}" })
                }
                "Ventas" -> {
                    val sales = data.getJSONArray("orders").objects().filter { it.getString("status") == "Entregado" }
                    if (sales.isEmpty()) item { Text("Las ventas se contabilizan cuando un pedido se entrega.") }
                    items(sales) { InfoCard("Venta · Pedido #${it.getString("id")}", "${money(it.getInt("total"))}\n${it.getString("date")}") }
                }
                "Usuarios" -> items(data.getJSONArray("users").objects()) { UserCard(vm, it, user.getString("id")) }
            }
        }
    }
}
@Composable fun Login(vm: OperationsViewModel) {
    var username by rememberSaveable { mutableStateOf("supervisor") }
    var password by remember { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("http://10.0.2.2:8000") }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Accede a tu operación", style = MaterialTheme.typography.titleLarge) }
        item { OutlinedTextField(username, { username = it }, label = { Text("Usuario de prueba") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(password, { password = it }, label = { Text("Contraseña") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(url, { url = it }, label = { Text("URL de API de prueba") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item { Button(onClick = { vm.login(url, username, password) }, enabled = !vm.busy && username.isNotBlank() && password.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Ingresar") } }
        item { InfoCard("Entorno académico", "Usuarios: administrador, supervisor, vendedor o repartidor.\nContraseña de prueba: Demo2026!") }
    }
}
@Composable fun InfoCard(title: String, description: String) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(description) } }
}
@Composable fun Choice(label: String, options: List<Pair<String,String>>, selected: String, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }) { Text("$label: ${options.find { it.first == selected }?.second ?: "Seleccionar"}") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) { options.forEach { (id,name) -> DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(id); open = false }) } }
    }
}
@Composable fun NewOrder(vm: OperationsViewModel, data: JSONObject) {
    var client by rememberSaveable { mutableStateOf("") }; var product by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("1") }; var priority by rememberSaveable { mutableStateOf("Normal") }
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Nuevo pedido", style = MaterialTheme.typography.titleMedium)
        Choice("Cliente", data.getJSONArray("clients").objects().map { it.getString("id") to it.getString("name") }, client) { client = it }
        Choice("Producto", data.getJSONArray("products").objects().map { it.getString("id") to it.getString("name") }, product) { product = it }
        OutlinedTextField(quantity, { quantity = it }, label = { Text("Cantidad (1–100)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Choice("Prioridad", listOf("Normal" to "Normal", "Alta" to "Alta"), priority) { priority = it }
        Button(enabled = !vm.busy && !vm.offline && client.isNotEmpty() && product.isNotEmpty() && (quantity.toIntOrNull() ?: 0) in 1..100, onClick = { vm.mutate("/orders", "POST", JSONObject().put("clientId",client).put("productId",product).put("quantity",quantity.toInt()).put("priority",priority)) }) { Text("Registrar pedido") }
    } }
}
@Composable fun OrderCard(vm: OperationsViewModel, order: JSONObject, data: JSONObject, role: String) {
    val id = order.getString("id"); val status = order.getString("status")
    var note by rememberSaveable(id) { mutableStateOf("") }
    var expanded by rememberSaveable(id) { mutableStateOf(false) }
    var confirmation by remember { mutableStateOf<String?>(null) }
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Pedido #$id · $status", fontWeight = FontWeight.Bold)
        Text(data.getJSONArray("clients").objects().find { it.getString("id") == order.getString("clientId") }?.getString("name") ?: "Cliente")
        Text("${order.getInt("quantity")} × " + (data.getJSONArray("products").objects().find { it.getString("id") == order.getString("productId") }?.getString("name") ?: "Producto"))
        Text("${money(order.getInt("total"))} · Prioridad ${order.getString("priority")}\n${order.getString("date").take(10)} · Ruta #${order.getString("routeId")}")
        if (role in listOf("Administrador", "Repartidor") && status != "Entregado") {
            OutlinedTextField(note, { note = it.take(500) }, label = { Text("Nota / incidencia") }, modifier = Modifier.fillMaxWidth())
            val next = if (status == "En ruta") listOf("Entregado", "Incidencia") else listOf("En ruta")
            next.forEach { target -> OutlinedButton(enabled = !vm.busy && !vm.offline && (target != "Incidencia" || note.isNotBlank()), onClick = { confirmation = target }) { Text("Marcar $target") } }
        }
        TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Ocultar historial" else "Ver trazabilidad") }
        if (expanded) order.getJSONArray("history").objects().forEach { Text("${it.getString("status")} · ${it.getString("actor")}\n${it.getString("date")} ${it.optString("note")}", style = MaterialTheme.typography.bodySmall) }
    } }
    confirmation?.let { target -> AlertDialog(onDismissRequest = { confirmation = null }, title = { Text("Confirmar $target") }, text = { Text("Se registrará el cambio y su autor en el historial del pedido #$id.") }, confirmButton = { TextButton(onClick = { vm.mutate("/orders/$id", "PATCH", JSONObject().put("status",target).put("expectedStatus",status).put("note",note)); confirmation = null }) { Text("Confirmar") } }, dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Cancelar") } }) }
}
@Composable fun UserCard(vm: OperationsViewModel, user: JSONObject, currentId: String) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
        Text(user.getString("name"), fontWeight = FontWeight.Bold)
        Text("${user.getString("username")} · ${if (user.getBoolean("active")) "Activo" else "Desactivado"}")
        if (user.getString("id") != currentId && !vm.busy && !vm.offline) {
            Choice("Rol", listOf("Administrador", "Supervisor", "Vendedor", "Repartidor").map { it to it }, user.getString("role")) { role -> vm.mutate("/users/${user.getString("id")}", "PATCH", JSONObject().put("role",role).put("active",user.getBoolean("active"))) }
            TextButton(onClick = { vm.mutate("/users/${user.getString("id")}", "PATCH", JSONObject().put("role",user.getString("role")).put("active",!user.getBoolean("active"))) }) { Text(if (user.getBoolean("active")) "Desactivar acceso" else "Activar acceso") }
        }
    } }
}
