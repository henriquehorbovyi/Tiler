package com.henriquehorbovyi.tiler

import java.awt.Color

object Preferences {
    // Window
    const val WINDOW_WIDTH = 1200
    const val WINDOW_HEIGHT = 800
    
    // Sidebar
    const val SIDEBAR_WIDTH = 200
    
    // Grid
    const val DEFAULT_GRID_ROWS = 16
    const val DEFAULT_GRID_COLS = 16
    const val CELL_SIZE = 16
    
    // Zoom
    const val ZOOM_MIN_SCALE = 0.1f
    const val ZOOM_MAX_SCALE = 5.0f
    const val ZOOM_IN_FACTOR = 1.1f
    const val ZOOM_OUT_FACTOR = 0.9f
    const val WHEEL_ZOOM_IN_FACTOR = 1.03f    // Slower zoom for mouse wheel
    const val WHEEL_ZOOM_OUT_FACTOR = 0.97f   // Slower zoom for mouse wheel

    // UI
    const val SCROLL_INCREMENT = 16
    const val TILE_PREVIEW_SIZE = 64
    const val TILE_CONTAINER_SIZE = 74
    
    // Scroll
    const val SCROLL_AMOUNT = 32
    
    // Colors
    val BACKGROUND_COLOR = Color(18, 18, 18)  // Dark background
    val GRID_COLOR = Color(70, 70, 70)        // Darker gray for grid
    val PANEL_COLOR = Color(30, 30, 30)       // Slightly lighter than background for panels
    val BUTTON_BACKGROUND = Color(45, 45, 45)  // Button background
    val BUTTON_TEXT = Color(200, 200, 200)    // Light gray for text
    val SELECTION_COLOR = Color(70, 70, 100)  // Added selection highlight color
}
