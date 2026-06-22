package com.example.model

import android.graphics.Color
import org.json.JSONArray
import java.util.UUID

object BoardTemplates {

    fun generateElementsJson(templateType: String): String {
        val array = JSONArray()
        try {
            when (templateType) {
                "STUDY" -> {
                    // Title Header
                    array.put(TextElement(
                        id = UUID.randomUUID().toString(),
                        text = "📚 Study Notes: Computer Science 101",
                        x = 100f, y = 50f,
                        color = 0xFF3700B3.toInt(),
                        fontSize = 24f,
                        width = 600f, height = 80f
                    ).toJson())
                    
                    // Subtitle
                    array.put(TextElement(
                        id = UUID.randomUUID().toString(),
                        text = "Topic: Binary Trees & Graph Traversal",
                        x = 100f, y = 130f,
                        color = 0xFF6200EE.toInt(),
                        fontSize = 16f,
                        width = 450f, height = 60f
                    ).toJson())

                    // Sticky note: Definition
                    array.put(StickyNoteElement(
                        id = UUID.randomUUID().toString(),
                        noteText = "Definition:\n\nA Binary Tree is a tree data structure in which each node has at most two children, referred to as the left child and the right child.",
                        x = 100f, y = 220f,
                        bgColor = 0xFFFFF9C4.toInt() // Soft Yellow
                    ).toJson())

                    // Sticky note: Key Complexity
                    array.put(StickyNoteElement(
                        id = UUID.randomUUID().toString(),
                        noteText = "DFS vs BFS\n\n- DFS uses a Stack (LIFO) or Recursion\n- BFS uses a Queue (FIFO)",
                        x = 400f, y = 220f,
                        bgColor = 0xFFE1F5FE.toInt() // Soft Blue
                    ).toJson())

                    // Custom rectangle grouping Key Algorithms
                    array.put(ShapeElement(
                        id = UUID.randomUUID().toString(),
                        shapeType = "rectangle",
                        startX = 100f, startY = 490f,
                        endX = 640f, endY = 700f,
                        color = 0xFF4CAF50.toInt(),
                        width = 4f
                    ).toJson())

                    array.put(TextElement(
                        id = UUID.randomUUID().toString(),
                        text = "💡 Study Tip: Practice drawing the trees with hand-drawn pen sketches on the active canvas!",
                        x = 120f, y = 510f,
                        color = 0xFF388E3C.toInt(),
                        fontSize = 14f,
                        width = 500f, height = 150f
                    ).toJson())
                }
                "MINDMAP" -> {
                    // Central Root Node
                    val rootId = UUID.randomUUID().toString()
                    array.put(ShapeElement(
                        id = rootId,
                        shapeType = "circle",
                        startX = 350f, startY = 250f,
                        endX = 550f, endY = 400f,
                        color = 0xFFE91E63.toInt(), // Pink accent
                        width = 6f
                    ).toJson())

                    array.put(TextElement(
                        id = UUID.randomUUID().toString(),
                        text = "My Business\nIdea",
                        x = 380f, y = 290f,
                        color = 0xFFE91E63.toInt(),
                        fontSize = 18f,
                        width = 150f, height = 80f
                    ).toJson())

                    // Node A: Marketing
                    val nodeAId = UUID.randomUUID().toString()
                    array.put(ShapeElement(
                        id = nodeAId,
                        shapeType = "rectangle",
                        startX = 50f, startY = 100f,
                        endX = 250f, endY = 200f,
                        color = 0xFF2196F3.toInt(), // Blue accent
                        width = 4f
                    ).toJson())

                    array.put(TextElement(
                        id = UUID.randomUUID().toString(),
                        text = "📱 Marketing Strategy\n- Social Media\n- Direct Outreach",
                        x = 60f, y = 110f,
                        color = 0xFF0D47A1.toInt(),
                        fontSize = 13f,
                        width = 180f, height = 80f
                    ).toJson())

                    // Node B: Operations
                    val nodeBId = UUID.randomUUID().toString()
                    array.put(ShapeElement(
                        id = nodeBId,
                        shapeType = "rectangle",
                        startX = 650f, startY = 380f,
                        endX = 850f, endY = 480f,
                        color = 0xFF4CAF50.toInt(), // Green accent
                        width = 4f
                    ).toJson())

                    array.put(TextElement(
                        id = UUID.randomUUID().toString(),
                        text = "⚙️ Dev & Operations\n- Offline First\n- Privacy Focused",
                        x = 660f, y = 390f,
                        color = 0xFF1B5E20.toInt(),
                        fontSize = 13f,
                        width = 185f, height = 80f
                    ).toJson())

                    // Connection Arrows (Center outward)
                    array.put(ShapeElement(
                        id = UUID.randomUUID().toString(),
                        shapeType = "arrow",
                        startX = 350f, startY = 250f,
                        endX = 250f, endY = 200f,
                        color = 0xFF3F51B5.toInt(),
                        width = 5f
                    ).toJson())

                    array.put(ShapeElement(
                        id = UUID.randomUUID().toString(),
                        shapeType = "arrow",
                        startX = 550f, startY = 400f,
                        endX = 650f, endY = 430f,
                        color = 0xFF3F51B5.toInt(),
                        width = 5f
                    ).toJson())

                    // Background Info Header
                    array.put(TextElement(
                        id = UUID.randomUUID().toString(),
                        text = "🧠 Brainstorming Mindmap Template",
                        x = 100f, y = 30f,
                        color = 0xFFE91E63.toInt(),
                        fontSize = 22f,
                        width = 500f, height = 60f
                    ).toJson())
                }
                "PLANNING" -> {
                    // Agile Kanban Board Planning
                    array.put(TextElement(
                        id = UUID.randomUUID().toString(),
                        text = "📋 Kanban Planning Board",
                        x = 50f, y = 40f,
                        color = 0xFF00796B.toInt(),
                        fontSize = 24f,
                        width = 400f, height = 60f
                    ).toJson())

                    // Three columns (To Do, In Progress, Done)
                    // Col 1: TO DO
                    array.put(ShapeElement(
                        id = UUID.randomUUID().toString(),
                        shapeType = "rectangle",
                        startX = 40f, startY = 120f,
                        endX = 300f, endY = 800f,
                        color = 0xFF757575.toInt(),
                        width = 4f
                    ).toJson())
                    array.put(TextElement(
                        id = UUID.randomUUID().toString(),
                        text = "📝 TO DO",
                        x = 60f, y = 140f,
                        color = 0xFF424242.toInt(),
                        fontSize = 18f,
                        width = 200f, height = 40f
                    ).toJson())
                    // Task note
                    array.put(StickyNoteElement(
                        id = UUID.randomUUID().toString(),
                        noteText = "Task:\nPrepare presentation slides for launch.",
                        x = 50f, y = 200f,
                        bgColor = 0xFFFFD180.toInt() // pastel orange
                    ).toJson())

                    // Col 2: IN PROGRESS
                    array.put(ShapeElement(
                        id = UUID.randomUUID().toString(),
                        shapeType = "rectangle",
                        startX = 330f, startY = 120f,
                        endX = 590f, endY = 800f,
                        color = 0xFF0288D1.toInt(),
                        width = 4f
                    ).toJson())
                    array.put(TextElement(
                        id = UUID.randomUUID().toString(),
                        text = "⚡ IN PROGRESS",
                        x = 350f, y = 140f,
                        color = 0xFF01579B.toInt(),
                        fontSize = 18f,
                        width = 220f, height = 40f
                    ).toJson())
                    // Task note
                    array.put(StickyNoteElement(
                        id = UUID.randomUUID().toString(),
                        noteText = "Sprint #1:\nDeveloping drawing engine for low latency.",
                        x = 340f, y = 200f,
                        bgColor = 0xFFE1F5FE.toInt() // light blue
                    ).toJson())

                    // Col 3: DONE
                    array.put(ShapeElement(
                        id = UUID.randomUUID().toString(),
                        shapeType = "rectangle",
                        startX = 620f, startY = 120f,
                        endX = 880f, endY = 800f,
                        color = 0xFF388E3C.toInt(),
                        width = 4f
                    ).toJson())
                    array.put(TextElement(
                        id = UUID.randomUUID().toString(),
                        text = "✅ DONE",
                        x = 640f, y = 140f,
                        color = 0xFF1B5E20.toInt(),
                        fontSize = 18f,
                        width = 200f, height = 40f
                    ).toJson())
                    // Task note
                    array.put(StickyNoteElement(
                        id = UUID.randomUUID().toString(),
                        noteText = "Completed:\nSetup local schema & Room Database components.",
                        x = 630f, y = 200f,
                        bgColor = 0xFFC8E6C9.toInt() // light green
                    ).toJson())
                }
                "LECTURE" -> {
                    // Solar System Schema
                    array.put(TextElement(
                        id = UUID.randomUUID().toString(),
                        text = "🌌 Physics Lecture: Orbital Mechanics",
                        x = 100f, y = 50f,
                        color = 0xFF303F9F.toInt(),
                        fontSize = 25f,
                        width = 500f, height = 80f
                    ).toJson())

                    // Sun Circle
                    array.put(ShapeElement(
                        id = UUID.randomUUID().toString(),
                        shapeType = "circle",
                        startX = 380f, startY = 250f,
                        endX = 520f, endY = 390f,
                        color = 0xFFFFEB3B.toInt(), // Yellow Sun
                        width = 5f
                    ).toJson())
                    array.put(TextElement(
                        id = UUID.randomUUID().toString(),
                        text = "Sun (M)",
                        x = 420f, y = 300f,
                        color = 0xFFE65100.toInt(),
                        fontSize = 16f,
                        width = 80f, height = 40f
                    ).toJson())

                    // Earth Circle
                    array.put(ShapeElement(
                        id = UUID.randomUUID().toString(),
                        shapeType = "circle",
                        startX = 700f, startY = 300f,
                        endX = 760f, endY = 360f,
                        color = 0xFF03A9F4.toInt(), // Blue Earth
                        width = 3f
                    ).toJson())
                    array.put(TextElement(
                        id = UUID.randomUUID().toString(),
                        text = "Earth (m)",
                        x = 695f, y = 370f,
                        color = 0xFF0D47A1.toInt(),
                        fontSize = 12f,
                        width = 80f, height = 30f
                    ).toJson())

                    // Centripetal velocity arrow
                    array.put(ShapeElement(
                        id = UUID.randomUUID().toString(),
                        shapeType = "line", // Orbit path ellipse, or line
                        startX = 730f, startY = 300f,
                        endX = 730f, endY = 180f,
                        color = 0xFFE91E63.toInt(), // Velocity vector
                        width = 4f
                    ).toJson())
                    array.put(ShapeElement(
                        id = UUID.randomUUID().toString(),
                        shapeType = "arrow", // Vector arrowhead
                        startX = 730f, startY = 190f,
                        endX = 730f, endY = 175f,
                        color = 0xFFE91E63.toInt(),
                        width = 4f
                    ).toJson())
                    array.put(TextElement(
                        id = UUID.randomUUID().toString(),
                        text = "Velocity Vector (V)",
                        x = 745f, y = 200f,
                        color = 0xFFE91E63.toInt(),
                        fontSize = 12f,
                        width = 150f, height = 30f
                    ).toJson())

                    // Gravitational force arrow
                    array.put(ShapeElement(
                        id = UUID.randomUUID().toString(),
                        shapeType = "arrow",
                        startX = 700f, startY = 330f,
                        endX = 520f, endY = 320f,
                        color = 0xFF4CAF50.toInt(), // Green Pull vector
                        width = 4f
                    ).toJson())
                    array.put(TextElement(
                        id = UUID.randomUUID().toString(),
                        text = "Gravity Force (Fg)",
                        x = 550f, y = 280f,
                        color = 0xFF1B5E20.toInt(),
                        fontSize = 12f,
                        width = 150f, height = 30f
                    ).toJson())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return array.toString()
    }
}
