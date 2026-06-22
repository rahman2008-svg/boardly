package com.example.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.BoardViewModel
import com.example.data.BoardEntity
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardListScreen(
    viewModel: BoardViewModel,
    onBoardSelected: (BoardEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val boards by viewModel.allBoards.collectAsState()
    val searchQuery by viewModel.boardSearchQuery.collectAsState()
    
    // Dialog setups
    var showCreateDialog by remember { mutableStateOf(false) }
    var createBoardName by remember { mutableStateOf("") }
    
    var showRenameDialog by remember { mutableStateOf<BoardEntity?>(null) }
    var renameValue by remember { mutableStateOf("") }
    
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonValue by remember { mutableStateOf("") }

    var showDeveloperDialog by remember { mutableStateOf(false) }

    // Filtered board list
    val filteredBoards = boards.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }.sortedByDescending { it.updatedAt }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFF7F9FC) // Premium slate-light background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            
            // 1. Header with Bento Branding logo "B" and developer options
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF005CBB), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "B",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                    Text(
                        text = "Boardly",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color(0xFF0F172A),
                        letterSpacing = (-0.5).sp
                    )
                }
                IconButton(
                    onClick = { showDeveloperDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFE2E8F0), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Developer Hub Info",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 2. High-contrast Outlined Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.boardSearchQuery.value = it },
                placeholder = { Text("Search offline whiteboards...", color = Color(0xFF94A3B8)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon", tint = Color(0xFF64748B)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFF005CBB),
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Bento Grid - Block A: Start Fresh / New Blank whiteboard card
            Card(
                onClick = {
                    createBoardName = ""
                    showCreateDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDBEAFE))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFF005CBB), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New blank board",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFDBEAFE), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "START FRESH",
                                color = Color(0xFF1D4ED8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "New Blank Board",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Infinite canvas • Offline workspace",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // 4. Bento Grid - Blocks B, C, D (Row of Recent Preview and quick templates / status combo)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left 50% Column: Recent Board card
                RecentBoardBentoCard(
                    recentBoard = boards.maxByOrNull { it.updatedAt },
                    onClick = onBoardSelected,
                    modifier = Modifier.weight(1f)
                )

                // Right 50% Column: Mind Map template & Storage status cards
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MindMapTemplateBentoCard(
                        onClick = {
                            viewModel.createBoard("🧠 Brainstorming Blueprint Mindmap", templateType = "MINDMAP")
                        }
                    )
                    DeviceStorageBentoCard(
                        boardCount = boards.size
                    )
                }
            }

            // 5. Bento Grid - Blocks E, F (Optional details)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ImportFileBentoCard(
                    onClick = {
                        importJsonValue = ""
                        showImportDialog = true
                    },
                    modifier = Modifier.weight(1f)
                )
                AuthorBentoCard(
                    onClick = {
                        showDeveloperDialog = true
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // 6. Horizontal Scroll Quick Blueprint templates list section (Preserving functionality)
            Text(
                text = "📚 Blueprint template library",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color(0xFF475569),
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                item {
                    TemplateChip(
                        title = "Study Notes",
                        icon = "📖",
                        color = Color(0xFFE8F5E9),
                        onClick = {
                            viewModel.createBoard("📚 Study Blueprint Notes", templateType = "STUDY")
                        }
                    )
                }
                item {
                    TemplateChip(
                        title = "Brainstorming Mindmap",
                        icon = "🧠",
                        color = Color(0xFFE1F5FE),
                        onClick = {
                            viewModel.createBoard("🧠 Brainstorming Blueprint Mindmap", templateType = "MINDMAP")
                        }
                    )
                }
                item {
                    TemplateChip(
                        title = "Agile Planner Kanban",
                        icon = "📋",
                        color = Color(0xFFFFF3E0),
                        onClick = {
                            viewModel.createBoard("📋 Project Planning Kanban", templateType = "PLANNING")
                        }
                    )
                }
                item {
                    TemplateChip(
                        title = "Lecture Blueprint",
                        icon = "🌌",
                        color = Color(0xFFF3E5F5),
                        onClick = {
                            viewModel.createBoard("🌌 Lecture Blueprint: Astronomy", templateType = "LECTURE")
                        }
                    )
                }
            }

            // 7. Dynamic Board list title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💼 Whiteboards Archive (${filteredBoards.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0F172A)
                )
            }

            // 8. Chunked grid drawing of whiteboard items to scroll beautifully inside main container
            if (filteredBoards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOff,
                            contentDescription = "Empty whiteboard browser",
                            tint = Color.Gray.copy(alpha = 0.4f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No offline boards found",
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Launch blank canvas or pre-made templates above!",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                // Chunk boards into pairs of 2 to display asymmetric bento row layout for My Whiteboards!
                filteredBoards.chunked(2).forEach { rowPair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowPair.forEach { board ->
                            Box(modifier = Modifier.weight(1f)) {
                                BoardGridItem(
                                    board = board,
                                    onClick = { onBoardSelected(board) },
                                    onRename = {
                                        renameValue = board.name
                                        showRenameDialog = board
                                    },
                                    onDelete = {
                                        viewModel.deleteBoard(board.id)
                                    }
                                )
                            }
                        }
                        // If odd number, render empty space holder
                        if (rowPair.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(80.dp))
        }

        // --- Dialog 1: Create Board Dialog ---
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Launch Brand New Board") },
                text = {
                    Column {
                        Text("Enter a workspace title to initialize:", fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = createBoardName,
                            onValueChange = { createBoardName = it },
                            placeholder = { Text("e.g., Creative Wireframe 1") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("new_board_name_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        modifier = Modifier.testTag("confirm_create_board"),
                        onClick = {
                            viewModel.createBoard(createBoardName)
                            showCreateDialog = false
                        }
                    ) {
                        Text("Launch")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // --- Dialog 2: Rename Board Dialog ---
        showRenameDialog?.let { board ->
            AlertDialog(
                onDismissRequest = { showRenameDialog = null },
                title = { Text("Rename Board") },
                text = {
                    OutlinedTextField(
                        value = renameValue,
                        onValueChange = { renameValue = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.renameBoard(board, renameValue)
                            showRenameDialog = null
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // --- Dialog 3: Import Shared Board JSON ---
        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = { Text("Import Shared Boardly Format") },
                text = {
                    Column {
                        Text("Paste Boardly JSON text string received from another device:", fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = importJsonValue,
                            onValueChange = { importJsonValue = it },
                            placeholder = { Text("Paste valid metadata JSON string here...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (importJsonValue.isNotBlank()) {
                                viewModel.importBoardJsonString(importJsonValue)
                            }
                            showImportDialog = false
                        }
                    ) {
                        Text("Verify & Build")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // --- Dialog 4: Creator info / About Company ---
        if (showDeveloperDialog) {
            Dialog(onDismissRequest = { showDeveloperDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.VerifiedUser,
                            contentDescription = "Success tick",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Boardly Whiteboard",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Version 1.0.0 (Offline Suite)",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        
                        Text(
                            text = "👨💻 About the Developer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Text(
                            text = "Prince AR Abdur Rahman",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.Start).padding(top = 2.dp)
                        )
                        Text(
                            text = "An independent Android architect passionate about creating responsive, beautiful, offline-first digital productivity suites, learning models, and developer sandboxes.",
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.Start).padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "🏢 About NexVora Lab's Ofc",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Text(
                            text = "Mission: Build high performance, private, beautiful applications accessible to individuals and classrooms everywhere.",
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.Start).padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "📞 Contact / Supports",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Text(
                            text = "• WhatsApp: 01707424006 | 01796951709\n• Facebook: https://www.facebook.com/share/1BNn32qoJo/\n• Instagram: @ur___abdur____rahman__2008",
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.align(Alignment.Start).padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showDeveloperDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentBoardBentoCard(
    recentBoard: BoardEntity?,
    onClick: (BoardEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = {
            recentBoard?.let { onClick(it) }
        },
        modifier = modifier.height(200.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 8.dp)
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
                    .border(
                        width = 1.dp,
                        color = Color(0xFFE2E8F0),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (recentBoard != null) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawLine(
                            color = Color(0xFF3B82F6),
                            start = Offset(size.width * 0.25f, size.height * 0.35f),
                            end = Offset(size.width * 0.75f, size.height * 0.35f),
                            strokeWidth = 6f,
                            cap = StrokeCap.Round
                        )
                        drawCircle(
                            color = Color(0xFFFB923C),
                            radius = size.minDimension * 0.16f,
                            center = Offset(size.width * 0.65f, size.height * 0.65f),
                            style = Stroke(width = 4f)
                        )
                        drawLine(
                            color = Color(0xFF22C55E),
                            start = Offset(size.width * 0.25f, size.height * 0.75f),
                            end = Offset(size.width * 0.45f, size.height * 0.55f),
                            strokeWidth = 10f,
                            cap = StrokeCap.Round
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.SpaceDashboard,
                        contentDescription = "Empty board mockup",
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Column {
                if (recentBoard != null) {
                    val timeDelta = System.currentTimeMillis() - recentBoard.updatedAt
                    val relativeTime = when {
                        timeDelta < 60_000 -> "Edited just now"
                        timeDelta < 3600_000 -> "Edited ${timeDelta / 60_000}m ago"
                        timeDelta < 86400000 -> "Edited ${timeDelta / 3600_000}h ago"
                        else -> {
                            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
                            "Edited ${sdf.format(Date(recentBoard.updatedAt))}"
                        }
                    }
                    Text(
                        text = relativeTime,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = recentBoard.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                } else {
                    Text(
                        text = "No recent work",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "No active boards",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MindMapTemplateBentoCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEDD5)), // orange-100
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFED7AA)) // orange-200
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MIND MAP",
                    color = Color(0xFFC2410C), // orange-700
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Launch Brainstorm",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF7C2D12), // orange-900
                    lineHeight = 16.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("🧠", fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun DeviceStorageBentoCard(
    boardCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(98.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C1E)), // Dark gray/coal
        border = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Color(0xFF4ADE80), CircleShape) // Green dot
                )
                Text(
                    text = "DEVICE STORAGE",
                    color = Color(0xFF94A3B8), // slate-400
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            Column {
                Text(
                    text = "${boardCount * 1.4 + 11.2} MB", // dynamic simulated size footprint
                    fontWeight = FontWeight.W300,
                    fontSize = 18.sp,
                    color = Color.White
                )
                Text(
                    text = "Fully local, no sync",
                    fontSize = 9.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
fun ImportFileBentoCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(76.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Input,
                    contentDescription = "Import share file",
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = "Import Share File",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color(0xFF334155) // slate-700
            )
        }
    }
}

@Composable
fun AuthorBentoCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(76.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF5FF)), // purple-50
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF3E8FF)) // purple-100
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0xFFE2D6F3), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("👑", fontSize = 14.sp)
            }
            Column {
                Text(
                    text = "v1.0.0 Info Hub",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFF7E22CE) // purple-700
                )
                Text(
                    text = "Prince AR",
                    fontSize = 11.sp,
                    color = Color(0xFF9333EA) // purple-600
                )
            }
        }
    }
}

@Composable
fun TemplateChip(
    title: String,
    icon: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .clickable { onClick() }
            .width(180.dp)
            .height(72.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color(0xFF2E3B2F),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun BoardGridItem(
    board: BoardEntity,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }
    
    val format = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
    val formattedDate = format.format(Date(board.updatedAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = board.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 2,
                        color = Color(0xFF0F172A),
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Pattern: ${board.backgroundType}",
                        fontSize = 11.sp,
                        color = Color(0xFF005CBB),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                Box {
                    IconButton(
                        onClick = { expandedMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Actions menu",
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF64748B)
                        )
                    }
                    
                    DropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename Title", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                expandedMenu = false
                                onRename()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Board", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Red) },
                            onClick = {
                                expandedMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
                if (board.isLocked) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Board is locked from edits",
                        tint = Color.Red.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
