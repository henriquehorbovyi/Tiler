package com.henriquehorbovyi.tiler.keyboard

import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.Toolkit
import java.awt.event.KeyEvent

class KeyboardManager {
    private val listeners = mutableMapOf<KeyboardEvent, (() -> Unit)>()
    private val isMac = System.getProperty("os.name").lowercase().contains("mac")

    init {
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
            .addKeyEventDispatcher(createKeyEventDispatcher())
    }

    private fun createKeyEventDispatcher() = KeyEventDispatcher { event ->
        if (event.id == KeyEvent.KEY_PRESSED) {
            when {
                isUndoEvent(event) -> listeners[KeyboardEvent.Undo]?.invoke()
                isZoomInEvent(event) -> listeners[KeyboardEvent.ZoomIn]?.invoke()
                isZoomOutEvent(event) -> listeners[KeyboardEvent.ZoomOut]?.invoke()
                // Fix scroll directions
                event.keyCode == KeyEvent.VK_W -> listeners[KeyboardEvent.ScrollUp]?.invoke()
                event.keyCode == KeyEvent.VK_S -> listeners[KeyboardEvent.ScrollDown]?.invoke()
                event.keyCode == KeyEvent.VK_A && event.modifiersEx == 0 -> listeners[KeyboardEvent.ScrollLeft]?.invoke()
                event.keyCode == KeyEvent.VK_D -> listeners[KeyboardEvent.ScrollRight]?.invoke()
                event.keyCode == KeyEvent.VK_G -> listeners[KeyboardEvent.Grid]?.invoke()
                event.keyCode == KeyEvent.VK_TAB && event.isAltDown -> {
                    event.consume()  // Prevent default tab behavior
                    listeners[KeyboardEvent.ToggleLoadedTilesTab]?.invoke()
                }
                event.keyCode == KeyEvent.VK_0 -> listeners[KeyboardEvent.ResetZoom]?.invoke()
                event.keyCode == KeyEvent.VK_A && event.isShiftDown -> listeners[KeyboardEvent.SelectAll]?.invoke()
                event.keyCode == KeyEvent.VK_ESCAPE -> listeners[KeyboardEvent.ClearSelection]?.invoke()
                event.keyCode == KeyEvent.VK_CLOSE_BRACKET -> listeners[KeyboardEvent.IncreaseTileArea]?.invoke()
                event.keyCode == KeyEvent.VK_OPEN_BRACKET -> listeners[KeyboardEvent.DecreaseTileArea]?.invoke()
            }
        }
        false
    }

    private fun isUndoEvent(event: KeyEvent): Boolean {
        val menuShortcutKeyMask = Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx
        return event.keyCode == KeyEvent.VK_Z && event.modifiersEx == menuShortcutKeyMask
    }

    private fun isZoomInEvent(event: KeyEvent): Boolean =
        event.keyCode == KeyEvent.VK_PLUS || event.keyCode == KeyEvent.VK_EQUALS

    private fun isZoomOutEvent(event: KeyEvent): Boolean =
        event.keyCode == KeyEvent.VK_MINUS

    fun addEventListener(event: KeyboardEvent, callback: () -> Unit) {
         listeners[event] = callback
    }

    fun removeEventListener(event: KeyboardEvent) {
        listeners.remove(event)
    }
}
