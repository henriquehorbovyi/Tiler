package com.henriquehorbovyi.tiler

import com.formdev.flatlaf.FlatDarkLaf
import com.henriquehorbovyi.tiler.keyboard.KeyboardEvent
import com.henriquehorbovyi.tiler.keyboard.KeyboardManager
import com.henriquehorbovyi.tiler.menu.Menu
import com.henriquehorbovyi.tiler.menu.MenuCallbacks
import org.json.JSONObject
import org.json.JSONTokener
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.filechooser.FileFilter
import javax.swing.filechooser.FileNameExtensionFilter

class MainWindow : JFrame(), MenuCallbacks {
    private var splitPane: JSplitPane? = null
    private var sidebarPanel: JPanel? = null
    private var tilesPanel: JPanel? = null
    private var mapPanel: MapPanel? = null
    private var loadTilesButton: JButton? = null
    private val tileLoader: TileLoader = TileLoader()
    private var selectedTile: Tile? = null
    private var lastOpenedDirectory: File? = null
    private val keyboardManager = KeyboardManager()
    private var isTilesPanelVisible = true
    private var lastDividerLocation = Preferences.SIDEBAR_WIDTH
    private val selectedTiles = mutableSetOf<Tile>()
    private var changeSolidStateButton: JButton? = null
    private var lastTilesFolder: File? = null
    private var lastMapMatrixFile: File? = null

    init {
        setupTheme()
        setTitle("Tile Map Editor")
        setDefaultCloseOperation(EXIT_ON_CLOSE)
        setSize(Preferences.WINDOW_WIDTH, Preferences.WINDOW_HEIGHT)

        // Add menu bar
        jMenuBar = Menu(this)

        // Initialize components
        initComponents()

        // Layout setup
        setupLayout()

        // Setup keyboard events
        setupKeyboardEvents()

        contentPane.background = Preferences.BACKGROUND_COLOR

        setLocationRelativeTo(null)
    }

    private fun setupTheme() {
        try {
            // Set dark theme for the entire application
            FlatDarkLaf.setup()

            // Configure specific colors
            UIManager.put("TitleBar.background", Preferences.BACKGROUND_COLOR)
            UIManager.put("TitleBar.foreground", Preferences.BUTTON_TEXT)
            UIManager.put("Panel.background", Preferences.BACKGROUND_COLOR)
            UIManager.put("ScrollBar.thumb", Preferences.BUTTON_BACKGROUND)
            UIManager.put("ScrollBar.track", Preferences.PANEL_COLOR)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initComponents() {
        sidebarPanel = JPanel().apply {
            preferredSize = Dimension(Preferences.SIDEBAR_WIDTH, getHeight())
            background = Preferences.PANEL_COLOR
        }

        loadTilesButton = JButton("Load Tiles").apply {
            addActionListener { loadTiles() }
            background = Preferences.BUTTON_BACKGROUND
            foreground = Preferences.BUTTON_TEXT
        }

        tilesPanel = JPanel().apply {
            layout = GridLayout(0, 2, 5, 5)
            background = Preferences.PANEL_COLOR
        }

        mapPanel = MapPanel(selectedTile).apply {
            background = Preferences.BACKGROUND_COLOR
            addMouseListener(object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) {
                    selectedTile?.let { tile ->
                        placeTile(e.x, e.y, tile, addToHistory = true)
                    }
                }
            })

            addMouseMotionListener(object : MouseAdapter() {
                override fun mouseMoved(e: MouseEvent) {
                    previewX = e.x
                    previewY = e.y

                    // Check if shift is down using the event's modifiers
                    if ((e.modifiersEx and KeyEvent.SHIFT_DOWN_MASK) != 0 && selectedTile != null) {
                        placeTile(e.x, e.y, selectedTile!!, addToHistory = true)
                    }

                    repaint()
                }

                override fun mouseExited(e: MouseEvent) {
                    previewX = -1
                    previewY = -1
                    repaint()
                }
            })

            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        }

        changeSolidStateButton = JButton("Change solid state").apply {
            addActionListener { toggleSelectedTilesSolidState() }
            background = Preferences.BUTTON_BACKGROUND
            foreground = Preferences.BUTTON_TEXT
            isVisible = false // Hidden by default
        }
    }

