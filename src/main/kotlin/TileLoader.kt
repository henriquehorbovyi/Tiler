package com.henriquehorbovyi.tiler

import java.io.File
import javax.imageio.ImageIO

class TileLoader {
    private val _loadedTiles = mutableListOf<Tile>()
    val loadedTiles: List<Tile> get() = _loadedTiles.toList()

    fun loadTiles(files: Array<File>) {
        files.forEach { file ->
            if (!_loadedTiles.any { it.name == file.name }) {  // Check if tile already exists
                try {
                    val image = ImageIO.read(file)
                    _loadedTiles.add(Tile(image, file.name))
                } catch (e: Exception) {
                    println("Error loading tile ${file.name}: ${e.message}")
                }
            }
        }
    }
}
