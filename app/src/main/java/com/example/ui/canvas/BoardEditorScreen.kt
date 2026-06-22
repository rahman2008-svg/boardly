package com.example.ui.canvas

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.BoardViewModel
import com.example.Tool
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BoardEditorScreen(
    viewModel: BoardViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val activeTool by viewModel.activeTool.collectAsState()
    val selectedColor by viewModel.selectedColor.collectAsState()
    val brushThickness by viewModel.brushThickness.collectAsState()
    val currentBoard by viewModel.currentBoard.collectAsState()
    val selectedShapeType by viewModel.selectedShapeType.collectAsState()
    val activePalette by viewModel.activePalette.collectAsState()
    
    val isSnapToGrid by viewModel.isSnapToGrid.collectAsState()
    val isAutoShapeCorrection by viewModel.isAutoShapeCorrectionEnabled.collectAsState()

    var showExportDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showShapesDialog by remember { mutableStateOf(false) }
    var showSettingsDrawer by remember { mutableStateOf(false) }
    
    // Dialog inputs
    var textInputState by remember { mutableStateOf<Tool?>(null) } // Text or Sticky Tool
    var inputTextValue by remember { mutableStateOf("") }
    var inputStickyColor by remember { mutableStateOf(0xFFFFF9C4.toInt()) } // default pale yellow

    // Presentation mode config
    var isPresentationMode by remember { mutableStateOf(false) }

    // Standalone Gallery image selection launcher (no permission required on modern Android API)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.addImageElement(it)
            Toast.makeText(context, "Image loaded onto the infinite canvas!", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        
        // 1. Core infinite whiteboard vector grid canvas
        WhiteboardCanvas(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Animated floating Top Menu Bar (hides during Presentation mode for maximum space)
        AnimatedVisibility(
            visible = !isPresentationMode,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .statusBarsPadding(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            viewModel.saveActiveBoardState()
                            onBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit back to board browser")
                        }
                        
                        Column(modifier = Modifier.padding(start = 4.dp)) {
                            Text(
                                text = currentBoard?.name ?: "Scratchpad Canvas",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                modifier = Modifier.widthIn(max = 140.dp)
                            )
                            Text(
                                text = "Infinite scale workspace",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Direct Vector undo/redo history controls
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.undo() }) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo stroke")
                        }
                        IconButton(onClick = { viewModel.redo() }) {
                            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo stroke")
                        }
                        
                        VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp))

                        IconButton(onClick = { showExportDialog = true }) {
                            Icon(Icons.Default.Share, contentDescription = "Export drawing share options")
                        }
                        IconButton(onClick = { showSettingsDrawer = true }) {
                            Icon(Icons.Default.Tune, contentDescription = "Canvas configurations drawer")
                        }
                    }
                }
            }
        }

        // 3. Floating Quick Side Viewport Helpers (Zoom diagnostics)
        AnimatedVisibility(
            visible = !isPresentationMode,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                tonalElevation = 2.dp,
                shadowElevation = 4.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(6.dp)
                ) {
                    IconButton(onClick = { viewModel.scaleCanvas(1.25f) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom in infinite scale", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { viewModel.scaleCanvas(0.8f) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom out infinite scale", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { viewModel.resetCanvasTransform() }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "Reset coordinate viewport center", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // 4. Centered Floating Bottom Toolbar (Standard drawing pens, highlighters, shapes, sticky, elements picker)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (isPresentationMode) 12.dp else 20.dp)
                .wrapContentWidth()
                .height(64.dp),
            shape = RoundedCornerShape(32.dp),
            color = if (isPresentationMode) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // If in Presentation mode, provide Exit button
                if (isPresentationMode) {
                    IconButton(
                        onClick = { isPresentationMode = false },
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.FullscreenExit,
                            contentDescription = "Exit presentations full-screen mode",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    VerticalDivider(modifier = Modifier.height(28.dp).padding(horizontal = 4.dp))
                }

                // Pen Tool
                ToolButton(
                    selected = activeTool == Tool.Pen,
                    icon = Icons.Default.BorderColor,
                    description = "Ink point Pen",
                    onClick = { viewModel.setTool(Tool.Pen) }
                )

                // Pencil Tool
                ToolButton(
                    selected = activeTool == Tool.Pencil,
                    icon = Icons.Default.Create,
                    description = "Soft textured Pencil",
                    onClick = { viewModel.setTool(Tool.Pencil) }
                )

                // Marker Highlighter
                ToolButton(
                    selected = activeTool == Tool.Marker,
                    icon = Icons.Default.Gesture,
                    description = "Marker Highlighter",
                    onClick = { viewModel.setTool(Tool.Marker) }
                )

                // Shape tool
                ToolButton(
                    selected = activeTool == Tool.Shape,
                    icon = when (selectedShapeType) {
                        "circle" -> Icons.Default.RadioButtonUnchecked
                        "line" -> Icons.Default.HorizontalRule
                        "arrow" -> Icons.Default.TrendingFlat
                        else -> Icons.Default.CropDin
                    },
                    description = "Geometric vector shapes",
                    onClick = { 
                        viewModel.setTool(Tool.Shape)
                        showShapesDialog = true
                    }
                )

                // Color picker bubble
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(selectedColor))
                        .border(1.5.dp, Color.White, CircleShape)
                        .clickable { showColorDialog = true }
                )

                VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp))

                // Sticky Note Tool
                ToolButton(
                    selected = activeTool == Tool.Sticky,
                    icon = Icons.Default.StickyNote2,
                    description = "Pastel Sticky Note",
                    onClick = { 
                        viewModel.setTool(Tool.Sticky)
                        inputTextValue = ""
                        textInputState = Tool.Sticky
                    }
                )

                // Text Element Tool
                ToolButton(
                    selected = activeTool == Tool.Text,
                    icon = Icons.Default.TextFields,
                    description = "Text annotation card",
                    onClick = { 
                        viewModel.setTool(Tool.Text)
                        inputTextValue = ""
                        textInputState = Tool.Text
                    }
                )

                // Local gallery image insertion trigger
                ToolButton(
                    selected = false,
                    icon = Icons.Default.AddPhotoAlternate,
                    description = "Insert offline picture",
                    onClick = {
                        galleryLauncher.launch("image/*")
                    }
                )

                VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp))

                // Laser Pointer
                ToolButton(
                    selected = activeTool == Tool.Laser,
                    icon = Icons.Default.WavingHand,
                    description = "Laser Pointer Trail",
                    onClick = { viewModel.setTool(Tool.Laser) }
                )

                // Eraser Tool
                ToolButton(
                    selected = activeTool == Tool.Eraser,
                    icon = Icons.Default.AutoFixNormal,
                    description = "Vector stroke Eraser",
                    onClick = { viewModel.setTool(Tool.Eraser) }
                )

                // Selection Drag Pointer
                ToolButton(
                    selected = activeTool == Tool.Select,
                    icon = Icons.Default.FrontHand,
                    description = "Grab, scale and depth layer elements",
                    onClick = { viewModel.setTool(Tool.Select) }
                )
            }
        }

        // --- Bottom Sheet / Options configuration drawer dialog ---
        if (showSettingsDrawer) {
            Dialog(onDismissRequest = { showSettingsDrawer = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Canvas Toolkit Configs", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            IconButton(onClick = { showSettingsDrawer = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close config screen")
                            }
                        }

                        // Drawing Thickness Slider
                        Column {
                            Text("Brush & Line Thickness (${brushThickness.toInt()}px)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Slider(
                                value = brushThickness,
                                onValueChange = { viewModel.updateThickness(it) },
                                valueRange = 2f..40f,
                                modifier = Modifier.fillMaxWidth().testTag("brush_thickness_slider")
                            )
                        }

                        // Infinite canvas backgrounds styles
                        Column {
                            Text("Whiteboard Grids & Ruled Lines Patterns", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("GRID", "DOTS", "RULED", "BLANK").forEach { bgPattern ->
                                    val isSelected = currentBoard?.backgroundType == bgPattern
                                    Button(
                                        onClick = { viewModel.changeBackgroundType(bgPattern) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(bgPattern, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        HorizontalDivider()

                        // Snap-to-Grid toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Snap to Grid", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Magnetically lock vectors to line intersections", fontSize = 11.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = isSnapToGrid,
                                onCheckedChange = { viewModel.isSnapToGrid.value = it },
                                modifier = Modifier.testTag("snap_to_grid_switch")
                            )
                        }

                        // Auto-Shape Correction toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Auto Shape Beautification", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Corrects rough sketches to perfect shapes", fontSize = 11.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = isAutoShapeCorrection,
                                onCheckedChange = { viewModel.isAutoShapeCorrectionEnabled.value = it },
                                modifier = Modifier.testTag("auto_shape_correct_switch")
                            )
                        }

                        HorizontalDivider()

                        // Presentation Mode trigger
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isPresentationMode = true
                                    showSettingsDrawer = false
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PresentToAll, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Launch Fullscreen Presentation Mode", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Collapses menus for cleaner drawing showcase", fontSize = 11.sp, color = Color.Gray)
                            }
                        }

                        // Board lock status
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.toggleBoardLock()
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val locked = currentBoard?.isLocked == true
                            Icon(
                                if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = if (locked) Color.Red else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    if (locked) "Unlock Board Sketches" else "Lock Board from Edits",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text("Preents accidental shifts/draws while teaching", fontSize = 11.sp, color = Color.Gray)
                            }
                        }

                        // Wipes all data
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.clearActiveBoard()
                                    showSettingsDrawer = false
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Wipe Canvas Board", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                                Text("Permanently delete all drawing lines", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        // --- Shape Type Picker Dialog ---
        if (showShapesDialog) {
            Dialog(onDismissRequest = { showShapesDialog = false }) {
                Card(
                    modifier = Modifier.padding(16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Select Mathematical Shape", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            listOf(
                                Pair("rectangle", Icons.Default.CropDin),
                                Pair("circle", Icons.Default.RadioButtonUnchecked),
                                Pair("line", Icons.Default.HorizontalRule),
                                Pair("arrow", Icons.Default.TrendingFlat)
                            ).forEach { (shape, icon) ->
                                val active = selectedShapeType == shape
                                IconButton(
                                    onClick = {
                                        viewModel.selectShapeType(shape)
                                        showShapesDialog = false
                                    },
                                    modifier = Modifier
                                        .size(54.dp)
                                        .background(
                                            if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    Icon(icon, contentDescription = "Shape $shape", tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Color Palettes & Selection Dialog ---
        if (showColorDialog) {
            Dialog(onDismissRequest = { showColorDialog = false }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Aesthetic Colors Picker", fontWeight = FontWeight.Bold)
                            
                            // Generator Button
                            TextButton(onClick = { viewModel.generateNextPalette() }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("New Palette", fontSize = 12.sp)
                                }
                            }
                        }

                        // Palette grid
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            activePalette.forEach { colorInt ->
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorInt))
                                        .border(
                                            width = if (selectedColor == colorInt) 3.dp else 0.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            viewModel.selectColor(colorInt)
                                            showColorDialog = false
                                        }
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        // --- Text/Sticky Card Content Creation popup input dialog ---
        textInputState?.let { toolType ->
            val isSticky = toolType == Tool.Sticky
            Dialog(onDismissRequest = { textInputState = null }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSticky) Color(inputStickyColor) else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isSticky) "Spawn Pastel Sticky Note" else "Create Custom Text Element",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black // High-contrast headers on sticky/shapes
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = inputTextValue,
                            onValueChange = { inputTextValue = it },
                            placeholder = { Text("Write content message here...") },
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(8.dp)
                        )

                        // If sticky, offer pastel background selection row
                        if (isSticky) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    0xFFFFF9C4.toInt(), // Pastel Yellow
                                    0xFFE1F5FE.toInt(), // Pastel Blue
                                    0xFFE8F5E9.toInt(), // Pastel Green
                                    0xFFFCE4EC.toInt(), // Pastel Pink
                                    0xFFF3E5F5.toInt()  // Pastel Purple
                                ).forEach { colorHex ->
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(colorHex))
                                            .border(
                                                width = if (inputStickyColor == colorHex) 2.dp else 0.dp,
                                                color = Color.Black,
                                                shape = CircleShape
                                            )
                                            .clickable { inputStickyColor = colorHex }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = { textInputState = null }) {
                                Text("Cancel", color = Color.Gray)
                            }
                            Button(
                                onClick = {
                                    if (isSticky) {
                                        viewModel.addStickyNote(inputTextValue, inputStickyColor)
                                    } else {
                                        viewModel.addTextElement(inputTextValue)
                                    }
                                    textInputState = null
                                }
                            ) {
                                Text("Spawn")
                            }
                        }
                    }
                }
            }
        }

        // --- Share & Export Board Offline Dialog ---
        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("Export Whiteboard") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Select how you want to compile and export this infinite canvas offline:", fontSize = 12.sp)
                        
                        Button(
                            onClick = {
                                try {
                                    val bitmap = viewModel.exportBoardAsPngBitmap()
                                    val cachePath = File(context.cacheDir, "images")
                                    cachePath.mkdirs()
                                    val file = File(cachePath, "boardly_export_${System.currentTimeMillis()}.png")
                                    val stream = FileOutputStream(file)
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                                    stream.close()

                                    val contentUri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )

                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, contentUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Board PNG Illustration"))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                                showExportDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Design PNG Image")
                        }

                        Button(
                            onClick = {
                                val shareStr = viewModel.generateBoardJsonShareString()
                                if (shareStr != null) {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareStr)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Send Boardly Layout Data"))
                                } else {
                                    Toast.makeText(context, "No layout elements to bundle", Toast.LENGTH_SHORT).show()
                                }
                                showExportDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Input, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Collaborative JSON File")
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun ToolButton(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .background(
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = CircleShape
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
