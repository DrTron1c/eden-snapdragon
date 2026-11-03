// SPDX-FileCopyrightText: Copyright 2026 Eden Emulator Project
// SPDX-License-Identifier: GPL-3.0-or-later

package org.yuzu.yuzu_emu.utils

import android.net.Uri
import okhttp3.OkHttpClient
import okhttp3.Request
import org.yuzu.yuzu_emu.YuzuApplication
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Moves a game's shader cache between devices as a zip, so a cache built on one device can be in
 * place before the game runs on another. Only the cache files are ever written, whatever a zip
 * holds. The emulator builds an installed cache at game start and drops one from another version.
 */
object ShaderCacheTransfer {
    enum class Result { Success, Cancelled, NoCacheInZip, WrongGame, Failed }

    private const val TRANSFERABLE_CACHE = "vulkan.bin"
    private const val DRIVER_CACHE = "vulkan_pipelines.bin"
    private const val TITLE_FILE = "title.txt"
    private const val BACKUP_SUFFIX = ".bak"
    private const val BUFFER_SIZE = 64 * 1024

    private val cacheFiles = listOf(TRANSFERABLE_CACHE, DRIVER_CACHE)

    fun hasCache(cacheDir: File): Boolean = File(cacheDir, TRANSFERABLE_CACHE).isFile

    fun export(cacheDir: File, destination: Uri): Boolean {
        return try {
            val output = YuzuApplication.appContext.contentResolver.openOutputStream(destination)
                ?: throw IOException("Can't open the export file")
            ZipOutputStream(output.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry(TITLE_FILE))
                zip.write(cacheDir.name.toByteArray())
                zip.closeEntry()
                for (name in cacheFiles) {
                    val file = File(cacheDir, name)
                    if (file.isFile) {
                        zip.putNextEntry(ZipEntry(name))
                        file.inputStream().use { it.copyTo(zip, BUFFER_SIZE) }
                        zip.closeEntry()
                    }
                }
            }
            true
        } catch (e: IOException) {
            Log.error("[ShaderCacheTransfer] Export failed: ${e.message}")
            false
        }
    }

    fun importFrom(cacheDir: File, source: Uri): Result {
        return try {
            val input = YuzuApplication.appContext.contentResolver.openInputStream(source)
                ?: return Result.Failed
            input.use { install(cacheDir, it) }
        } catch (e: IOException) {
            Log.error("[ShaderCacheTransfer] Import failed: ${e.message}")
            Result.Failed
        }
    }

    /** [onProgress] returns true to cancel, as ProgressDialogFragment's callback does. */
    fun download(
        cacheDir: File,
        url: String,
        onProgress: (max: Long, progress: Long) -> Boolean
    ): Result {
        val context = YuzuApplication.appContext
        val zipFile = File(context.cacheDir, "shader_cache_download.zip")
        return try {
            val request = Request.Builder().url(url).build()
            OkHttpClient().newCall(request).execute().use { response ->
                val body = response.body
                if (!response.isSuccessful || body == null) {
                    return Result.Failed
                }
                val total = body.contentLength()
                var received = 0L
                val buffer = ByteArray(BUFFER_SIZE)
                body.byteStream().use { input ->
                    zipFile.outputStream().use { output ->
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) {
                                break
                            }
                            output.write(buffer, 0, count)
                            received += count
                            if (onProgress(maxOf(total, received), received)) {
                                return Result.Cancelled
                            }
                        }
                    }
                }
            }
            zipFile.inputStream().use { install(cacheDir, it) }
        } catch (e: IOException) {
            Log.error("[ShaderCacheTransfer] Download failed: ${e.message}")
            Result.Failed
        } catch (e: IllegalArgumentException) {
            Log.error("[ShaderCacheTransfer] Bad link: ${e.message}")
            Result.Failed
        } finally {
            zipFile.delete()
        }
    }

    private fun install(cacheDir: File, input: InputStream): Result {
        val staging = File(YuzuApplication.appContext.cacheDir, "shader_cache_staging")
        staging.deleteRecursively()
        staging.mkdirs()
        try {
            var title: String? = null
            ZipInputStream(input.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    // Folders in the zip are ignored, so nothing can land outside the cache.
                    val name = entry.name.substringAfterLast('/').substringAfterLast('\\')
                    if (!entry.isDirectory) {
                        if (name == TITLE_FILE) {
                            title = zip.readBytes().decodeToString().trim()
                        } else if (name in cacheFiles) {
                            File(staging, name).outputStream().use { zip.copyTo(it, BUFFER_SIZE) }
                        }
                    }
                    entry = zip.nextEntry
                }
            }

            if (title != null && !title.equals(cacheDir.name, ignoreCase = true)) {
                return Result.WrongGame
            }
            if (!File(staging, TRANSFERABLE_CACHE).isFile) {
                return Result.NoCacheInZip
            }

            cacheDir.mkdirs()
            for (name in cacheFiles) {
                val incoming = File(staging, name)
                if (incoming.isFile) {
                    val target = File(cacheDir, name)
                    if (target.isFile) {
                        target.renameTo(File(cacheDir, name + BACKUP_SUFFIX))
                    }
                    incoming.copyTo(target, overwrite = true)
                }
            }
            return Result.Success
        } catch (e: IOException) {
            Log.error("[ShaderCacheTransfer] Install failed: ${e.message}")
            return Result.Failed
        } catch (e: IllegalArgumentException) {
            Log.error("[ShaderCacheTransfer] Bad zip: ${e.message}")
            return Result.Failed
        } finally {
            staging.deleteRecursively()
        }
    }
}
