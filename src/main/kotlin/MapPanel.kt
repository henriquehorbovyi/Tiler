package com.henriquehorbovyi.tiler

import java.awt.AlphaComposite
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import javax.swing.JPanel

data class PlacedTile(
    val x: Int,
    val y: Int,
    val tile: Tile,
)

class MapPanel(var selectedTile: Tile?) : JPanel(), MouseWheelListener {
    // Make placedTiles accessible
    val placedTiles: List<PlacedTile> get() = _placedTiles.toList()
    private val _placedTiles = mutableListOf<PlacedTile>()
    private val tileHistory = mutableListOf<PlacedTile>()
    private var gridRows = Preferences.DEFAULT_GRID_ROWS
    private var gridCols = Preferences.DEFAULT_GRID_COLS
    private var cellSize = Preferences.CELL_SIZE
    private var scale = 1.0f
    private val minScale = Preferences.ZOOM_MIN_SCALE
    private val maxScale = Preferences.ZOOM_MAX_SCALE
    var previewX = -1
    var previewY = -1
    private var scrollX = 0
    private var scrollY = 0

    private var tileAreaSize = 1
    private val minTileArea = 1
    private val maxTileArea = 32

    init {
        updatePreferredSize()
        addMouseWheelListener(this)
    }

    fun zoom(factor: Float) {
        scale = (scale * factor).coerceIn(minScale, maxScale)
        updatePreferredSize()
        revalidate()
        repaint()
    }

    fun placeTile(x: Int, y: Int, tile: Tile, addToHistory: Boolean = true) {
        val gridWidth = gridCols * cellSize * scale
        val gridHeight = gridRows * cellSize * scale
        val offsetX = (width - gridWidth) / 2 + scrollX
        val offsetY = (height - gridHeight) / 2 + scrollY

        val adjustedX = ((x - offsetX) / scale).toInt()
        val adjustedY = ((y - offsetY) / scale).toInt()

        val gridX = (adjustedX / cellSize) * cellSize
        val gridY = (adjustedY / cellSize) * cellSize

        val gridCol = gridX / cellSize
        val gridRow = gridY / cellSize

        if (gridCol in 0 until gridCols && gridRow in 0 until gridRows) {
            for (dy in 0 until tileAreaSize) {
                for (dx in 0 until tileAreaSize) {
                    val col = gridCol + dx
                    val row = gridRow + dy
                    if (col in 0 until gridCols && row in 0 until gridRows) {
                        val centeredX = (col * cellSize) + cellSize / 2
                        val centeredY = (row * cellSize) + cellSize / 2

                        val existingTile = _placedTiles.find { it.x == centeredX && it.y == centeredY }
                        if (existingTile?.tile == tile) continue

                        if (addToHistory) {
                            existingTile?.let {
                                tileHistory.add(it)
                            }
                        }

                        _placedTiles.removeIf { it.x == centeredX && it.y == centeredY }
                        val newTile = PlacedTile(centeredX, centeredY, tile)
                        _placedTiles.add(newTile)

                        if (addToHistory) {
                            tileHistory.add(newTile)
                        }
                    }
                }
            }
            repaint()
            logPlacedTilesMatrix()
        }
    }

    fun undo() {
        if (tileHistory.isNotEmpty()) {
            val lastTile = tileHistory.removeAt(tileHistory.lastIndex)
            _placedTiles.removeIf { it.x == lastTile.x && it.y == lastTile.y }

            tileHistory.findLast { it.x == lastTile.x && it.y == lastTile.y }?.let {
                _placedTiles.add(it)
            }

            repaint()
        }
    }

    fun updateGridSize(rows: Int, cols: Int) {
        gridRows = rows
        gridCols = cols
        _placedTiles.clear()
        tileHistory.clear()
        updatePreferredSize()
        revalidate()
        repaint()
    }

    fun updateCellSize(size: Int) {
        cellSize = size
        updatePreferredSize()
        revalidate()
        repaint()
    }

    private fun updatePreferredSize() {
        preferredSize = Dimension(
            (gridCols * cellSize * scale).toInt(),
            (gridRows * cellSize * scale).toInt()
        )
    }

    fun scroll(dx: Int, dy: Int) {
        scrollX += dx
        scrollY += dy
        repaint()
    }

