package com.lifetrack.app.presentation.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.exifinterface.media.ExifInterface
import com.lifetrack.app.data.remote.MealAnalysisResult
import com.lifetrack.app.presentation.viewmodel.MealViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@Composable
fun MealCameraScreen(contentPadding: PaddingValues, onBack: () -> Unit, viewModel: MealViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted = it }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { selected ->
            val copied = copyImageAsJpeg(context, selected)
            if (copied != null) viewModel.setPhoto(copied.absolutePath)
            else viewModel.setCaptureError("No pudimos abrir esa imagen. Elegí otra fotografía.")
        }
    }

    ScreenColumn(contentPadding) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) { PageHeader("Fotografiar comida", "La estimación siempre queda bajo tu control") }
            IconButton(onClick = onBack) { Icon(Icons.Rounded.Close, "Cerrar cámara") }
        }

        when {
            state.result != null -> MealResult(state.result!!, viewModel::saveEdited, viewModel::startOver, state.loading)
            state.notice != null -> {
                FriendlyNotice(state.notice!!)
                Button(onClick = viewModel::startOver, modifier = Modifier.fillMaxWidth()) { Text("Fotografiar otra comida") }
            }
            state.photoPath != null -> {
                PhotoPreview(state.photoPath!!, viewModel::setCaptureError)
                if (state.loading) {
                    Text("Estamos identificando alimentos y nutrientes…", style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(onClick = viewModel::startOver, modifier = Modifier.weight(1f)) { Text("Repetir") }
                        Button(onClick = viewModel::analyze, modifier = Modifier.weight(1f)) { Text("Usar foto") }
                    }
                }
            }
            !state.authInitialized -> LinearProgressIndicator(Modifier.fillMaxWidth())
            state.ownerUserId == null -> EmptyState("Iniciá sesión desde Perfil para proteger la fotografía y asociar el análisis únicamente a tu cuenta.")
            !state.serviceConfigured -> EmptyState("El análisis de comidas no está configurado en esta versión. Tus demás registros siguen disponibles.")
            !state.consent -> ConsentCard(state.consent, viewModel::setConsent)
            permissionGranted -> {
                CameraPreview(onCaptured = viewModel::setPhoto, onError = viewModel::setCaptureError)
                FilledTonalButton(onClick = { gallery.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                    Text("  Elegir de la galería")
                }
            }
            else -> {
                EmptyState("Para sacar una foto, permití el acceso a la cámara. También podés elegir una imagen de tu galería.")
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }, modifier = Modifier.fillMaxWidth()) { Text("Permitir cámara") }
                FilledTonalButton(onClick = { gallery.launch("image/*") }, modifier = Modifier.fillMaxWidth()) { Text("Abrir galería") }
            }
        }
        state.error?.let { ErrorCard(it) }
    }
}

@Composable
private fun ConsentCard(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().toggleable(checked, role = Role.Checkbox, onValueChange = onCheckedChange)) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Checkbox(checked, onCheckedChange = null)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Procesamiento de imagen", style = MaterialTheme.typography.titleMedium)
                Text("Autorizo el procesamiento para identificar alimentos. La foto se guarda de forma privada con mi historial y puedo eliminarla junto con el registro.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FriendlyNotice(message: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(message, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PhotoPreview(path: String, onError: (String) -> Unit) {
    var bitmap by remember(path) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(path) {
        bitmap = withContext(Dispatchers.IO) { decodeSampledBitmap(path) }
        if (bitmap == null) onError("No pudimos mostrar esta fotografía. Elegí otra imagen.")
    }
    bitmap?.let {
        Image(
            it.asImageBitmap(),
            "Vista previa de la comida",
            Modifier.fillMaxWidth().height(340.dp),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun CameraPreview(onCaptured: (String) -> Unit, onError: (String) -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var preview by remember { mutableStateOf<Preview?>(null) }
    var capturing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AndroidView(
            factory = { viewContext ->
                PreviewView(viewContext).also { view ->
                    ProcessCameraProvider.getInstance(context).addListener({
                        runCatching {
                            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                            val previewUseCase = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
                            cameraProvider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, previewUseCase, imageCapture)
                            provider = cameraProvider
                            preview = previewUseCase
                        }.onFailure { onError("La cámara no está disponible ahora. Podés elegir una foto de tu galería.") }
                    }, ContextCompat.getMainExecutor(context))
                }
            },
            modifier = Modifier.fillMaxWidth().height(420.dp),
        )
        Button(
            onClick = {
                capturing = true
                val directory = File(context.filesDir, "pending_meals").apply { mkdirs() }
                val file = File(directory, "meal-${UUID.randomUUID()}.jpg")
                imageCapture.takePicture(
                    ImageCapture.OutputFileOptions.Builder(file).build(),
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            scope.launch {
                                val normalized = withContext(Dispatchers.IO) { normalizeJpegFile(file) }
                                capturing = false
                                if (normalized != null) onCaptured(normalized.absolutePath)
                                else onError("No pudimos preparar la fotografía. Intentá nuevamente.")
                            }
                        }
                        override fun onError(exception: ImageCaptureException) {
                            capturing = false
                            file.delete()
                            onError("No pudimos tomar la foto. Intentá nuevamente o elegí una de tu galería.")
                        }
                    },
                )
            },
            enabled = !capturing,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Icon(Icons.Rounded.CameraAlt, contentDescription = null)
            Text(if (capturing) "  Guardando…" else "  Tomar foto")
        }
    }
    DisposableEffect(owner) {
        onDispose { preview?.let { provider?.unbind(it, imageCapture) } }
    }
}