    private fun setupLayout() {
        // Create split pane
        splitPane = JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            createSidebarPanel(),
            createMainPanel()
        )
        splitPane?.setDividerLocation(200)

        add(splitPane)
    }

    private fun createSidebarPanel(): JPanel {
        return sidebarPanel?.apply {
            layout = BorderLayout()
            add(JScrollPane(tilesPanel), BorderLayout.CENTER)

            // Create a panel for buttons
            val buttonsPanel = JPanel().apply {
                layout = BorderLayout()
                background = Preferences.PANEL_COLOR
                changeSolidStateButton?.let {
                    add(it, BorderLayout.NORTH)
                }

                loadTilesButton?.let {
                    add(it, BorderLayout.SOUTH)
                }
            }

            add(buttonsPanel, BorderLayout.SOUTH)
        } ?: throw IllegalStateException("Sidebar panel not initialized")
    }

    private fun createMainPanel(): JPanel {
        return JPanel(BorderLayout()).apply {
            add(JScrollPane(mapPanel).apply {
                horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
                horizontalScrollBar.unitIncrement = Preferences.SCROLL_INCREMENT
                verticalScrollBar.unitIncrement = Preferences.SCROLL_INCREMENT
            }, BorderLayout.CENTER)
        }
    }

    private fun loadTiles() {
        val fileChooser = JFileChooser(lastOpenedDirectory).apply {
            isMultiSelectionEnabled = true
            fileFilter = object : FileFilter() {
                override fun accept(f: File) =
                    f.isDirectory || f.name.lowercase().endsWith(".png")

                override fun getDescription() = "PNG Images"
            }
        }

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            val files = fileChooser.selectedFiles
            // Save the last opened directory
            lastOpenedDirectory = fileChooser.currentDirectory
            tileLoader.loadTiles(files)
            displayLoadedTiles()
        }
    }

    private fun displayLoadedTiles() {
        tilesPanel?.apply {
            removeAll()
            tileLoader.loadedTiles.forEach { tile ->
                tile.let {
                    add(JPanel().apply {
                        layout = BorderLayout()
                        preferredSize = Dimension(Preferences.TILE_CONTAINER_SIZE, Preferences.TILE_CONTAINER_SIZE + 25)
                        maximumSize = preferredSize
                        minimumSize = preferredSize
                        background = if (selectedTiles.contains(tile)) {
                            Preferences.SELECTION_COLOR
                        } else {
                            Preferences.PANEL_COLOR
                        }

                        // Tile preview button
                        add(
                            JButton(
                                it.getScaledIcon(
                                    Preferences.TILE_PREVIEW_SIZE,
                                    Preferences.TILE_PREVIEW_SIZE
                                )
                            ).apply {
                                toolTipText = it.name
                                addActionListener { e ->
                                    if ((e.modifiers and InputEvent.SHIFT_MASK) != 0) {
                                        selectedTiles.add(tile)
                                        updateSelectedTilesButtonVisibility()
                                        displayLoadedTiles()
                                    } else {
                                        selectedTiles.clear()
                                        selectTile(tile)
                                        updateSelectedTilesButtonVisibility()
                                        displayLoadedTiles()
                                    }
                                }
                                preferredSize = Dimension(Preferences.TILE_PREVIEW_SIZE, Preferences.TILE_PREVIEW_SIZE)
                                maximumSize = preferredSize
                                minimumSize = preferredSize
                                background = Preferences.BUTTON_BACKGROUND
                            }, BorderLayout.CENTER
                        )

                        // Change checkbox label and logic
                        add(JCheckBox("Solid").apply {
                            isSelected = tile.isSolid
                            foreground = Preferences.BUTTON_TEXT
                            background = if (selectedTiles.contains(tile)) {
                                Preferences.SELECTION_COLOR
                            } else {
                                Preferences.PANEL_COLOR
                            }
                            addActionListener {
                                if (selectedTiles.isNotEmpty() && selectedTiles.contains(tile)) {
                                    // Apply to all selected tiles
                                    selectedTiles.forEach { it.isSolid = isSelected }
                                } else {
                                    // Apply to single tile
                                    tile.isSolid = isSelected
                                }
                            }
                        }, BorderLayout.SOUTH)
                    })
                }
            }
            revalidate()
            repaint()
        }
    }

    private fun selectTile(tile: Tile) {
        selectedTile = tile
        mapPanel?.selectedTile = tile
        println("Selected tile: ${tile.name}")
    }

    private fun showGridDialog() {
        val panel = JPanel(GridLayout(2, 2, 5, 5)).apply {
            add(JLabel("Columns:"))
            val colsField = JTextField(mapPanel?.getColumns().toString())
            add(colsField)
            add(JLabel("Rows:"))
            val rowsField = JTextField(mapPanel?.getRows().toString())
            add(rowsField)


            border = EmptyBorder(10, 10, 10, 10)
        }

        val result = JOptionPane.showConfirmDialog(
            this,
            panel,
            "Grid Configuration",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        )

        if (result == JOptionPane.OK_OPTION) {
            try {
                val cols = panel.getComponent(1).let { it as JTextField }.text.toInt()
                val rows = panel.getComponent(3).let { it as JTextField }.text.toInt()
                updateGrid(rows, cols)
            } catch (e: NumberFormatException) {
                JOptionPane.showMessageDialog(
                    this,
                    "Please enter valid numbers",
                    "Invalid Input",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }
    }

    private fun updateGrid(rows: Int, cols: Int) {
        mapPanel?.updateGridSize(rows, cols)
    }

    private fun toggleSelectedTilesSolidState() {
        if (selectedTiles.isNotEmpty()) {
            // Toggle state based on first selected tile
            val newState = !selectedTiles.first().isSolid
            selectedTiles.forEach { it.isSolid = newState }
            displayLoadedTiles() // Refresh display
        }
    }

    private fun updateSelectedTilesButtonVisibility() {
        changeSolidStateButton?.isVisible = selectedTiles.size > 1
    }

    private fun selectAllTiles() {
        selectedTiles.clear()
        selectedTiles.addAll(tileLoader.loadedTiles)
        updateSelectedTilesButtonVisibility()
        displayLoadedTiles()
    }

    private fun setupKeyboardEvents() {
        keyboardManager.addEventListener(KeyboardEvent.Undo) {
            mapPanel?.undo()
        }

        keyboardManager.addEventListener(KeyboardEvent.ZoomIn) {
            mapPanel?.zoom(Preferences.ZOOM_IN_FACTOR)
        }

        keyboardManager.addEventListener(KeyboardEvent.ZoomOut) {
            mapPanel?.zoom(Preferences.ZOOM_OUT_FACTOR)
        }

        keyboardManager.addEventListener(KeyboardEvent.ScrollLeft) {
            mapPanel?.scroll(Preferences.SCROLL_AMOUNT, 0)
        }

        keyboardManager.addEventListener(KeyboardEvent.ScrollRight) {
            mapPanel?.scroll(-Preferences.SCROLL_AMOUNT, 0)
        }

        keyboardManager.addEventListener(KeyboardEvent.ScrollUp) {
            mapPanel?.scroll(0, Preferences.SCROLL_AMOUNT)
        }

        keyboardManager.addEventListener(KeyboardEvent.ScrollDown) {
            mapPanel?.scroll(0, -Preferences.SCROLL_AMOUNT)
        }

        keyboardManager.addEventListener(KeyboardEvent.Grid) {
            showGridDialog()
        }

        keyboardManager.addEventListener(KeyboardEvent.ToggleLoadedTilesTab) {
            toggleLoadedTilesTab()
        }

        keyboardManager.addEventListener(KeyboardEvent.ResetZoom) {
            mapPanel?.resetZoom()
        }

        keyboardManager.addEventListener(KeyboardEvent.SelectAll) {
            selectAllTiles()
        }

        keyboardManager.addEventListener(KeyboardEvent.ClearSelection) {
            selectedTiles.clear()
            updateSelectedTilesButtonVisibility()
            tilesPanel?.revalidate()
            tilesPanel?.repaint()
        }

        keyboardManager.addEventListener(KeyboardEvent.IncreaseTileArea) {
            mapPanel?.increaseTileArea()
        }

        keyboardManager.addEventListener(KeyboardEvent.DecreaseTileArea) {
            mapPanel?.decreaseTileArea()
        }
    }

    private fun toggleLoadedTilesTab() {
        isTilesPanelVisible = !isTilesPanelVisible
        if (isTilesPanelVisible) {
            // Store the current divider location before showing
            splitPane?.leftComponent?.isVisible = true
            splitPane?.dividerLocation = lastDividerLocation
        } else {
            // Store the divider location before hiding
            lastDividerLocation = splitPane?.dividerLocation ?: Preferences.SIDEBAR_WIDTH
            splitPane?.leftComponent?.isVisible = false
        }
        splitPane?.revalidate()
        splitPane?.repaint()
    }

    // Implement MenuCallbacks
    override fun onNew() {
        // Clear the grid and reset to default size
        mapPanel?.updateGridSize(Preferences.DEFAULT_GRID_ROWS, Preferences.DEFAULT_GRID_COLS)
    }

    override fun onOpen() {
        loadTiles()
    }

    override fun onImport() {
        val fileChooser = JFileChooser(lastOpenedDirectory).apply {
            dialogTitle = "Import Map Matrix"
            dialogType = JFileChooser.OPEN_DIALOG
            fileSelectionMode = JFileChooser.FILES_ONLY
            isMultiSelectionEnabled = false
            fileFilter = object : javax.swing.filechooser.FileFilter() {
                override fun accept(f: File) =
                    f.isDirectory || f.name.lowercase().endsWith(".txt")

                override fun getDescription() = "Text Files (*.txt)"
            }
        }

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            val file = fileChooser.selectedFile
            lastOpenedDirectory = file.parentFile
            importMapMatrix(file)
        }
    }

    private fun importMapMatrix(file: File) {
        try {
            val lines = file.readLines()
            val rows = lines.size
            val regex = Regex("""\[[^\[\]]+\]""")
            val firstLineMatches =
                if (lines.isNotEmpty()) regex.findAll(lines.first()).map { it.value }.toList() else emptyList()
            val cols = firstLineMatches.size
            if (rows == 0 || cols == 0) {
                JOptionPane.showMessageDialog(
                    this,
                    "Invalid map file.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                )
                return
            }

            // Update grid size
            mapPanel?.updateGridSize(rows, cols)

            // Clear placed tiles
            mapPanel?.clearPlaced()
            mapPanel?.repaint()

            // Parse and place tiles
            lines.forEachIndexed { rowIdx, line ->
                val matches = regex.findAll(line).map { it.value }.toList()
                matches.forEachIndexed { colIdx, cell ->
                    if (cell.isNotEmpty() && cell != ".") {
                        // Example: [f:tile.png, s:true]
                        val nameRegex = """f:([^,\]]+)""".toRegex()
                        val solidRegex = """s:(true|false)""".toRegex()
                        val name = nameRegex.find(cell)?.groupValues?.get(1)
                        val isSolid = solidRegex.find(cell)?.groupValues?.get(1)?.toBoolean() ?: false

                        val tile = tileLoader.loadedTiles.find { it.name == name }
                        if (tile != null) {
                            tile.isSolid = isSolid
                            val x = (colIdx * Preferences.CELL_SIZE) + Preferences.CELL_SIZE / 2
                            val y = (rowIdx * Preferences.CELL_SIZE) + Preferences.CELL_SIZE / 2
                            mapPanel?.addPlaced(PlacedTile(x, y, tile))
                        }
                    }
                }
            }
            mapPanel?.repaint()
            JOptionPane.showMessageDialog(
                this,
                "Map imported successfully!",
                "Success",
                JOptionPane.INFORMATION_MESSAGE
            )
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(
                this,
                "Error importing map: ${e.message}",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    override fun onExport() {
        val fileChooser = JFileChooser(lastOpenedDirectory).apply {
            dialogTitle = "Export Project"
            dialogType = JFileChooser.SAVE_DIALOG
            fileSelectionMode = JFileChooser.FILES_ONLY
            isMultiSelectionEnabled = false
            fileFilter = FileNameExtensionFilter("Tiling Project (*.tiling.json)", "tiling.json")
        }

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            var file = fileChooser.selectedFile
            if (!file.name.endsWith(".tiling.json")) {
                file = File(file.absolutePath + ".tiling.json")
            }
            lastOpenedDirectory = file.parentFile

            // Ask for tiles folder and map matrix file if not set
            if (lastTilesFolder == null) {
                val tilesChooser = JFileChooser().apply {
                    dialogTitle = "Select Tiles Folder"
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                }
                if (tilesChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    lastTilesFolder = tilesChooser.selectedFile
                }
            }
            if (lastMapMatrixFile == null) {
                val mapChooser = JFileChooser().apply {
                    dialogTitle = "Select Map Matrix File"
                    fileSelectionMode = JFileChooser.FILES_ONLY
                    fileFilter = FileNameExtensionFilter("Text Files (*.txt)", "txt")
                }
                if (mapChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    lastMapMatrixFile = mapChooser.selectedFile
                }
            }

            // Save project file
            val projectJson = JSONObject().apply {
                put("tiles_folder", lastTilesFolder?.absolutePath ?: "")
                put("map_matrix", lastMapMatrixFile?.absolutePath ?: "")
                put("grid_rows", mapPanel?.getRows() ?: Preferences.DEFAULT_GRID_ROWS)
                put("grid_cols", mapPanel?.getColumns() ?: Preferences.DEFAULT_GRID_COLS)
                put("cell_size", Preferences.CELL_SIZE)
            }
            file.writeText(projectJson.toString(2))

            JOptionPane.showMessageDialog(
                this,
                "Project exported successfully!",
                "Success",
                JOptionPane.INFORMATION_MESSAGE
            )
        }
    }

    override fun onImportProject() {
        val fileChooser = JFileChooser(lastOpenedDirectory).apply {
            dialogTitle = "Import Project"
            dialogType = JFileChooser.OPEN_DIALOG
            fileSelectionMode = JFileChooser.FILES_ONLY
            fileFilter = FileNameExtensionFilter("Tiling Project (*.tiling.json)", "tiling.json")
        }
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            val file = fileChooser.selectedFile
            lastOpenedDirectory = file.parentFile
            try {
                val json = JSONObject(JSONTokener(file.inputStream()))
                val tilesFolder = File(json.getString("tiles_folder"))
                val mapMatrixFile = File(json.getString("map_matrix"))
                val gridRows = json.optInt("grid_rows", Preferences.DEFAULT_GRID_ROWS)
                val gridCols = json.optInt("grid_cols", Preferences.DEFAULT_GRID_COLS)
                val cellSize = json.optInt("cell_size", Preferences.CELL_SIZE)

                // Load tiles
                if (tilesFolder.exists() && tilesFolder.isDirectory) {
                    val tileFiles = tilesFolder.listFiles { f -> f.name.endsWith(".png") } ?: emptyArray()
                    tileLoader.loadTiles(tileFiles)
                    displayLoadedTiles()
                    lastTilesFolder = tilesFolder
                }
                // Load map matrix
                if (mapMatrixFile.exists()) {
                    importMapMatrix(mapMatrixFile)
                    lastMapMatrixFile = mapMatrixFile
                }
                // Set grid/cell size
                mapPanel?.updateGridSize(gridRows, gridCols)
                mapPanel?.updateCellSize(cellSize)

                JOptionPane.showMessageDialog(
                    this,
                    "Project imported successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
                )
            } catch (e: Exception) {
                JOptionPane.showMessageDialog(
                    this,
                    "Error importing project: ${e.message}",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }
    }

    override fun onGenerateMapMatrix() {
        val fileChooser = JFileChooser(lastOpenedDirectory).apply {
            dialogTitle = "Save Map Matrix"
            dialogType = JFileChooser.SAVE_DIALOG
            fileSelectionMode = JFileChooser.FILES_ONLY
            isMultiSelectionEnabled = false
            fileFilter = object : FileFilter() {
                override fun accept(f: File) =
                    f.isDirectory || f.name.lowercase().endsWith(".txt")

                override fun getDescription() = "Text Files (*.txt)"
            }
        }

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            var file = fileChooser.selectedFile
            // Add .txt extension if not present
            if (!file.name.lowercase().endsWith(".txt")) {
                file = File(file.absolutePath + ".txt")
            }
            lastOpenedDirectory = file.parentFile
            generateMapMatrix(file)
        }
    }

    private fun generateMapMatrix(file: File) {
        try {
            file.bufferedWriter().use { writer ->
                val matrix = Array(mapPanel?.getRows() ?: 0) { Array(mapPanel?.getColumns() ?: 0) { "" } }

                // Fill the matrix with placed tiles
                mapPanel?.placedTiles?.forEach { placed ->
                    val col = (placed.x - Preferences.CELL_SIZE / 2) / Preferences.CELL_SIZE
                    val row = (placed.y - Preferences.CELL_SIZE / 2) / Preferences.CELL_SIZE
                    if (row in 0 until (mapPanel?.getRows() ?: 0) &&
                        col in 0 until (mapPanel?.getColumns() ?: 0)
                    ) {
                        // f = file name
                        // s = solid
                        matrix[row][col] = "[f:${placed.tile.name}, s:${placed.tile.isSolid}]"
                    }
                }

                // Write matrix to file
                matrix.forEach { row ->
                    writer.write(row.joinToString(", "))
                    writer.newLine()
                }
            }

            JOptionPane.showMessageDialog(
                this,
                "Map matrix saved successfully!",
                "Success",
                JOptionPane.INFORMATION_MESSAGE
            )
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(
                this,
                "Error saving map matrix: ${e.message}",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    private fun showCellSizeDialog() {
        val panel = JPanel(GridLayout(1, 2, 5, 5)).apply {
            add(JLabel("Cell size:"))
            val sizeField = JTextField(Preferences.CELL_SIZE.toString())
            add(sizeField)
            border = EmptyBorder(10, 10, 10, 10)
        }

        val result = JOptionPane.showConfirmDialog(
            this,
            panel,
            "Cell Size Configuration",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        )

        if (result == JOptionPane.OK_OPTION) {
            try {
                val size = panel.getComponent(1).let { it as JTextField }.text.toInt()
                if (size > 0) {
                    mapPanel?.updateCellSize(size)
                } else {
                    throw NumberFormatException("Cell size must be greater than 0")
                }
            } catch (e: NumberFormatException) {
                JOptionPane.showMessageDialog(
                    this,
                    "Please enter a valid positive number",
                    "Invalid Input",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }
    }

    // MenuCallbacks implementations
    override fun onChangeGrid() {
        showGridDialog()
    }

    override fun onZoomIn() {
        mapPanel?.zoom(Preferences.ZOOM_IN_FACTOR)
    }

    override fun onZoomOut() {
        mapPanel?.zoom(Preferences.ZOOM_OUT_FACTOR)
    }

    override fun onResetZoom() {
        mapPanel?.resetZoom()
    }

    override fun onChangeCell() {
        showCellSizeDialog()
    }
}
