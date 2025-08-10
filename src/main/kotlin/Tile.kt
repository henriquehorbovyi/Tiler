package com.henriquehorbovyi.tiling

import java.awt.image.BufferedImage
import javax.swing.ImageIcon

data class Tile(
    val image: BufferedImage,
    val name: String,
    var isSolid: Boolean = true  // Changed from isWalkable to isSolid with default true
) {
    fun getScaledIcon(width: Int, height: Int): ImageIcon =
        ImageIcon(image.getScaledInstance(width, height, BufferedImage.SCALE_SMOOTH))
}