@Composable
private fun MealResult(result: MealAnalysisResult, onSave: (MealAnalysisResult) -> Unit, onStartOver: () -> Unit, loading: Boolean) {
    var foods by remember(result.id) { mutableStateOf(result.foods) }
    var calories by remember(result.id) { mutableStateOf(result.nutrition.calories.toString()) }
    var protein by remember(result.id) { mutableStateOf(result.nutrition.proteinG.toString()) }
    var carbs by remember(result.id) { mutableStateOf(result.nutrition.carbsG.toString()) }
    var fat by remember(result.id) { mutableStateOf(result.nutrition.fatG.toString()) }
    var fiber by remember(result.id) { mutableStateOf(result.nutrition.fiberG.toString()) }
    var sugars by remember(result.id) { mutableStateOf(result.nutrition.sugarsG.toString()) }
    var sodium by remember(result.id) { mutableStateOf(result.nutrition.sodiumMg.toString()) }
    var showValidation by remember { mutableStateOf(false) }
    var lowConfidenceConfirmed by remember(result.id) { mutableStateOf(false) }
    val inputs = listOf(calories, protein, carbs, fat, fiber, sugars, sodium)
    val values = inputs.map(String::toDoubleOrNull)
    val numericValid = values.zip(listOf(20_000.0, 5_000.0, 5_000.0, 5_000.0, 1_000.0, 5_000.0, 100_000.0)).all { (value, max) -> value != null && value.isFinite() && value in 0.0..max }
    val foodsValid = foods.isNotEmpty() && foods.all { it.name.trim().length in 1..120 && it.estimatedPortion.trim().length in 1..120 && it.estimatedGrams > 0 }
    val valid = numericValid && foodsValid
    val saved = result.status in setOf("completed", "corrected")
    val needsConfirmation = result.confidence < 0.7

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(if (saved) "Comida guardada" else "Revisá la estimación", style = MaterialTheme.typography.titleLarge)
        }
        Text("Confianza del análisis: ${(result.confidence * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, color = if (needsConfirmation) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        if (result.observations.isNotEmpty()) Text(result.observations.joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant)
        foods.forEachIndexed { index, food ->
            OutlinedTextField(
                food.name,
                { value -> foods = foods.toMutableList().also { it[index] = food.copy(name = value) } },
                label = { Text("Alimento") }, modifier = Modifier.fillMaxWidth(), enabled = !saved,
                isError = showValidation && food.name.trim().length !in 1..120,
                supportingText = { if (showValidation && food.name.trim().length !in 1..120) Text("Ingresá un nombre de hasta 120 caracteres") },
            )
            OutlinedTextField(
                food.estimatedPortion,
                { value -> foods = foods.toMutableList().also { it[index] = food.copy(estimatedPortion = value) } },
                label = { Text("Porción estimada") }, modifier = Modifier.fillMaxWidth(), enabled = !saved,
                isError = showValidation && food.estimatedPortion.trim().length !in 1..120,
                supportingText = { if (showValidation && food.estimatedPortion.trim().length !in 1..120) Text("Ingresá una porción válida") },
            )
            if (food.alternatives.isNotEmpty()) {
                Text("Alternativas: ${food.alternatives.joinToString()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        NutritionField("Calorías", calories, { calories = it }, !saved)
        NutritionField("Proteínas (g)", protein, { protein = it }, !saved)
        NutritionField("Carbohidratos (g)", carbs, { carbs = it }, !saved)
        NutritionField("Grasas (g)", fat, { fat = it }, !saved)
        NutritionField("Fibra (g)", fiber, { fiber = it }, !saved)
        NutritionField("Azúcares (g)", sugars, { sugars = it }, !saved)
        NutritionField("Sodio (mg)", sodium, { sodium = it }, !saved)
        if (showValidation && !valid) Text("Revisá alimentos, porciones y valores nutricionales antes de guardar.", color = MaterialTheme.colorScheme.error)
        if (needsConfirmation && !saved) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().clickable { lowConfidenceConfirmed = !lowConfidenceConfirmed }.padding(12.dp)) {
                    Checkbox(lowConfidenceConfirmed, { lowConfidenceConfirmed = it })
                    Text("Revisé el alimento y la porción. Confirmo que los valores editados son adecuados para mi registro.", modifier = Modifier.weight(1f))
                }
            }
        }
        Text("Los valores son estimaciones visuales para seguimiento general y no constituyen consejo médico o nutricional.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!saved) {
            Button(
                onClick = {
                    showValidation = true
                    if (valid && (!needsConfirmation || lowConfidenceConfirmed)) {
                        val parsed = inputs.map { it.toDouble() }
                        onSave(result.copy(foods = foods.map { it.copy(name = it.name.trim(), estimatedPortion = it.estimatedPortion.trim()) }, nutrition = result.nutrition.copy(calories = parsed[0], proteinG = parsed[1], carbsG = parsed[2], fatG = parsed[3], fiberG = parsed[4], sugarsG = parsed[5], sodiumMg = parsed[6])))
                    }
                },
                enabled = !loading && (!needsConfirmation || lowConfidenceConfirmed),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Guardar comida") }
        } else {
            Button(onClick = onStartOver, modifier = Modifier.fillMaxWidth()) { Text("Analizar otra comida") }
        }
    }
}

