package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.canvas.BoardEditorScreen
import com.example.ui.dashboard.BoardListScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    
    private val viewModel: BoardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                var selectedBoard by remember { mutableStateOf<com.example.data.BoardEntity?>(null) }
                
                // Safely intercepts system gestures while sketching to make sure elements are auto-saved
                BackHandler(enabled = selectedBoard != null) {
                    viewModel.saveActiveBoardState()
                    selectedBoard = null
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        AnimatedContent(
                            targetState = selectedBoard,
                            transitionSpec = {
                                slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                            },
                            label = "screen_routing"
                        ) { board ->
                            if (board == null) {
                                BoardListScreen(
                                    viewModel = viewModel,
                                    onBoardSelected = { selected ->
                                        viewModel.loadBoard(selected)
                                        selectedBoard = selected
                                    }
                                )
                            } else {
                                BoardEditorScreen(
                                    viewModel = viewModel,
                                    onBack = {
                                        viewModel.saveActiveBoardState()
                                        selectedBoard = null
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Force state persistence during backgrounding/minimizing
        viewModel.saveActiveBoardState()
    }
}
