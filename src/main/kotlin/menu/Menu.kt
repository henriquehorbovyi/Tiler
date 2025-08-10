package com.henriquehorbovyi.tiling.menu

import com.henriquehorbovyi.tiling.Preferences
import java.awt.Color
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.KeyStroke
import java.awt.event.KeyEvent
import java.awt.event.InputEvent

class Menu(private val callbacks: MenuCallbacks) : JMenuBar() {

    init {
        background = Preferences.PANEL_COLOR
        foreground = Color.BLACK
        add(createFileMenu())
        add(createGridMenu())
    }

    private fun createFileMenu(): JMenu {
        return JMenu("File").apply {
            foreground = Preferences.BUTTON_TEXT

            add(JMenuItem("New").apply {
                accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.SHIFT_DOWN_MASK)
                foreground = Preferences.BUTTON_TEXT
                addActionListener { callbacks.onNew() }
            })

            add(JMenuItem("Open...").apply {
                accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.SHIFT_DOWN_MASK)
                foreground = Preferences.BUTTON_TEXT
                addActionListener { callbacks.onOpen() }
            })

            addSeparator()

            add(JMenuItem("Import Project...").apply {
                accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.SHIFT_DOWN_MASK)
                foreground = Preferences.BUTTON_TEXT
                addActionListener { callbacks.onImportProject() }
            })

            add(JMenuItem("Import...").apply {
                accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.SHIFT_DOWN_MASK)
                foreground = Preferences.BUTTON_TEXT
                addActionListener { callbacks.onImport() }
            })

            // Create Export submenu
            add(JMenu("Export").apply {
                foreground = Preferences.BUTTON_TEXT

                add(JMenuItem("Project...").apply {
                    accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.SHIFT_DOWN_MASK)
                    foreground = Preferences.BUTTON_TEXT
                    addActionListener { callbacks.onExport() }
                })

                add(JMenuItem("Map Matrix...").apply {
                    accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.SHIFT_DOWN_MASK)
                    foreground = Preferences.BUTTON_TEXT
                    addActionListener { callbacks.onGenerateMapMatrix() }
                })
            })
        }
    }

    private fun createGridMenu(): JMenu {
        return JMenu("Grid").apply {
            foreground = Preferences.BUTTON_TEXT

            add(JMenuItem("Change Grid").apply {
                accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_G, 0)
                foreground = Preferences.BUTTON_TEXT
                addActionListener { callbacks.onChangeGrid() }
            })

            add(JMenuItem("Change Cell").apply {
                accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_C, 0)
                foreground = Preferences.BUTTON_TEXT
                addActionListener { callbacks.onChangeCell() }
            })

            addSeparator()

            add(JMenuItem("Zoom in").apply {
                accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0)
                foreground = Preferences.BUTTON_TEXT
                addActionListener { callbacks.onZoomIn() }
            })

            add(JMenuItem("Zoom out").apply {
                accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0)
                foreground = Preferences.BUTTON_TEXT
                addActionListener { callbacks.onZoomOut() }
            })

            add(JMenuItem("Reset zoom").apply {
                accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_0, 0)
                foreground = Preferences.BUTTON_TEXT
                addActionListener { callbacks.onResetZoom() }
            })
        }
    }
}

interface MenuCallbacks {
    fun onNew()
    fun onOpen()
    fun onImport()
    fun onExport()
    fun onGenerateMapMatrix()
    fun onChangeGrid()
    fun onZoomIn()
    fun onZoomOut()
    fun onResetZoom()
    fun onChangeCell()
    fun onImportProject()
}
