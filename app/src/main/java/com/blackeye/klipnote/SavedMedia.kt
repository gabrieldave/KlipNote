package com.blackeye.klipnote

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.core.content.ContextCompat

fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(ClipboardManager::class.java) ?: return
    cm.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.app_name), text))
}

/** Guarda el guion en Descargas/KlipNote/Texto y opcionalmente abre el selector para compartirlo. */
fun saveTranscriptTxt(
    context: Context,
    title: String,
    transcript: String
): Uri? = SealDownloader.saveTranscriptTxt(context, title, transcript)

fun shareTranscriptTxt(context: Context, uri: Uri, displayName: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, displayName)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(
            Intent.createChooser(send, context.getString(R.string.share_txt_title))
        )
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.error_no_app_share), Toast.LENGTH_SHORT).show()
    }
}

/** Guarda el .txt y, si [share] es true, abre el selector con la Uri content://. */
fun saveOrShareTranscriptTxt(
    context: Context,
    title: String,
    transcript: String,
    share: Boolean
): Uri? {
    val uri = saveTranscriptTxt(context, title, transcript) ?: return null
    if (share) {
        val name = SealDownloader.sanitizeFileName(title.ifBlank { "guion" }) + ".txt"
        shareTranscriptTxt(context, uri, name)
    }
    return uri
}

/**
 * Guarda el guion como `.txt` en Descargas/KlipNote/Texto, copia el prompt 4 en 1 al portapapeles
 * como respaldo y abre el selector de Android adjuntando el `.txt` junto con el prompt.
 */
fun sendTranscriptWithAiPack(
    context: Context,
    title: String,
    transcript: String
): Boolean {
    val uri = saveTranscriptTxt(context, title, transcript)
    if (uri == null) {
        Toast.makeText(
            context,
            context.getString(R.string.save_txt_failed),
            Toast.LENGTH_SHORT
        ).show()
        return false
    }
    val displayName = SealDownloader.sanitizeFileName(title.ifBlank { "guion" }) + ".txt"
    val prompt = AiStudyPrompts.buildSendWithAiPack(context, title)
    copyToClipboard(context, prompt)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, prompt)
        putExtra(Intent.EXTRA_SUBJECT, displayName)
        clipData = ClipData.newRawUri(displayName, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try {
        context.startActivity(
            Intent.createChooser(send, context.getString(R.string.send_with_ai_title))
        )
        Toast.makeText(
            context,
            context.getString(R.string.send_with_ai_toast),
            Toast.LENGTH_LONG
        ).show()
        true
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(
            context,
            context.getString(R.string.ai_share_unavailable),
            Toast.LENGTH_LONG
        ).show()
        false
    }
}

/**
 * Archivo guardado en Descargas/KlipNote (MediaStore content Uri).
 */
data class SavedMediaInfo(
    val uri: Uri,
    val displayName: String,
    val isVideo: Boolean
) {
    val mimeType: String get() = if (isVideo) "video/mp4" else "audio/mp4"
    fun openLabel(context: Context): String =
        context.getString(if (isVideo) R.string.media_open_video else R.string.media_open_audio)
}

object SavedMediaActions {

    fun openWithPlayer(context: Context, media: SavedMediaInfo) {
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(media.uri, media.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(Intent.createChooser(view, media.openLabel(context)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, context.getString(R.string.error_no_app_open), Toast.LENGTH_SHORT).show()
        }
    }

    fun share(context: Context, media: SavedMediaInfo) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = media.mimeType
            putExtra(Intent.EXTRA_STREAM, media.uri)
            putExtra(Intent.EXTRA_SUBJECT, media.displayName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(
                Intent.createChooser(send, context.getString(R.string.share_file_title, media.displayName))
            )
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, context.getString(R.string.error_no_app_share), Toast.LENGTH_SHORT).show()
        }
    }

    fun openDownloadsFolder(context: Context) {
        val app = context.applicationContext
        // 1) Lista de descargas del sistema
        try {
            val dm = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(dm)
            return
        } catch (_: Exception) {
            // continuar
        }
        // 2) DocumentsUI apuntando a Download/KlipNote
        try {
            val docId = "primary:Download/KlipNote"
            val uri = DocumentsContract.buildDocumentUri(
                "com.android.externalstorage.documents",
                docId
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (Build.VERSION.SDK_INT >= 26) {
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
                }
            }
            context.startActivity(intent)
            return
        } catch (_: Exception) {
            // continuar
        }
        try {
            val browse = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    Uri.parse("content://com.android.externalstorage.documents/document/primary:Download"),
                    DocumentsContract.Document.MIME_TYPE_DIR
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browse)
            return
        } catch (_: Exception) {
            // fallthrough
        }
        Toast.makeText(
            app,
            context.getString(R.string.hint_find_in_downloads),
            Toast.LENGTH_LONG
        ).show()
    }

    fun fromHistory(
        downloadUri: String?,
        downloadDisplayName: String?,
        downloadIsVideo: Boolean
    ): SavedMediaInfo? {
        if (downloadUri.isNullOrBlank() || downloadDisplayName.isNullOrBlank()) return null
        return runCatching {
            SavedMediaInfo(
                uri = Uri.parse(downloadUri),
                displayName = downloadDisplayName,
                isVideo = downloadIsVideo
            )
        }.getOrNull()
    }
}

fun vibrateShort(context: Context) {
    runCatching {
        val vibrator = ContextCompat.getSystemService(context, android.os.Vibrator::class.java)
            ?: return
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(
                android.os.VibrationEffect.createOneShot(40, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(40)
        }
    }
}
