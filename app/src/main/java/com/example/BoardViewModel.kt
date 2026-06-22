package com.example

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.net.Uri
import android.os.Environment
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BoardEntity
import com.example.data.BoardRepository
import com.example.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

sealed class Tool {
    object Pen : Tool()
    object Pencil : Tool()
    object Marker : Tool()
    object Eraser : Tool()
    object Shape : Tool()
    object Text : Tool()
    object Sticky : Tool()
    object Laser : Tool()
    object Select : Tool()
}

class BoardViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    val repository = BoardRepository(database.boardDao())

    // All available boards
    val allBoards: StateFlow<List<BoardEntity>> = repository.allBoards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently active board (Database representation)
    private val _currentBoard = MutableStateFlow<BoardEntity?>(null)
    val currentBoard: StateFlow<BoardEntity?> = _currentBoard.asStateFlow()

    // Loaded canvas elements (In-memory representation for fast, interactive updates)
    private val _boardElements = MutableStateFlow<List<BoardElement>>(emptyList())
    val boardElements: StateFlow<List<BoardElement>> = _boardElements.asStateFlow()

    // Active Tool & Brush settings
    private val _activeTool = MutableStateFlow<Tool>(Tool.Pen)
    val activeTool: StateFlow<Tool> = _activeTool.asStateFlow()

    private val _selectedColor = MutableStateFlow(0xFF1E1E1E.toInt()) // Default Dark Charcoal
    val selectedColor: StateFlow<Int> = _selectedColor.asStateFlow()

    private val _brushThickness = MutableStateFlow(6f)
    val brushThickness: StateFlow<Float> = _brushThickness.asStateFlow()

    private val _selectedShapeType = MutableStateFlow("rectangle") // circle, rectangle, arrow, line
    val selectedShapeType: StateFlow<String> = _selectedShapeType.asStateFlow()

    // Real-time drawn stroke state
    private val _currentStroke = MutableStateFlow<StrokeElement?>(null)
    val currentStroke: StateFlow<StrokeElement?> = _currentStroke.asStateFlow()

    // Real-time laser pointer trail points
    private val _laserTrail = MutableStateFlow<List<Point>>(emptyList())
    val laserTrail: StateFlow<List<Point>> = _laserTrail.asStateFlow()
    private var laserJob: Job? = null

    // Real-time drawing current shape coordinates
    private val _activeShapeGuide = MutableStateFlow<ShapeElement?>(null)
    val activeShapeGuide: StateFlow<ShapeElement?> = _activeShapeGuide.asStateFlow()

    // Infinite Canvas scale & pan states
    private val _zoomScale = MutableStateFlow(1f)
    val zoomScale: StateFlow<Float> = _zoomScale.asStateFlow()

    private val _panOffset = MutableStateFlow(Offset(0f, 0f))
    val panOffset: StateFlow<Offset> = _panOffset.asStateFlow()

    // Selection properties
    private val _selectedElementId = MutableStateFlow<String?>(null)
    val selectedElementId: StateFlow<String?> = _selectedElementId.asStateFlow()

    // Undo / Redo Stacks
    private val undoStack = mutableListOf<List<BoardElement>>()
    private val redoStack = mutableListOf<List<BoardElement>>()

    // Smart Features Config
    val isSnapToGrid = MutableStateFlow(false)
    val isAutoShapeCorrectionEnabled = MutableStateFlow(false)
    
    // Aesthetic Color Palette Custom Generator
    private val palettes = listOf(
        listOf(0xFF1E1E1E.toInt(), 0xFFE91E63.toInt(), 0xFF9C27B0.toInt(), 0xFF3F51B5.toInt(), 0xFF00BCD4.toInt()), // Cyberpunk Slate
        listOf(0xFF1B5E20.toInt(), 0xFF4CAF50.toInt(), 0xFF8BC34A.toInt(), 0xFFCDDC39.toInt(), 0xFFFFEB3B.toInt()), // Forest Jade
        listOf(0xFF01579B.toInt(), 0xFF0288D1.toInt(), 0xFF29B6F6.toInt(), 0xFFFF7043.toInt(), 0xFFFFEB3B.toInt()), // Retro Ocean
        listOf(0xFFD84315.toInt(), 0xFFFF5722.toInt(), 0xFFFF9800.toInt(), 0xFFFFEB3B.toInt(), 0xFF4CAF50.toInt()), // Sunset Glow
        listOf(0xFF3E2723.toInt(), 0xFF4E342E.toInt(), 0xFF8D6E63.toInt(), 0xFFD7CCC8.toInt(), 0xFFE0F2F1.toInt())  // Warm Terracotta
    )
    val activePalette = MutableStateFlow(palettes[0])

    // Search query for boards dashboard
    val boardSearchQuery = MutableStateFlow("")

    init {
        // Automatically check if there are no boards, and insert a couple of default tutorial boards!
        viewModelScope.launch {
            allBoards.collect { list ->
                if (list.isEmpty()) {
                    createBoard("💡 Welcome & Quick Tutorial", isTutorial = true)
                    createBoard("🎨 Infinite Design Scratchpad")
                }
            }
        }
    }

    // Load elements from active board
    fun loadBoard(board: BoardEntity) {
        saveCurrentBoardData() // save current state before moving!
        _currentBoard.value = board
        _selectedElementId.value = null
        
        // Parse JSON
        val list = mutableListOf<BoardElement>()
        try {
            val arr = JSONArray(board.elementsJson)
            for (i in 0 until arr.length()) {
                val element = BoardElement.fromJson(arr.getJSONObject(i))
                if (element != null) {
                    list.add(element)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _boardElements.value = list
        
        // Reset transformation
        _zoomScale.value = 1f
        _panOffset.value = Offset(0f, 0f)
        
        // Clear history
        undoStack.clear()
        redoStack.clear()
    }

    private fun saveCurrentBoardData() {
        val board = _currentBoard.value ?: return
        val elements = _boardElements.value
        
        viewModelScope.launch {
            val jsonArr = JSONArray()
            for (el in elements) {
                jsonArr.put(el.toJson())
            }
            val updated = board.copy(
                elementsJson = jsonArr.toString(),
                updatedAt = System.currentTimeMillis()
            )
            repository.updateBoard(updated)
        }
    }

    fun saveActiveBoardState() {
        saveCurrentBoardData()
    }

    private fun pushHistoryState() {
        undoStack.add(_boardElements.value)
        redoStack.clear()
        if (undoStack.size > 25) {
            undoStack.removeAt(0) // limit size
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(_boardElements.value)
            val prevState = undoStack.removeAt(undoStack.size - 1)
            _boardElements.value = prevState
            _selectedElementId.value = null
            saveCurrentBoardData()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(_boardElements.value)
            val nextState = redoStack.removeAt(redoStack.size - 1)
            _boardElements.value = nextState
            _selectedElementId.value = null
            saveCurrentBoardData()
        }
    }

    // Tools Configuration
    fun setTool(tool: Tool) {
        _activeTool.value = tool
        _selectedElementId.value = null
        if (tool == Tool.Laser) {
            startLaserCleanup()
        } else {
            laserJob?.cancel()
            _laserTrail.value = emptyList()
        }
    }

    fun selectColor(color: Int) {
        _selectedColor.value = color
    }

    fun updateThickness(thickness: Float) {
        _brushThickness.value = thickness
    }

    fun selectShapeType(type: String) {
        _selectedShapeType.value = type
    }

    // Generate random curated color palette
    fun generateNextPalette() {
        val currentIndex = palettes.indexOf(activePalette.value)
        val nextIndex = (currentIndex + 1) % palettes.size
        activePalette.value = palettes[nextIndex]
        _selectedColor.value = palettes[nextIndex][1] // select some bold accent
    }

    // Create a brand new board
    fun createBoard(name: String, templateType: String? = null, isTutorial: Boolean = false) {
        viewModelScope.launch {
            val cleanName = name.ifBlank { "Untitled Whiteboard" }
            val elementsJsonString = if (templateType != null) {
                BoardTemplates.generateElementsJson(templateType)
            } else if (isTutorial) {
                generateTutorialElementsJson()
            } else {
                "[]"
            }
            
            val newBoard = BoardEntity(
                name = cleanName,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                backgroundType = if (templateType == "PLANNING") "RULED" else "GRID",
                elementsJson = elementsJsonString
            )
            val insertedId = repository.insertBoard(newBoard)
            val createdBoard = newBoard.copy(id = insertedId)
            
            // Auto load board if first launch or selected
            if (_currentBoard.value == null || !isTutorial) {
                loadBoard(createdBoard)
            }
        }
    }

    fun renameBoard(board: BoardEntity, newName: String) {
        viewModelScope.launch {
            val updated = board.copy(name = newName.ifBlank { "Board" }, updatedAt = System.currentTimeMillis())
            repository.updateBoard(updated)
            if (_currentBoard.value?.id == board.id) {
                _currentBoard.value = updated
            }
        }
    }

    fun changeBackgroundType(bgType: String) {
        val board = _currentBoard.value ?: return
        val updated = board.copy(backgroundType = bgType)
        _currentBoard.value = updated
        saveCurrentBoardData()
    }

    fun deleteBoard(boardId: Long) {
        viewModelScope.launch {
            repository.deleteBoardById(boardId)
            if (_currentBoard.value?.id == boardId) {
                _currentBoard.value = null
                _boardElements.value = emptyList()
            }
        }
    }

    fun toggleBoardLock() {
        val board = _currentBoard.value ?: return
        val updated = board.copy(isLocked = !board.isLocked)
        _currentBoard.value = updated
        saveCurrentBoardData()
    }

    // Core Drawing and Touch State Handlers
    fun onDrawStart(x: Float, y: Float) {
        val activeBoard = _currentBoard.value ?: return
        if (activeBoard.isLocked) return

        val cx = (x - _panOffset.value.x) / _zoomScale.value
        val cy = (y - _panOffset.value.y) / _zoomScale.value
        val snapped = snapPoint(cx, cy)

        pushHistoryState()

        when (val tool = _activeTool.value) {
            Tool.Pen, Tool.Pencil, Tool.Marker -> {
                val alphaVal = when (tool) {
                    Tool.Pencil -> 0.5f
                    Tool.Marker -> 0.35f
                    else -> 1.0f
                }
                val strokeType = when (tool) {
                    Tool.Pencil -> "pencil"
                    Tool.Marker -> "marker"
                    else -> "pen"
                }
                
                _currentStroke.value = StrokeElement(
                    id = UUID.randomUUID().toString(),
                    points = listOf(Point(cx, cy)),
                    color = _selectedColor.value,
                    width = _brushThickness.value * (if (tool == Tool.Marker) 3f else 1f),
                    alpha = alphaVal,
                    brushType = strokeType
                )
            }
            Tool.Shape -> {
                _activeShapeGuide.value = ShapeElement(
                    id = UUID.randomUUID().toString(),
                    shapeType = _selectedShapeType.value,
                    startX = snapped.x,
                    startY = snapped.y,
                    endX = snapped.x,
                    endY = snapped.y,
                    color = _selectedColor.value,
                    width = max(4f, _brushThickness.value)
                )
            }
            Tool.Laser -> {
                _laserTrail.value = listOf(Point(cx, cy))
            }
            Tool.Eraser -> {
                eraseElementAt(cx, cy)
            }
            Tool.Select -> {
                selectElementAt(cx, cy)
            }
            else -> {}
        }
    }

    fun onDrawDrag(x: Float, y: Float) {
        val activeBoard = _currentBoard.value ?: return
        if (activeBoard.isLocked) return

        val cx = (x - _panOffset.value.x) / _zoomScale.value
        val cy = (y - _panOffset.value.y) / _zoomScale.value
        val snapped = snapPoint(cx, cy)

        when (_activeTool.value) {
            Tool.Pen, Tool.Pencil, Tool.Marker -> {
                val stroke = _currentStroke.value ?: return
                _currentStroke.value = stroke.copy(points = stroke.points + Point(cx, cy))
            }
            Tool.Shape -> {
                val shape = _activeShapeGuide.value ?: return
                _activeShapeGuide.value = shape.copy(endX = snapped.x, endY = snapped.y)
            }
            Tool.Laser -> {
                _laserTrail.value = _laserTrail.value + Point(cx, cy)
            }
            Tool.Eraser -> {
                eraseElementAt(cx, cy)
            }
            else -> {}
        }
    }

    fun onDrawEnd() {
        val activeBoard = _currentBoard.value ?: return
        if (activeBoard.isLocked) return

        when (_activeTool.value) {
            Tool.Pen, Tool.Pencil, Tool.Marker -> {
                val stroke = _currentStroke.value ?: return
                if (stroke.points.size > 2) {
                    if (isAutoShapeCorrectionEnabled.value) {
                        val corrected = attemptAutoShapeCorrection(stroke)
                        _boardElements.value = _boardElements.value + corrected
                    } else {
                        _boardElements.value = _boardElements.value + stroke
                    }
                }
                _currentStroke.value = null
                saveCurrentBoardData()
            }
            Tool.Shape -> {
                val shape = _activeShapeGuide.value ?: return
                // Check if drawn shape has positive dimensions
                _boardElements.value = _boardElements.value + shape
                _activeShapeGuide.value = null
                saveCurrentBoardData()
            }
            else -> {}
        }
    }

    // Geometry snap assistance
    private fun snapPoint(x: Float, y: Float): Point {
        if (!isSnapToGrid.value) return Point(x, y)
        val gridSize = 35f
        val sx = Math.round(x / gridSize) * gridSize
        val sy = Math.round(y / gridSize) * gridSize
        return Point(sx, sy)
    }

    // Auto-straightening of rough vectors
    private fun attemptAutoShapeCorrection(stroke: StrokeElement): BoardElement {
        val points = stroke.points
        if (points.size < 6) return stroke
        
        val first = points.first()
        val last = points.last()
        val bounds = stroke.getBounds()
        val w = bounds.right - bounds.left
        val h = bounds.bottom - bounds.top
        
        val startEndDist = hypot(first.x - last.x, first.y - last.y)
        
        // Loop detected: Replace with Circle or Rectangle
        if (startEndDist < 80f && w > 40f && h > 40f) {
            val eccentricity = abs(w - h)
            return if (eccentricity < 50f) {
                // Circle
                ShapeElement(
                    id = UUID.randomUUID().toString(),
                    shapeType = "circle",
                    startX = bounds.left, startY = bounds.top,
                    endX = bounds.right, endY = bounds.bottom,
                    color = stroke.color, width = stroke.width
                )
            } else {
                // Rectangle
                ShapeElement(
                    id = UUID.randomUUID().toString(),
                    shapeType = "rectangle",
                    startX = bounds.left, startY = bounds.top,
                    endX = bounds.right, endY = bounds.bottom,
                    color = stroke.color, width = stroke.width
                )
            }
        }
        
        // Linear path detected, replace with perfect line
        if (points.size > 10) {
            // Rough straight line
            return ShapeElement(
                id = UUID.randomUUID().toString(),
                shapeType = "line",
                startX = first.x, startY = first.y,
                endX = last.x, endY = last.y,
                color = stroke.color, width = stroke.width
            )
        }
        
        return stroke
    }

    // Tap/Drag Elements selection
    fun selectElementAt(x: Float, y: Float) {
        val elements = _boardElements.value
        // Search in reverse to catch top elements first
        for (i in elements.indices.reversed()) {
            val el = elements[i]
            val bounds = el.getBounds()
            // Add custom padding margin for checking touches on thin elements
            val margin = if (el is StrokeElement) 30f else 15f
            if (bounds.containsWithMargin(x, y, margin)) {
                _selectedElementId.value = el.id
                return
            }
        }
        _selectedElementId.value = null
    }

    // Stroke-by-stroke instant eraser
    private fun eraseElementAt(x: Float, y: Float) {
        val elements = _boardElements.value
        val toRemove = mutableListOf<BoardElement>()
        for (i in elements.indices.reversed()) {
            val el = elements[i]
            if (el.isLocked) continue
            val bounds = el.getBounds()
            val margin = if (el is StrokeElement) 35f else 15f
            if (bounds.containsWithMargin(x, y, margin)) {
                toRemove.add(el)
                break // stroke erase removes first hit element
            }
        }
        if (toRemove.isNotEmpty()) {
            _boardElements.value = elements.filterNot { it in toRemove }
            saveCurrentBoardData()
        }
    }

    // Drag move operations on selected element
    fun dragSelectedElement(dx: Float, dy: Float) {
        val activeBoard = _currentBoard.value ?: return
        if (activeBoard.isLocked) return

        val selId = _selectedElementId.value ?: return
        _boardElements.value = _boardElements.value.map { el ->
            if (el.id == selId && !el.isLocked) {
                el.translate(dx, dy)
            } else {
                el
            }
        }
    }

    fun resizeSelectedElement(newWidth: Float, newHeight: Float) {
        val activeBoard = _currentBoard.value ?: return
        if (activeBoard.isLocked) return

        val selId = _selectedElementId.value ?: return
        _boardElements.value = _boardElements.value.map { el ->
            if (el.id == selId && !el.isLocked) {
                el.resize(newWidth, newHeight)
            } else {
                el
            }
        }
    }

    fun deleteSelectedElement() {
        val activeBoard = _currentBoard.value ?: return
        if (activeBoard.isLocked) return

        val selId = _selectedElementId.value ?: return
        pushHistoryState()
        _boardElements.value = _boardElements.value.filterNot { it.id == selId }
        _selectedElementId.value = null
        saveCurrentBoardData()
    }

    // Bring forward/backward
    fun moveSelectedToFront() {
        val selId = _selectedElementId.value ?: return
        val elements = _boardElements.value.toMutableList()
        val index = elements.indexOfFirst { it.id == selId }
        if (index != -1 && index < elements.size - 1) {
            pushHistoryState()
            val item = elements.removeAt(index)
            elements.add(item)
            _boardElements.value = elements
            saveCurrentBoardData()
        }
    }

    fun moveSelectedToBack() {
        val selId = _selectedElementId.value ?: return
        val elements = _boardElements.value.toMutableList()
        val index = elements.indexOfFirst { it.id == selId }
        if (index > 0) {
            pushHistoryState()
            val item = elements.removeAt(index)
            elements.add(0, item)
            _boardElements.value = elements
            saveCurrentBoardData()
        }
    }

    fun toggleSelectedLockState() {
        val selId = _selectedElementId.value ?: return
        _boardElements.value = _boardElements.value.map { el ->
            if (el.id == selId) {
                el.setLocked(!el.isLocked)
            } else {
                el
            }
        }
        saveCurrentBoardData()
    }

    // Non-drawing standard Element creations
    fun addStickyNote(text: String, noteColorHex: Int = 0xFFFFF9C4.toInt()) {
        val activeBoard = _currentBoard.value ?: return
        if (activeBoard.isLocked) return

        pushHistoryState()
        // Center-oriented positioning relative to the zoom / scale
        val mapX = (-_panOffset.value.x + 300f) / _zoomScale.value
        val mapY = (-_panOffset.value.y + 400f) / _zoomScale.value
        
        val newSticky = StickyNoteElement(
            id = UUID.randomUUID().toString(),
            noteText = text.ifEmpty { "Sticky Note\n(double tap to edit)" },
            x = mapX,
            y = mapY,
            bgColor = noteColorHex
        )
        _boardElements.value = _boardElements.value + newSticky
        saveCurrentBoardData()
    }

    fun addTextElement(text: String, size: Float = 16f, color: Int = _selectedColor.value) {
        val activeBoard = _currentBoard.value ?: return
        if (activeBoard.isLocked) return

        pushHistoryState()
        val mapX = (-_panOffset.value.x + 300f) / _zoomScale.value
        val mapY = (-_panOffset.value.y + 400f) / _zoomScale.value

        val newText = TextElement(
            id = UUID.randomUUID().toString(),
            text = text.ifEmpty { "Text card (double tap to edit)" },
            x = mapX,
            y = mapY,
            color = color,
            fontSize = size
        )
        _boardElements.value = _boardElements.value + newText
        saveCurrentBoardData()
    }

    fun addImageElement(uri: Uri) {
        val activeBoard = _currentBoard.value ?: return
        if (activeBoard.isLocked) return

        pushHistoryState()
        val mapX = (-_panOffset.value.x + 200f) / _zoomScale.value
        val mapY = (-_panOffset.value.y + 300f) / _zoomScale.value

        val newImg = ImageElement(
            id = UUID.randomUUID().toString(),
            uriString = uri.toString(),
            x = mapX,
            y = mapY
        )
        _boardElements.value = _boardElements.value + newImg
        saveCurrentBoardData()
    }

    // Update specific text inside elements
    fun updateTextElementContent(id: String, text: String) {
        _boardElements.value = _boardElements.value.map { el ->
            if (el.id == id && el is TextElement) {
                el.copy(text = text)
            } else {
                el
            }
        }
        saveCurrentBoardData()
    }

    fun updateStickyNoteContent(id: String, text: String) {
        _boardElements.value = _boardElements.value.map { el ->
            if (el.id == id && el is StickyNoteElement) {
                el.copy(noteText = text)
            } else {
                el
            }
        }
        saveCurrentBoardData()
    }

    // Laser fading pointer coroutine
    private fun startLaserCleanup() {
        laserJob?.cancel()
        laserJob = viewModelScope.launch {
            while (true) {
                delay(30)
                val trail = _laserTrail.value
                if (trail.isNotEmpty()) {
                    // Fade out points by removing elements progressively from the front
                    _laserTrail.value = trail.drop(1)
                }
            }
        }
    }

    // Canvas panning and zooming
    fun scaleCanvas(scaleFactor: Float) {
        val activeBoard = _currentBoard.value ?: return
        val currentScale = _zoomScale.value
        val targetScale = (currentScale * scaleFactor).coerceIn(0.15f, 8f)
        _zoomScale.value = targetScale
    }

    fun panCanvas(dx: Float, dy: Float) {
        _panOffset.value = Offset(_panOffset.value.x + dx, _panOffset.value.y + dy)
    }

    fun resetCanvasTransform() {
        _zoomScale.value = 1f
        _panOffset.value = Offset(0f, 0f)
    }

    fun clearActiveBoard() {
        val activeBoard = _currentBoard.value ?: return
        if (activeBoard.isLocked) return

        pushHistoryState()
        _boardElements.value = emptyList()
        _selectedElementId.value = null
        saveCurrentBoardData()
    }

    // Local JSON Export/Import Sharing
    fun generateBoardJsonShareString(): String? {
        val board = _currentBoard.value ?: return null
        val elements = _boardElements.value
        return try {
            val json = JSONObject()
            json.put("boardly_format_version", 1)
            json.put("name", board.name)
            json.put("backgroundType", board.backgroundType)
            
            val array = JSONArray()
            for (el in elements) {
                array.put(el.toJson())
            }
            json.put("elements", array)
            json.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun importBoardJsonString(jsonStr: String): Boolean {
        return try {
            val json = JSONObject(jsonStr)
            val name = json.optString("name", "Imported Board")
            val bgType = json.optString("backgroundType", "GRID")
            val elementsArray = json.getJSONArray("elements")
            
            createBoard("[Imported] $name")
            
            // Allow DB update delay before replacing loaded elements
            viewModelScope.launch {
                delay(300)
                val elements = mutableListOf<BoardElement>()
                for (i in 0 until elementsArray.length()) {
                    val el = BoardElement.fromJson(elementsArray.getJSONObject(i))
                    if (el != null) {
                        elements.add(el)
                    }
                }
                _boardElements.value = elements
                saveCurrentBoardData()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // PNG Offline Bitmap rendering
    fun exportBoardAsPngBitmap(): Bitmap {
        // High density bitmap, 1200x1600 rendering resolution
        val width = 1200
        val height = 1600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw whiteboard canvas background (dark/light matching style)
        val bgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Draw grid lines
        val gridPaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        val currentBgType = _currentBoard.value?.backgroundType ?: "GRID"
        if (currentBgType == "GRID") {
            val size = 50f
            var x = 0f
            while (x < width) {
                canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
                x += size
            }
            var y = 0f
            while (y < height) {
                canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
                y += size
            }
        } else if (currentBgType == "RULED") {
            val size = 40f
            var y = 80f
            while (y < height) {
                canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
                y += size
            }
        }

        // Render all elements sequentially (painter layer order)
        val paint = Paint().apply {
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        for (el in _boardElements.value) {
            when (el) {
                is StrokeElement -> {
                    if (el.points.size > 1) {
                        paint.color = el.color
                        paint.strokeWidth = el.width
                        paint.style = Paint.Style.STROKE
                        paint.alpha = (el.alpha * 255).toInt()
                        
                        val iterator = el.points.iterator()
                        var prev = iterator.next()
                        while (iterator.hasNext()) {
                            val curr = iterator.next()
                            canvas.drawLine(prev.x, prev.y, curr.x, curr.y, paint)
                            prev = curr
                        }
                    }
                }
                is ShapeElement -> {
                    paint.color = el.color
                    paint.strokeWidth = el.width
                    paint.style = Paint.Style.STROKE
                    paint.alpha = 255
                    
                    val left = min(el.startX, el.endX)
                    val top = min(el.startY, el.endY)
                    val right = max(el.startX, el.endX)
                    val bottom = max(el.startY, el.endY)
                    
                    when (el.shapeType) {
                        "circle" -> {
                            val r = hypot(el.endX - el.startX, el.endY - el.startY) / 2
                            val cx = (el.startX + el.endX) / 2
                            val cy = (el.startY + el.endY) / 2
                            canvas.drawCircle(cx, cy, r, paint)
                        }
                        "rectangle" -> {
                            canvas.drawRect(left, top, right, bottom, paint)
                        }
                        "line" -> {
                            canvas.drawLine(el.startX, el.startY, el.endX, el.endY, paint)
                        }
                        "arrow" -> {
                            canvas.drawLine(el.startX, el.startY, el.endX, el.endY, paint)
                            // Draw Arrowhead
                            val dx = el.endX - el.startX
                            val dy = el.endY - el.startY
                            val len = hypot(dx, dy)
                            if (len > 10) {
                                val ux = dx / len
                                val uy = dy / len
                                val arrowLength = 25f
                                
                                val turnAngle = 0.5f // roughly 30 deg
                                val ax = el.endX - ux * arrowLength
                                val ay = el.endY - uy * arrowLength
                                
                                val rx = ax + (-uy) * arrowLength * turnAngle
                                val ry = ay + ux * arrowLength * turnAngle
                                val lx = ax - (-uy) * arrowLength * turnAngle
                                val ly = ay - ux * arrowLength * turnAngle
                                
                                canvas.drawLine(el.endX, el.endY, rx, ry, paint)
                                canvas.drawLine(el.endX, el.endY, lx, ly, paint)
                            }
                        }
                    }
                }
                is StickyNoteElement -> {
                    // Draw Solid Pastel Sticky note background
                    val stickyPaint = Paint().apply {
                        color = el.bgColor
                        style = Paint.Style.FILL
                    }
                    canvas.drawRect(el.x, el.y, el.x + el.width, el.y + el.height, stickyPaint)
                    
                    // Draw simple outline
                    stickyPaint.color = Color.DKGRAY
                    stickyPaint.style = Paint.Style.STROKE
                    stickyPaint.strokeWidth = 2f
                    canvas.drawRect(el.x, el.y, el.x + el.width, el.y + el.height, stickyPaint)

                    // Text
                    val textPaint = Paint().apply {
                        color = Color.BLACK
                        textSize = 20f
                        isAntiAlias = true
                    }
                    val lines = el.noteText.split("\n")
                    var startY = el.y + 40f
                    for (line in lines) {
                        if (startY > el.y + el.height - 15f) break
                        canvas.drawText(
                            if (line.length > 20) line.take(18) + ".." else line,
                            el.x + 15f,
                            startY,
                            textPaint
                        )
                        startY += 30f
                    }
                }
                is TextElement -> {
                    val textPaint = Paint().apply {
                        color = el.color
                        textSize = el.fontSize * 1.5f // scale for high density exporting
                        isAntiAlias = true
                    }
                    canvas.drawText(el.text, el.x, el.y + el.fontSize * 1.5f, textPaint)
                }
                is ImageElement -> {
                    // Image files loading are heavy so we render placeholder on bitmap
                    paint.color = Color.LTGRAY
                    paint.style = Paint.Style.FILL
                    canvas.drawRect(el.x, el.y, el.x + el.width, el.y + el.height, paint)
                    paint.color = Color.GRAY
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 2f
                    canvas.drawRect(el.x, el.y, el.x + el.width, el.y + el.height, paint)
                    
                    val textPaint = Paint().apply {
                        color = Color.DKGRAY
                        textSize = 20f
                        isAntiAlias = true
                    }
                    canvas.drawText("[Inserted Image]", el.x + 20f, el.y + el.height / 2, textPaint)
                }
            }
        }
        return bitmap
    }

    private fun generateTutorialElementsJson(): String {
        val array = JSONArray()
        try {
            // Title Header
            array.put(TextElement(
                id = UUID.randomUUID().toString(),
                text = "🖊️ Welcome to Boardly!",
                x = 100f, y = 50f,
                color = 0xFF6200EE.toInt(),
                fontSize = 26f,
                width = 500f, height = 70f
            ).toJson())

            // Tutorial Subtext Info Card
            array.put(TextElement(
                id = UUID.randomUUID().toString(),
                text = "An infinite canvas vector whiteboard tailored for brainstorming classes, lecture notes, and diagrams completely offline.",
                x = 100f, y = 130f,
                color = 0xFF757575.toInt(),
                fontSize = 15f,
                width = 650f, height = 110f
            ).toJson())

            // Core features note
            array.put(StickyNoteElement(
                id = UUID.randomUUID().toString(),
                noteText = "💡 Quick Tips:\n\n1. Select tools below (Pen, Pencil, Shapes, Sticky, Text)\n2. Long press & drag on Sticky/Text cards to slide them!\n3. Select Mode enables Resize & Layer depth controls.",
                x = 100f, y = 280f,
                bgColor = 0xFFFFFFF0.toInt() // Ivory cream
            ).toJson())

            // Shape alignment diagram
            array.put(ShapeElement(
                id = UUID.randomUUID().toString(),
                shapeType = "circle",
                startX = 460f, startY = 300f,
                endX = 580f, endY = 420f,
                color = 0xFF3F51B5.toInt(),
                width = 5f
            ).toJson())

            // Interactive Arrow
            array.put(ShapeElement(
                id = UUID.randomUUID().toString(),
                shapeType = "arrow",
                startX = 330f, startY = 380f,
                endX = 430f, endY = 370f,
                color = 0xFFFF5722.toInt(),
                width = 4f
            ).toJson())

            // Sticky note 2: Developer details
            array.put(StickyNoteElement(
                id = UUID.randomUUID().toString(),
                noteText = "🚀 Offline First\n\nNo internet required. Elements are auto-saved to Room instantly. Export layouts safely as PNG anytime!",
                x = 440f, y = 490f,
                bgColor = 0xFFE8F5E9.toInt() // light green
            ).toJson())

            // Pre-drawn Smiley Face
            val smilePoints = listOf(
                Point(200f, 600f), Point(220f, 605f), Point(240f, 600f), // Eye
                Point(280f, 600f), Point(300f, 605f), Point(320f, 600f), // Eye 2
                Point(180f, 660f), Point(200f, 680f), Point(230f, 690f), Point(260f, 690f), Point(290f, 680f), Point(310f, 660f) // Smiling mouth
            )
            array.put(StrokeElement(
                id = UUID.randomUUID().toString(),
                points = smilePoints,
                color = 0xFF4CAF50.toInt(),
                width = 6f,
                alpha = 1.0f,
                brushType = "pen"
            ).toJson())

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return array.toString()
    }
}
