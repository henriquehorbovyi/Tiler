package com.henriquehorbovyi.tiler.keyboard

sealed class KeyboardEvent {
    object Undo : KeyboardEvent()
    object ZoomIn : KeyboardEvent()
    object ZoomOut : KeyboardEvent()
    object ScrollLeft : KeyboardEvent()
    object ScrollRight : KeyboardEvent()
    object ScrollUp : KeyboardEvent()
    object ScrollDown : KeyboardEvent()
    object Grid : KeyboardEvent()
    object ToggleLoadedTilesTab : KeyboardEvent()
    object ResetZoom : KeyboardEvent()
    object SelectAll : KeyboardEvent()
    object ClearSelection : KeyboardEvent()
    object IncreaseTileArea : KeyboardEvent()
    object DecreaseTileArea : KeyboardEvent()
}