@Composable
private fun NutritionField(label: String, value: String, onValueChange: (String) -> Unit, enabled: Boolean) {
    OutlinedTextField(
        value,
        onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun copyImageAsJpeg(context: Context, uri: Uri): File? = runCatching {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    options.inSampleSize = calculateSampleSize(options.outWidth, options.outHeight, 2048)
    options.inJustDecodeBounds = false
    val decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
    val rotation = context.contentResolver.openInputStream(uri)?.use { exifRotation(ExifInterface(it)) } ?: 0f
    val bitmap = rotateBitmap(decoded, rotation)
    val directory = File(context.filesDir, "pending_meals").apply { mkdirs() }
    File(directory, "meal-${UUID.randomUUID()}.jpg").also { file ->
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        bitmap.recycle()
    }
}.getOrNull()

private fun normalizeJpegFile(source: File): File? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(source.absolutePath, bounds)
    val options = BitmapFactory.Options().apply { inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, 2048) }
    val decoded = BitmapFactory.decodeFile(source.absolutePath, options) ?: return null
    val bitmap = rotateBitmap(decoded, exifRotation(ExifInterface(source.absolutePath)))
    val normalized = File(source.parentFile, "normalized-${source.name}")
    normalized.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
    bitmap.recycle()
    source.delete()
    normalized
}.getOrNull()

private fun decodeSampledBitmap(path: String): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    val options = BitmapFactory.Options().apply { inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, 1200) }
    return BitmapFactory.decodeFile(path, options)
}

private fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
    var sample = 1
    while (width / sample > maxDimension || height / sample > maxDimension) sample *= 2
    return sample
}

private fun exifRotation(exif: ExifInterface): Float = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
    else -> 0f
}

private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
    if (degrees == 0f) return source
    val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, Matrix().apply { postRotate(degrees) }, true)
    if (rotated !== source) source.recycle()
    return rotated
}
