package com.example.model

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

data class Point(val x: Float, val y: Float) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("x", x.toDouble())
        obj.put("y", y.toDouble())
        return obj
    }
}

data class BoardRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun contains(px: Float, py: Float): Boolean {
        return px in left..right && py in top..bottom
    }
    
    fun containsWithMargin(px: Float, py: Float, margin: Float): Boolean {
        return px in (left - margin)..(right + margin) && py in (top - margin)..(bottom + margin)
    }
}

sealed class BoardElement {
    abstract val id: String
    abstract val isLocked: Boolean
    
    abstract fun toJson(): JSONObject
    abstract fun translate(dx: Float, dy: Float): BoardElement
    abstract fun resize(newWidth: Float, newHeight: Float): BoardElement
    abstract fun getBounds(): BoardRect
    abstract fun setLocked(locked: Boolean): BoardElement

    companion object {
        fun fromJson(json: JSONObject): BoardElement? {
            return try {
                when (json.getString("type")) {
                    "stroke" -> {
                        val id = json.getString("id")
                        val color = json.getInt("color")
                        val width = json.getDouble("width").toFloat()
                        val alpha = json.getDouble("alpha").toFloat()
                        val brushType = json.getString("brushType")
                        val isLocked = json.optBoolean("isLocked", false)
                        val pointsArray = json.getJSONArray("points")
                        val points = mutableListOf<Point>()
                        for (i in 0 until pointsArray.length()) {
                            val pObj = pointsArray.getJSONObject(i)
                            points.add(Point(pObj.getDouble("x").toFloat(), pObj.optDouble("y").toFloat()))
                        }
                        StrokeElement(id, points, color, width, alpha, brushType, isLocked)
                    }
                    "shape" -> {
                        val id = json.getString("id")
                        val shapeType = json.getString("shapeType")
                        val startX = json.getDouble("startX").toFloat()
                        val startY = json.getDouble("startY").toFloat()
                        val endX = json.getDouble("endX").toFloat()
                        val endY = json.getDouble("endY").toFloat()
                        val color = json.getInt("color")
                        val width = json.getDouble("width").toFloat()
                        val isLocked = json.optBoolean("isLocked", false)
                        ShapeElement(id, shapeType, startX, startY, endX, endY, color, width, isLocked)
                    }
                    "text" -> {
                        val id = json.getString("id")
                        val text = json.getString("text")
                        val x = json.getDouble("x").toFloat()
                        val y = json.getDouble("y").toFloat()
                        val color = json.getInt("color")
                        val fontSize = json.getDouble("fontSize").toFloat()
                        val width = json.optDouble("width", 250.0).toFloat()
                        val height = json.optDouble("height", 120.0).toFloat()
                        val isLocked = json.optBoolean("isLocked", false)
                        TextElement(id, text, x, y, color, fontSize, width, height, isLocked)
                    }
                    "sticky" -> {
                        val id = json.getString("id")
                        val noteText = json.getString("noteText")
                        val x = json.getDouble("x").toFloat()
                        val y = json.getDouble("y").toFloat()
                        val bgColor = json.getInt("bgColor")
                        val width = json.optDouble("width", 240.0).toFloat()
                        val height = json.optDouble("height", 240.0).toFloat()
                        val isLocked = json.optBoolean("isLocked", false)
                        StickyNoteElement(id, noteText, x, y, bgColor, width, height, isLocked)
                    }
                    "image" -> {
                        val id = json.getString("id")
                        val uriString = json.getString("uriString")
                        val x = json.getDouble("x").toFloat()
                        val y = json.getDouble("y").toFloat()
                        val width = json.optDouble("width", 350.0).toFloat()
                        val height = json.optDouble("height", 350.0).toFloat()
                        val isLocked = json.optBoolean("isLocked", false)
                        ImageElement(id, uriString, x, y, width, height, isLocked)
                    }
                    else -> null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}

data class StrokeElement(
    override val id: String,
    val points: List<Point>,
    val color: Int,
    val width: Float,
    val alpha: Float,
    val brushType: String, // "pen", "pencil", "marker", "laser"
    override val isLocked: Boolean = false
) : BoardElement() {
    
    override fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("type", "stroke")
        obj.put("id", id)
        obj.put("color", color)
        obj.put("width", width.toDouble())
        obj.put("alpha", alpha.toDouble())
        obj.put("brushType", brushType)
        obj.put("isLocked", isLocked)
        
        val arr = JSONArray()
        for (p in points) {
            arr.put(p.toJson())
        }
        obj.put("points", arr)
        return obj
    }

    override fun translate(dx: Float, dy: Float): BoardElement {
        if (isLocked) return this
        return this.copy(points = points.map { Point(it.x + dx, it.y + dy) })
    }

    override fun resize(newWidth: Float, newHeight: Float): BoardElement {
        // Strokes don't typically resize standardly, but we can scale points relative to bounding box
        if (isLocked || points.isEmpty()) return this
        val bounds = getBounds()
        val currentWidth = max(5f, bounds.right - bounds.left)
        val currentHeight = max(5f, bounds.bottom - bounds.top)
        
        val scaleX = newWidth / currentWidth
        val scaleY = newHeight / currentHeight
        
        val newPoints = points.map { p ->
            val rx = p.x - bounds.left
            val ry = p.y - bounds.top
            Point(bounds.left + rx * scaleX, bounds.top + ry * scaleY)
        }
        return this.copy(points = newPoints)
    }

    override fun getBounds(): BoardRect {
        if (points.isEmpty()) return BoardRect(0f, 0f, 0f, 0f)
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        for (p in points) {
            if (p.x < minX) minX = p.x
            if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y
            if (p.y > maxY) maxY = p.y
        }
        return BoardRect(minX, minY, maxX, maxY)
    }

    override fun setLocked(locked: Boolean): BoardElement {
        return this.copy(isLocked = locked)
    }
}

data class ShapeElement(
    override val id: String,
    val shapeType: String, // "circle", "rectangle", "arrow", "line"
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val color: Int,
    val width: Float,
    override val isLocked: Boolean = false
) : BoardElement() {

    override fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("type", "shape")
        obj.put("id", id)
        obj.put("shapeType", shapeType)
        obj.put("startX", startX.toDouble())
        obj.put("startY", startY.toDouble())
        obj.put("endX", endX.toDouble())
        obj.put("endY", endY.toDouble())
        obj.put("color", color)
        obj.put("width", width.toDouble())
        obj.put("isLocked", isLocked)
        return obj
    }

    override fun translate(dx: Float, dy: Float): BoardElement {
        if (isLocked) return this
        return this.copy(
            startX = startX + dx,
            startY = startY + dy,
            endX = endX + dx,
            endY = endY + dy
        )
    }

    override fun resize(newWidth: Float, newHeight: Float): BoardElement {
        if (isLocked) return this
        val left = min(startX, endX)
        val top = min(startY, endY)
        
        // Retain sign to maintain layout orientation direction
        val newEndX = if (endX >= startX) startX + newWidth else startX - newWidth
        val newEndY = if (endY >= startY) startY + newHeight else startY - newHeight
        
        return this.copy(endX = newEndX, endY = newEndY)
    }

    override fun getBounds(): BoardRect {
        val left = min(startX, endX)
        val right = max(startX, endX)
        val top = min(startY, endY)
        val bottom = max(startY, endY)
        return BoardRect(
            left = left,
            top = top,
            right = max(left + 15f, right),
            bottom = max(top + 15f, bottom)
        )
    }

    override fun setLocked(locked: Boolean): BoardElement {
        return this.copy(isLocked = locked)
    }
}

data class TextElement(
    override val id: String,
    val text: String,
    val x: Float,
    val y: Float,
    val color: Int,
    val fontSize: Float,
    val width: Float = 250f,
    val height: Float = 120f,
    override val isLocked: Boolean = false
) : BoardElement() {

    override fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("type", "text")
        obj.put("id", id)
        obj.put("text", text)
        obj.put("x", x.toDouble())
        obj.put("y", y.toDouble())
        obj.put("color", color)
        obj.put("fontSize", fontSize.toDouble())
        obj.put("width", width.toDouble())
        obj.put("height", height.toDouble())
        obj.put("isLocked", isLocked)
        return obj
    }

    override fun translate(dx: Float, dy: Float): BoardElement {
        if (isLocked) return this
        return this.copy(x = x + dx, y = y + dy)
    }

    override fun resize(newWidth: Float, newHeight: Float): BoardElement {
        if (isLocked) return this
        return this.copy(
            width = max(80f, newWidth),
            height = max(40f, newHeight),
            fontSize = max(10f, min(36f, newHeight * 0.25f)) // dynamic font scale
        )
    }

    override fun getBounds(): BoardRect {
        return BoardRect(x, y, x + width, y + height)
    }

    override fun setLocked(locked: Boolean): BoardElement {
        return this.copy(isLocked = locked)
    }
}

data class StickyNoteElement(
    override val id: String,
    val noteText: String,
    val x: Float,
    val y: Float,
    val bgColor: Int, // Hex Color Int representation (e.g. pastel colors)
    val width: Float = 240f,
    val height: Float = 240f,
    override val isLocked: Boolean = false
) : BoardElement() {

    override fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("type", "sticky")
        obj.put("id", id)
        obj.put("noteText", noteText)
        obj.put("x", x.toDouble())
        obj.put("y", y.toDouble())
        obj.put("bgColor", bgColor)
        obj.put("width", width.toDouble())
        obj.put("height", height.toDouble())
        obj.put("isLocked", isLocked)
        return obj
    }

