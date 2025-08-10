package com.henriquehorbovyi.tiler

import javax.swing.SwingUtilities

fun main() {
    SwingUtilities.invokeLater(Runnable {
        val window = MainWindow()
        window.isVisible = true
    })
}