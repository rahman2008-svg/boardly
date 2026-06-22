package com.example.ui.canvas

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.BoardViewModel
import com.example.Tool
import com.example.model.*
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

@Composable
fun WhiteboardCanvas(
    viewModel: BoardViewModel,
    modifier: Modifier = Modifier
) {
    val boardElements by viewModel.boardElements.collectAsState()
    val currentStroke by viewModel.currentStroke.collectAsState()
    val activeShapeGuide by viewModel.activeShapeGuide.collectAsState()
    val laserTrail by viewModel.laserTrail.collectAsState()
    val activeTool by viewModel.activeTool.collectAsState()
    val selectedElementId by viewModel.selectedElementId.collectAsState()
    
    val zoomScale by viewModel.zoomScale.collectAsState()
    val panOffset by viewModel.panOffset.collectAsState()
    
    val currentBoard by viewModel.currentBoard.collectAsState()

    val isDark = isSystemInDarkTheme()
    val canvasBgColor = if (isDark) Color(0xFF141416) else Color(0xFFFAF9F6) // Elegant soft tone
    val patternColor = if (isDark) Color(0xFF2C2C2F) else Color(0xFFE5E4DE)

    // Touch event tracker counts current fingers
    var pointerCount by remember { mutableIntStateOf(0) }

    // Dialog state for text/sticky note edits
    var textEditDialogState by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // ID to isStickyNote
    var textEditValue by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(canvasBgColor)
            .pointerInput(activeTool) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        pointerCount = event.changes.size
                        
                        // Cancel current stroke if user puts a second finger down (initiates pan/zoom)
                        if (pointerCount > 1) {
                            viewModel.onDrawEnd()
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (pointerCount > 1) {
                        viewModel.scaleCanvas(zoom)
                        viewModel.panCanvas(pan.x, pan.y)
                    }
                }
            }
            .pointerInput(activeTool) {
                if (activeTool == Tool.Select) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            viewModel.onDrawStart(offset.x, offset.y)
                        },
                        onDrag = { px, dragAmount ->
                            px.consume()
                            val selId = viewModel.selectedElementId.value
                            if (selId != null) {
                                // Translate element
                                viewModel.dragSelectedElement(
                                    dragAmount.x / zoomScale,
                                    dragAmount.y / zoomScale
                                )
                            } else {
                                // Pan background board
                                viewModel.panCanvas(dragAmount.x, dragAmount.y)
                            }
                        },
                        onDragEnd = {
                            viewModel.onDrawEnd()
                        }
                    )
                } else {
                    // Standard sketch toolings
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (pointerCount == 1) {
                                viewModel.onDrawStart(offset.x, offset.y)
                            }
                        },
                        onDrag = { change, _ ->
                            if (pointerCount == 1) {
                                change.consume()
                                viewModel.onDrawDrag(change.position.x, change.position.y)
                            }
                        },
                        onDragEnd = {
                            if (pointerCount <= 1) {
                                viewModel.onDrawEnd()
                            }
                        }
                    )
                }
            }
    ) {

        // 1. Vector Drawing Canvas (Renders lines, pen, pencil, highlighters, mathematical shapes, laser trails)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("whiteboard_canvas")
        ) {
            val bgType = currentBoard?.backgroundType ?: "GRID"

            // A. Draw Grid Patterns
            val gridSpacing = 40f * zoomScale
            val offsetX = panOffset.x % gridSpacing
            val offsetY = panOffset.y % gridSpacing

            when (bgType) {
                "GRID" -> {
                    // Vertical lines
                    var x = offsetX
                    while (x < size.width) {
                        drawLine(patternColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                        x += gridSpacing
                    }
                    // Horizontal lines
                    var y = offsetY
                    while (y < size.height) {
                        drawLine(patternColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                        y += gridSpacing
                    }
                }
                "DOTS" -> {
                    var x = offsetX
                    while (x < size.width) {
                        var y = offsetY
                        while (y < size.height) {
                            drawCircle(patternColor, radius = 2.5f * zoomScale, center = Offset(x, y))
                            y += gridSpacing
                        }
                        x += gridSpacing
                    }
                }
                "RULED" -> {
                    var y = offsetY
                    while (y < size.height) {
                        drawLine(patternColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.2f)
                        y += gridSpacing
                    }
                }
            }

            // B. Apply Scale/Translation Matrix for active vector items
            withTransform({
                translate(panOffset.x, panOffset.y)
                scale(zoomScale, zoomScale)
            }) {
                
                // Draw historic static board elements
                for (el in boardElements) {
                    when (el) {
                        is StrokeElement -> {
                            if (el.points.size > 1) {
                                val brushPaint = Paint().apply {
                                    color = Color(el.color)
                                    strokeWidth = el.width
                                    style = PaintingStyle.Stroke
                                    strokeCap = StrokeCap.Round
                                    strokeJoin = StrokeJoin.Round
                                }
                                val path = Path()
                                path.moveTo(el.points[0].x, el.points[0].y)
                                // Bezier path interpolation for smooth vectors (handwriting smoothing)
                                for (i in 1 until el.points.size) {
                                    val prev = el.points[i - 1]
                                    val curr = el.points[i]
                                    val midX = (prev.x + curr.x) / 2
                                    val midY = (prev.y + curr.y) / 2
                                    path.quadraticTo(prev.x, prev.y, midX, midY)
                                }
                                path.lineTo(el.points.last().x, el.points.last().y)
                                
                                drawPath(
                                    path = path,
                                    color = Color(el.color),
                                    alpha = el.alpha,
                                    style = Stroke(
                                        width = el.width,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }
                        is ShapeElement -> {
                            val color = Color(el.color)
                            val stroke = Stroke(width = el.width, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            
                            val start = Offset(el.startX, el.startY)
                            val end = Offset(el.endX, el.endY)
                            
                            val left = min(el.startX, el.endX)
                            val top = min(el.startY, el.endY)
                            val right = max(el.startX, el.endX)
                            val bottom = max(el.startY, el.endY)
                            val width = max(2f, right - left)
                            val height = max(2f, bottom - top)

                            when (el.shapeType) {
                                "line" -> {
                                    drawLine(color, start, end, strokeWidth = el.width)
                                }
                                "rectangle" -> {
                                    drawRect(color, Offset(left, top), androidx.compose.ui.geometry.Size(width, height), style = stroke)
                                }
                                "circle" -> {
                                    val r = hypot(el.endX - el.startX, el.endY - el.startY) / 2
                                    val cx = (el.startX + el.endX) / 2
                                    val cy = (el.startY + el.endY) / 2
                                    drawCircle(color, radius = r, center = Offset(cx, cy), style = stroke)
                                }
                                "arrow" -> {
                                    drawLine(color, start, end, strokeWidth = el.width)
                                    val dx = el.endX - el.startX
                                    val dy = el.endY - el.startY
                                    val len = hypot(dx, dy)
                                    if (len > 8f) {
                                        val ux = dx / len
                                        val uy = dy / len
                                        val arrowLength = 18f
                                        val angleScale = 0.5f // 30 deg
                                        
                                        val ax = el.endX - ux * arrowLength
                                        val ay = el.endY - uy * arrowLength
                                        
                                        val rx = ax + (-uy) * arrowLength * angleScale
                                        val ry = ay + ux * arrowLength * angleScale
                                        val lx = ax - (-uy) * arrowLength * angleScale
                                        val ly = ay  - ux * arrowLength * angleScale
                                        
                                        drawLine(color, end, Offset(rx, ry), strokeWidth = el.width)
                                        drawLine(color, end, Offset(lx, ly), strokeWidth = el.width)
                                    }
                                }
                            }
                        }
                        else -> {
                            // Text, StickyNotes, and Images are drawn via Compose interactive overlay layers
                        }
                    }
                }

                // C. Draw Real-time Active line guideline
                currentStroke?.let { stroke ->
                    if (stroke.points.size > 1) {
                        val path = Path()
                        path.moveTo(stroke.points[0].x, stroke.points[0].y)
                        for (i in 1 until stroke.points.size) {
                            val prev = stroke.points[i - 1]
                            val curr = stroke.points[i]
                            val midX = (prev.x + curr.x) / 2
                            val midY = (prev.y + curr.y) / 2
                            path.quadraticTo(prev.x, prev.y, midX, midY)
                        }
                        path.lineTo(stroke.points.last().x, stroke.points.last().y)
                        
                        drawPath(
                            path = path,
                            color = Color(stroke.color),
                            alpha = stroke.alpha,
                            style = Stroke(
                                width = stroke.width,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                // D. Renders Shape guidelines during drag setup
                activeShapeGuide?.let { shape ->
                    val color = Color(shape.color).copy(alpha = 0.6f)
                    val stroke = Stroke(width = shape.width, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    
                    val start = Offset(shape.startX, shape.startY)
                    val end = Offset(shape.endX, shape.endY)
                    
                    val left = min(shape.startX, shape.endX)
                    val top = min(shape.startY, shape.endY)
                    val right = max(shape.startX, shape.endX)
                    val bottom = max(shape.startY, shape.endY)
                    
                    when (shape.shapeType) {
                        "line" -> {
                            drawLine(color, start, end, strokeWidth = shape.width)
                        }
                        "rectangle" -> {
                            drawRect(color, Offset(left, top), androidx.compose.ui.geometry.Size(right - left, bottom - top), style = stroke)
                        }
                        "circle" -> {
                            val r = hypot(shape.endX - shape.startX, shape.endY - shape.startY) / 2
                            val cx = (shape.startX + shape.endX) / 2
                            val cy = (shape.startY + shape.endY) / 2
                            drawCircle(color, radius = r, center = Offset(cx, cy), style = stroke)
                        }
                        "arrow" -> {
                            drawLine(color, start, end, strokeWidth = shape.width)
                            val dx = shape.endX - shape.startX
                            val dy = shape.endY - shape.startY
                            val len = hypot(dx, dy)
                            if (len > 8f) {
                                val ux = dx / len
                                val uy = dy / len
                                val arrowLength = 18f
                                val angleScale = 0.5f
                                
                                val ax = shape.endX - ux * arrowLength
                                val ay = shape.endY - uy * arrowLength
                                
                                val rx = ax + (-uy) * arrowLength * angleScale
                                val ry = ay + ux * arrowLength * angleScale
                                val lx = ax - (-uy) * arrowLength * angleScale
                                val ly = ay  - ux * arrowLength * angleScale
                                
                                drawLine(color, end, Offset(rx, ry), strokeWidth = shape.width)
                                drawLine(color, end, Offset(lx, ly), strokeWidth = shape.width)
                            }
                        }
                    }
                }

                // E. Draw Real-time decaying Laser trail
                if (laserTrail.size > 1) {
                    val laserPaintColor = Color.Red.copy(alpha = 0.85f)
                    for (i in 1 until laserTrail.size) {
                        val progress = i / laserTrail.size.toFloat() // older points are thinner
                        drawLine(
                            color = laserPaintColor,
                            start = Offset(laserTrail[i-1].x, laserTrail[i-1].y),
                            end = Offset(laserTrail[i].x, laserTrail[i].y),
                            strokeWidth = 10f * progress,
                            cap = StrokeCap.Round
                        )
                    }
                    // draw radial laser glow at tip
                    val tip = laserTrail.last()
                    drawCircle(
                        color = Color.Red,
                        radius = 8f,
                        center = Offset(tip.x, tip.y)
                    )
                }
            }
        }

        // 2. Interactive Compose Elements Overlay Layer (Sticky notes, Text elements, Image items)
        boardElements.forEach { el ->
            when (el) {
                is StickyNoteElement -> {
                    val xInScreen = el.x * zoomScale + panOffset.x
                    val yInScreen = el.y * zoomScale + panOffset.y
                    val widthInScreen = el.width * zoomScale
                    val heightInScreen = el.height * zoomScale
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(xInScreen.toInt(), yInScreen.toInt()) }
                            .size(widthInScreen.dp, heightInScreen.dp)
                            .shadow(elevation = 3.dp, shape = RoundedCornerShape(2.dp))
                            .background(Color(el.bgColor))
                            .border(width = 0.5.dp, Color.Black.copy(alpha = 0.15f))
                            .clickable {
                                viewModel.selectElementAt(el.x + 10f, el.y + 10f)
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDrag = { px, dragAmount ->
                                        px.consume()
                                        viewModel.selectElementAt(el.x + 10f, el.y + 10f)
                                        viewModel.dragSelectedElement(
                                            dragAmount.x / zoomScale,
                                            dragAmount.y / zoomScale
                                        )
                                    }
                                )
                            },
                        contentAlignment = Alignment.TopStart
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp * zoomScale)
                        ) {
                            Text(
                                text = el.noteText,
                                style = TextStyle(
                                    color = Color.Black, // Sticky note content is high-contrast
                                    fontSize = (12f * zoomScale).coerceIn(4f, 24f).sp,
                                    lineHeight = (15f * zoomScale).coerceIn(6f, 30f).sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = {
                                        textEditDialogState = Pair(el.id, true)
                                        textEditValue = el.noteText
                                    },
                                    modifier = Modifier.size((28.dp * zoomScale).coerceIn(16.dp, 40.dp))
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit Card",
                                        tint = Color.Black.copy(alpha = 0.6f),
                                        modifier = Modifier.size((16.dp * zoomScale).coerceIn(8.dp, 28.dp))
                                    )
                                }
                            }
                        }
                    }
                }
                is TextElement -> {
                    val xInScreen = el.x * zoomScale + panOffset.x
                    val yInScreen = el.y * zoomScale + panOffset.y
                    val widthInScreen = el.width * zoomScale
                    val heightInScreen = el.height * zoomScale
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(xInScreen.toInt(), yInScreen.toInt()) }
                            .size(widthInScreen.dp, heightInScreen.dp)
                            .border(
                                width = 1.dp,
                                color = if (selectedElementId == el.id) Color.Gray.copy(alpha = 0.4f) else Color.Transparent,
                                shape = RoundedCornerShape(2.dp)
                            )
                            .clickable {
                                viewModel.selectElementAt(el.x + 5f, el.y + 5f)
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDrag = { px, dragAmount ->
                                        px.consume()
                                        viewModel.selectElementAt(el.x + 5f, el.y + 5f)
                                        viewModel.dragSelectedElement(
                                            dragAmount.x / zoomScale,
                                            dragAmount.y / zoomScale
                                        )
                                    }
                                )
                            },
                        contentAlignment = Alignment.TopStart
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp * zoomScale)
                        ) {
                            Text(
                                text = el.text,
                                style = TextStyle(
                                    color = Color(el.color),
                                    fontSize = (el.fontSize * zoomScale).coerceIn(6f, 48f).sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Start
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                            
                            IconButton(
                                onClick = {
                                    textEditDialogState = Pair(el.id, false)
                                    textEditValue = el.text
                                },
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .size((28.dp * zoomScale).coerceIn(16.dp, 40.dp))
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Text",
                                    tint = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f),
                                    modifier = Modifier.size((16.dp * zoomScale).coerceIn(8.dp, 28.dp))
                                )
                            }
                        }
                    }
                }
                is ImageElement -> {
                    val xInScreen = el.x * zoomScale + panOffset.x
                    val yInScreen = el.y * zoomScale + panOffset.y
                    val widthInScreen = el.width * zoomScale
                    val heightInScreen = el.height * zoomScale
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(xInScreen.toInt(), yInScreen.toInt()) }
                            .size(widthInScreen.dp, heightInScreen.dp)
                            .shadow(elevation = 2.dp, shape = RoundedCornerShape(4.dp))
                            .background(if (isDark) Color(0xFF222225) else Color.White)
                            .border(
                                width = 1.dp,
                                color = if (selectedElementId == el.id) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable {
                                viewModel.selectElementAt(el.x + 10f, el.y + 10f)
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDrag = { px, dragAmount ->
                                        px.consume()
                                        viewModel.selectElementAt(el.x + 10f, el.y + 10f)
                                        viewModel.dragSelectedElement(
                                            dragAmount.x / zoomScale,
                                            dragAmount.y / zoomScale
                                        )
                                    }
                                )
                            }
                    ) {
                        AsyncImage(
                            model = el.uriString,
                            contentDescription = "User drawing annotation asset",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                        
                        Text(
                            text = "🖼️ Gallery Image",
                            fontSize = (9f * zoomScale).coerceIn(6f, 15f).sp,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(topEnd = 4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                else -> {}
            }
        }

        // 3. Selection Bounding Dashboard (Only rendered when in Select mode with a validated selected element)
        selectedElementId?.let { selId ->
            val selectedElement = boardElements.find { it.id == selId }
            if (activeTool == Tool.Select && selectedElement != null) {
                val bounds = selectedElement.getBounds()
                
                // Mapped screen coords
                val left = bounds.left * zoomScale + panOffset.x
                val top = bounds.top * zoomScale + panOffset.y
                val right = bounds.right * zoomScale + panOffset.x
                val bottom = bounds.bottom * zoomScale + panOffset.y
                
                val width = max(40f, right - left)
                val height = max(40f, bottom - top)

                // Outer selection outline overlay
                Box(
                    modifier = Modifier
                        .offset { IntOffset(left.toInt(), top.toInt()) }
                        .size(width.dp, height.dp)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(2.dp)
                        )
                ) {
                    // Small floating toolbar for selected component
                    Surface(
                        modifier = Modifier
                            .offset(y = (-52).dp)
                            .align(Alignment.TopCenter)
                            .height(44.dp)
                            .wrapContentWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                        tonalElevation = 6.dp,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.moveSelectedToFront() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.VerticalAlignTop,
                                    contentDescription = "Bring to Front",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.moveSelectedToBack() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.VerticalAlignBottom,
                                    contentDescription = "Send to Back",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.toggleSelectedLockState() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    if (selectedElement.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "Toggle Element Lock",
                                    tint = if (selectedElement.isLocked) Color.Red else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            VerticalDivider(modifier = Modifier.height(20.dp))
                            IconButton(
                                onClick = { viewModel.deleteSelectedElement() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete element",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Resize corner handle anchoring at lower right
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 6.dp, y = 6.dp)
                            .size(20.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                            .pointerInput(selectedElement) {
                                detectDragGestures { px, dragAmount ->
                                    px.consume()
                                    // Resize computation
                                    val localW = (right - left) / zoomScale
                                    val localH = (bottom - top) / zoomScale
                                    val deltaW = dragAmount.x / zoomScale
                                    val deltaH = dragAmount.y / zoomScale
                                    viewModel.resizeSelectedElement(
                                        max(40f, localW + deltaW),
                                        max(40f, localH + deltaH)
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AspectRatio,
                            contentDescription = "Resize icon indicator",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        // 4. In-place Quick Card text updating popup dialog
        textEditDialogState?.let { (id, isSticky) ->
            Dialog(
                onDismissRequest = { textEditDialogState = null }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSticky) Color(0xFFFFFDE7) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isSticky) "Edit Sticky Note Description" else "Edit Text Card Properties",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        OutlinedTextField(
                            value = textEditValue,
                            onValueChange = { textEditValue = it },
                            placeholder = { Text("Write content here...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            textStyle = TextStyle(color = Color.Black),
                            shape = RoundedCornerShape(8.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = { textEditDialogState = null }
                            ) {
                                Text("Cancel", color = Color.Gray)
                            }
                            Button(
                                onClick = {
                                    if (isSticky) {
                                        viewModel.updateStickyNoteContent(id, textEditValue)
                                    } else {
                                        viewModel.updateTextElementContent(id, textEditValue)
                                    }
                                    textEditDialogState = null
                                }
                            ) {
                                Text("Apply")
                            }
                        }
                    }
                }
            }
        }
    }
}