    override fun translate(dx: Float, dy: Float): BoardElement {
        if (isLocked) return this
        return this.copy(x = x + dx, y = y + dy)
    }

    override fun resize(newWidth: Float, newHeight: Float): BoardElement {
        if (isLocked) return this
        val bounded = max(120f, min(newWidth, newHeight)) // keep square-ish
        return this.copy(width = bounded, height = bounded)
    }

    override fun getBounds(): BoardRect {
        return BoardRect(x, y, x + width, y + height)
    }

    override fun setLocked(locked: Boolean): BoardElement {
        return this.copy(isLocked = locked)
    }
}

data class ImageElement(
    override val id: String,
    val uriString: String,
    val x: Float,
    val y: Float,
    val width: Float = 350f,
    val height: Float = 350f,
    override val isLocked: Boolean = false
) : BoardElement() {

    override fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("type", "image")
        obj.put("id", id)
        obj.put("uriString", uriString)
        obj.put("x", x.toDouble())
        obj.put("y", y.toDouble())
        obj.put("width", width.toDouble())
        obj.put("height", height.toDouble())
        obj.put("isLocked", isLocked)
        return obj
    }

    override fun translate(dx: Float, dy: Float): BoardElement {
        if (isLocked) return this
        return this.copy(x = x + dx, y = y + dy)
    }

    override fun resize(newWidth: Float, newHeight: Float): BoardElement {
        if (isLocked) return this
        return this.copy(width = max(80f, newWidth), height = max(80f, newHeight))
    }

    override fun getBounds(): BoardRect {
        return BoardRect(x, y, x + width, y + height)
    }

    override fun setLocked(locked: Boolean): BoardElement {
        return this.copy(isLocked = locked)
    }
}
