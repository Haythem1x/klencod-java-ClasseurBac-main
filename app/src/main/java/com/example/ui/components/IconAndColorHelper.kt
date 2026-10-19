package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Translate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object IconAndColorHelper {
    val AVAILABLE_ICONS = listOf(
        "book" to Icons.Default.Book,
        "science" to Icons.Default.Science,
        "biotech" to Icons.Default.Biotech,
        "calculate" to Icons.Default.Calculate,
        "menu_book" to Icons.Default.MenuBook,
        "auto_stories" to Icons.Default.AutoStories,
        "psychology" to Icons.Default.Psychology,
        "translate" to Icons.Default.Translate,
        "code" to Icons.Default.Code,
        "bookmark" to Icons.Default.Bookmark,
        "school" to Icons.Default.School,
        "folder" to Icons.Default.Folder,
        "draw" to Icons.Default.Draw,
        "description" to Icons.Default.Description
    )

    val AVAILABLE_COLORS = listOf(
        0xFF1A365D.toInt(), // Deep Academic Navy
        0xFF2563EB.toInt(), // Blue
        0xFF0D9488.toInt(), // Teal
        0xFF059669.toInt(), // Green
        0xFFEA580C.toInt(), // Orange
        0xFFD97706.toInt(), // Amber
        0xFFDC2626.toInt(), // Red
        0xFF9333EA.toInt(), // Purple
        0xFFDB2777.toInt(), // Pink
        0xFF4F46E5.toInt()  // Indigo
    )

    fun getIcon(key: String): ImageVector {
        return AVAILABLE_ICONS.firstOrNull { it.first == key }?.second ?: Icons.Default.Book
    }

    fun getColor(argb: Int): Color {
        return Color(argb)
    }
}
