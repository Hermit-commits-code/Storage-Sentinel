package com.example.storagesentinel.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

val Icons.Filled.Folder: ImageVector
    get() {
        if (_folder != null) {
            return _folder!!
        }
        _folder = materialIcon(name = "Filled.Folder") {
            materialPath {
                moveTo(10f, 4f)
                horizontalLineTo(4f)
                curveTo(2.9f, 4f, 2.01f, 4.9f, 2.01f, 6f)
                lineTo(2f, 18f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineTo(20f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(8f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                horizontalLineTo(12f)
                lineTo(10f, 4f)
                close()
            }
        }
        return _folder!!
    }

private var _folder: ImageVector? = null

val Icons.Filled.Note: ImageVector
    get() {
        if (_note != null) {
            return _note!!
        }
        _note = materialIcon(name = "Filled.Note") {
            materialPath {
                moveTo(14f, 2f)
                horizontalLineTo(6f)
                curveTo(4.9f, 2f, 4f, 2.9f, 4f, 4f)
                verticalLineToRelative(16f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(12f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(8f)
                lineTo(14f, 2f)
                close()
                moveTo(18f, 20f)
                horizontalLineTo(6f)
                verticalLineTo(4f)
                horizontalLineToRelative(7f)
                verticalLineToRelative(5f)
                horizontalLineToRelative(5f)
                verticalLineToRelative(11f)
                close()
            }
        }
        return _note!!
    }

private var _note: ImageVector? = null

val Icons.Filled.Science: ImageVector
    get() {
        if (_science != null) {
            return _science!!
        }
        _science = materialIcon(name = "Filled.Science") {
            materialPath {
                moveTo(19.8f, 18.4f)
                lineTo(14f, 12.6f)
                verticalLineTo(6f)
                horizontalLineToRelative(-4f)
                verticalLineToRelative(6.6f)
                lineTo(4.2f, 18.4f)
                curveTo(3.5f, 19.1f, 4f, 20.2f, 5f, 20.2f)
                horizontalLineToRelative(14f)
                curveToRelative(1f, 0f, 1.5f, -1.1f, 0.8f, -1.8f)
                close()
            }
        }
        return _science!!
    }

private var _science: ImageVector? = null

val Icons.Filled.FileCopy: ImageVector
    get() {
        if (_fileCopy != null) {
            return _fileCopy!!
        }
        _fileCopy = materialIcon(name = "Filled.FileCopy") {
            materialPath {
                moveTo(16f, 1f)
                horizontalLineTo(4f)
                curveTo(2.9f, 1f, 2f, 1.9f, 2f, 3f)
                verticalLineToRelative(14f)
                horizontalLineToRelative(2f)
                verticalLineTo(3f)
                horizontalLineToRelative(12f)
                verticalLineTo(1f)
                close()
                moveTo(19f, 5f)
                horizontalLineTo(8f)
                curveTo(6.9f, 5f, 6f, 5.9f, 6f, 7f)
                verticalLineToRelative(14f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(11f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(7f)
                curveTo(21f, 5.9f, 20.1f, 5f, 19f, 5f)
                close()
                moveTo(19f, 21f)
                horizontalLineTo(8f)
                verticalLineTo(7f)
                horizontalLineToRelative(11f)
                verticalLineTo(21f)
                close()
            }
        }
        return _fileCopy!!
    }

private var _fileCopy: ImageVector? = null
