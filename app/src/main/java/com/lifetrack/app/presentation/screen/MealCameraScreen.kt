package com.lifetrack.app.presentation.screen

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifetrack.app.presentation.viewmodel.MealViewModel
import java.io.File
import java.util.UUID

@Composable fun MealCameraScreen(contentPadding: PaddingValues, onBack: () -> Unit, viewModel: MealViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle(); val context = LocalContext.current
    var permission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permission = it }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val directory = File(context.filesDir, "pending_meals").apply { mkdirs() }
            val file = File(directory, "meal-${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(it)?.use { input -> file.outputStream().use(input::copyTo) }
            if (file.exists() && file.length() > 0) viewModel.submitPhoto(file.absolutePath)
        }
    }
    ScreenColumn(contentPadding) {
        Text("Analizar comida", style = MaterialTheme.typography.headlineMedium)
        if (state.result == null) {
            Row { Checkbox(state.consent, viewModel::setConsent); Text("Autorizo el procesamiento temporal de la imagen para identificar alimentos. La foto se elimina despues del analisis.") }
            Button(onClick = { gallery.launch("image/*") }, enabled = state.consent && !state.loading, modifier = Modifier.fillMaxWidth()) { Text("Seleccionar desde galeria") }
        }
        when {
            state.result != null -> MealResult(state.result!!, viewModel::saveEdited, state.loading)
            state.photoPath != null -> {
                val bitmap = remember(state.photoPath) { BitmapFactory.decodeFile(state.photoPath) }
                bitmap?.let { Image(it.asImageBitmap(), "Vista previa de la comida", Modifier.fillMaxWidth().height(320.dp)) }
                Text("Analizando alimentos y nutrientes...", style = MaterialTheme.typography.titleMedium)
            }
            !state.consent -> EmptyState("Acepta el procesamiento temporal para habilitar la camara o la galeria.")
            permission -> CameraPreview(onCaptured = viewModel::submitPhoto, onError = viewModel::setCaptureError)
            else -> Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text("Permitir camara") }
        }
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.error?.let { ErrorCard(it) }
        if (state.history.isNotEmpty()) {
            Text("Historial nutricional", style = MaterialTheme.typography.titleLarge)
            state.history.take(5).forEach { meal -> Text("${meal.foods.joinToString { it.name }} · ${meal.nutrition.calories.toInt()} kcal") }
        }
        TextButton(onClick = onBack) { Text("Volver") }
    }
}

@Composable private fun CameraPreview(onCaptured: (String) -> Unit, onError: (String) -> Unit) {
    val context = LocalContext.current; val owner = LocalLifecycleOwner.current; val imageCapture = remember { ImageCapture.Builder().build() }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AndroidView(factory = { PreviewView(it).also { view ->
            val providerFuture = ProcessCameraProvider.getInstance(context)
            providerFuture.addListener({ val provider = providerFuture.get(); val preview = Preview.Builder().build().also { p -> p.surfaceProvider = view.surfaceProvider }; provider.unbindAll(); provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture) }, ContextCompat.getMainExecutor(context))
        } }, modifier = Modifier.fillMaxWidth().height(420.dp))
        Button(onClick = {
            val file = File(File(context.filesDir, "pending_meals").apply { mkdirs() }, "meal-${UUID.randomUUID()}.jpg")
            imageCapture.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) = onCaptured(file.absolutePath)
                override fun onError(exception: ImageCaptureException) { file.delete(); onError("No se pudo tomar la foto. Intenta nuevamente.") }
            })
        }, modifier = Modifier.fillMaxWidth()) { Text("Tomar foto") }
    }
    DisposableEffect(Unit) { onDispose { } }
}

@Composable private fun MealResult(result: com.lifetrack.app.data.remote.MealAnalysisResult, onSave: (com.lifetrack.app.data.remote.MealAnalysisResult) -> Unit, loading: Boolean) {
    var foods by remember(result.id) { mutableStateOf(result.foods) }
    var calories by remember(result.id) { mutableStateOf(result.nutrition.calories.toString()) }; var protein by remember(result.id) { mutableStateOf(result.nutrition.proteinG.toString()) }
    var carbs by remember(result.id) { mutableStateOf(result.nutrition.carbsG.toString()) }; var fat by remember(result.id) { mutableStateOf(result.nutrition.fatG.toString()) }
    var fiber by remember(result.id) { mutableStateOf(result.nutrition.fiberG.toString()) }; var sugars by remember(result.id) { mutableStateOf(result.nutrition.sugarsG.toString()) }; var sodium by remember(result.id) { mutableStateOf(result.nutrition.sodiumMg.toString()) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Resultado estimado", style = MaterialTheme.typography.titleLarge)
        foods.forEachIndexed { index, food ->
            androidx.compose.material3.OutlinedTextField(food.name, { value -> foods = foods.toMutableList().also { it[index] = food.copy(name = value) } }, label = { Text("Alimento") })
            androidx.compose.material3.OutlinedTextField(food.ingredients.joinToString(", "), { value -> foods = foods.toMutableList().also { it[index] = food.copy(ingredients = value.split(',').map(String::trim).filter(String::isNotEmpty)) } }, label = { Text("Ingredientes detectados") })
            androidx.compose.material3.OutlinedTextField(food.estimatedPortion, { value -> foods = foods.toMutableList().also { it[index] = food.copy(estimatedPortion = value) } }, label = { Text("Porcion estimada") })
        }
        androidx.compose.material3.OutlinedTextField(calories, { calories = it }, label = { Text("Calorias estimadas") })
        androidx.compose.material3.OutlinedTextField(protein, { protein = it }, label = { Text("Proteinas (g)") })
        androidx.compose.material3.OutlinedTextField(carbs, { carbs = it }, label = { Text("Carbohidratos (g)") })
        androidx.compose.material3.OutlinedTextField(fat, { fat = it }, label = { Text("Grasas (g)") })
        androidx.compose.material3.OutlinedTextField(fiber, { fiber = it }, label = { Text("Fibra (g)") })
        androidx.compose.material3.OutlinedTextField(sugars, { sugars = it }, label = { Text("Azucares (g)") })
        androidx.compose.material3.OutlinedTextField(sodium, { sodium = it }, label = { Text("Sodio (mg)") })
        Text(result.disclaimer, style = MaterialTheme.typography.bodySmall)
        Button(onClick = {
            val values = listOf(calories, protein, carbs, fat, fiber, sugars, sodium).map { it.toDoubleOrNull() }
            if (values.all { it != null && it.isFinite() && it >= 0.0 }) {
                onSave(result.copy(foods = foods, nutrition = result.nutrition.copy(calories = values[0]!!, proteinG = values[1]!!, carbsG = values[2]!!, fatG = values[3]!!, fiberG = values[4]!!, sugarsG = values[5]!!, sodiumMg = values[6]!!)))
            }
        }, enabled = !loading && result.id == null, modifier = Modifier.fillMaxWidth()) { Text(if (result.id == null) "Guardar comida" else "Comida guardada") }
    }
}