    private fun drawTile(g2d: Graphics2D, tile: Tile, x: Int, y: Int, isPreview: Boolean = false) {
        if (isPreview) {
            g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f)
        }

        // Calculate the position to center the tile in the cell
        val tileWidth = tile.image.width
        val tileHeight = tile.image.height
        val xOffset = (cellSize - tileWidth) / 2
        val yOffset = (cellSize - tileHeight) / 2

        g2d.drawImage(
            tile.image,
            x - cellSize / 2 + xOffset,
            y - cellSize / 2 + yOffset,
            tileWidth,
            tileHeight,
            null
        )
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D
        g2d.drawString("${gridCols}x${gridRows}", 10, 20)

        val gridWidth = gridCols * cellSize * scale
        val gridHeight = gridRows * cellSize * scale

        val offsetX = (width - gridWidth) / 2 + scrollX
        val offsetY = (height - gridHeight) / 2 + scrollY

        g2d.translate(offsetX.toInt(), offsetY.toInt())
        g2d.scale(scale.toDouble(), scale.toDouble())

        g2d.color = Preferences.GRID_COLOR

        for (i in 0..gridCols) {
            val x = i * cellSize
            g2d.drawLine(x, 0, x, gridRows * cellSize)
        }

        for (i in 0..gridRows) {
            val y = i * cellSize
            g2d.drawLine(0, y, gridCols * cellSize, y)
        }

        _placedTiles.forEach { placed ->
            drawTile(g2d, placed.tile, placed.x, placed.y)
        }

        // Preview area
        if (selectedTile != null && previewX >= 0 && previewY >= 0) {
            val adjustedX = ((previewX - offsetX) / scale).toInt()
            val adjustedY = ((previewY - offsetY) / scale).toInt()

            val gridX = (adjustedX / cellSize) * cellSize
            val gridY = (adjustedY / cellSize) * cellSize

            val gridCol = gridX / cellSize
            val gridRow = gridY / cellSize

            if (gridCol in 0 until gridCols && gridRow in 0 until gridRows) {
                for (dy in 0 until tileAreaSize) {
                    for (dx in 0 until tileAreaSize) {
                        val col = gridCol + dx
                        val row = gridRow + dy
                        if (col in 0 until gridCols && row in 0 until gridRows) {
                            val centeredX = (col * cellSize) + cellSize / 2
                            val centeredY = (row * cellSize) + cellSize / 2
                            drawTile(g2d, selectedTile!!, centeredX, centeredY, true)
                        }
                    }
                }
            }
        }
    }

    fun getColumns() = gridCols
    fun getRows() = gridRows

    override fun mouseWheelMoved(e: MouseWheelEvent) {
        if (e.wheelRotation == 0) return
        // Invert the wheel rotation for natural scrolling
        val zoomFactor = if (e.wheelRotation > 0) {
            Preferences.WHEEL_ZOOM_IN_FACTOR
        } else {
            Preferences.WHEEL_ZOOM_OUT_FACTOR
        }
        zoom(zoomFactor)
    }

    fun resetZoom() {
        scale = 1.0f
        updatePreferredSize()
        revalidate()
        repaint()
    }

    fun increaseTileArea() {
        if (tileAreaSize < maxTileArea) {
            tileAreaSize++
            repaint()
        }
    }

    fun decreaseTileArea() {
        if (tileAreaSize > minTileArea) {
            tileAreaSize--
            repaint()
        }
    }


    fun addPlaced(placed: PlacedTile) {
        _placedTiles.add(placed)
    }

    fun clearPlaced() {
        _placedTiles.clear()
    }

    private fun logPlacedTilesMatrix() {
        val matrix = Array(gridRows) { Array(gridCols) { "" } }
        _placedTiles.forEach { placed ->
            val col = (placed.x - Preferences.CELL_SIZE / 2) / Preferences.CELL_SIZE
            val row = (placed.y - Preferences.CELL_SIZE / 2) / Preferences.CELL_SIZE
            if (row in 0 until gridRows && col in 0 until gridCols) {
                matrix[row][col] = placed.tile.name
            }
        }
        println("Placed Tiles Matrix:")
        matrix.forEach { row ->
            println(row.joinToString(", ") { it.ifEmpty { "." } })
        }
    }
}
