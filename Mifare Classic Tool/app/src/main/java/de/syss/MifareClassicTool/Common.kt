/*
 * Copyright 2013 Gerhard Klostermeier
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.syss.MifareClassicTool

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.app.Application
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.database.Cursor
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.NfcA
import android.os.Build
import android.provider.OpenableColumns
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.util.SparseArray
import android.util.TypedValue
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.preference.PreferenceManager
import de.syss.MifareClassicTool.Activities.IActivityThatReactsToSave
import de.syss.MifareClassicTool.Activities.Preferences.Preference.AutoCopyUID
import de.syss.MifareClassicTool.Activities.Preferences.Preference.UIDFormat
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Arrays
import java.util.GregorianCalendar
import java.util.Locale

/**
 * Common functions and variables for all Activities.
 * @author Gerhard Klostermeier
 */
open class Common : Application() {

    /**
     * Possible operations on a MIFARE Classic Tag.
     */
    enum class Operation {
        Read, Write, Increment, DecTransRest, ReadKeyA, ReadKeyB, ReadAC,
        WriteKeyA, WriteKeyB, WriteAC
    }

    override fun onCreate() {
        super.onCreate()
        mAppContext = applicationContext
        mScale = resources.displayMetrics.density

        try {
            mVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0)
                ).versionName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0).versionName
            }
        } catch (e: NameNotFoundException) {
            Log.d(LOG_TAG, "Version not found.")
        }
    }

    companion object {
        /**
         * True if this is the donate version of MCT.
         */
        const val IS_DONATE_VERSION: Boolean = false

        /**
         * The directory name of the root directory of this app.
         */
        const val HOME_DIR: String = "/MifareClassicTool"

        /**
         * The directory name of the key files directory.
         * (sub directory of [HOME_DIR].)
         */
        const val KEYS_DIR: String = "key-files"

        /**
         * The directory name of the dump files directory.
         * (sub directory of [HOME_DIR].)
         */
        const val DUMPS_DIR: String = "dump-files"

        /**
         * The directory name of the folder where temporary files are
         * stored. The directory will be cleaned during the creation of
         * the main activity
         * ([de.syss.MifareClassicTool.Activities.MainMenu]).
         * (sub directory of [HOME_DIR].)
         */
        const val TMP_DIR: String = "tmp"

        /**
         * This file contains some standard MIFARE keys.
         * - 0xFFFFFFFFFFFF - Un-formatted, factory fresh tags.
         * - 0xA0A1A2A3A4A5 - First sector of the tag (MIFARE MAD).
         * - 0xD3F7D3F7D3F7 - NDEF formatted tags.
         */
        const val STD_KEYS: String = "std.keys"

        /**
         * Keys taken from SLURP by Anders Sundman anders@4zm.org
         * the proxmark3 repositories and a short google search.
         */
        const val STD_KEYS_EXTENDED: String = "extended-std.keys"

        /**
         * Keys from the extended-std.keys file which are known to work on hotel cards.
         */
        const val STD_HOTEL_KEYS: String = "hotel-std.keys"

        /**
         * Log file with UIDs which have been discovered in the past.
         */
        const val UID_LOG_FILE: String = "uid-log-file.txt"

        private val LOG_TAG: String = Common::class.java.simpleName

        /**
         * The last detected tag.
         * Set by [treatAsNewTag]
         */
        private var mTag: Tag? = null

        /**
         * The last detected UID.
         * Set by [treatAsNewTag]
         */
        private var mUID: ByteArray? = null

        /**
         * Just a global storage to save key maps generated by
         * [de.syss.MifareClassicTool.Activities.KeyMapCreator]
         */
        private var mKeyMap: SparseArray<Array<ByteArray?>>? = null

        /**
         * Global storage for the point where
         * [de.syss.MifareClassicTool.Activities.KeyMapCreator] started to
         * create a key map.
         */
        private var mKeyMapFrom: Int = -1

        /**
         * Global storage for the point where
         * [de.syss.MifareClassicTool.Activities.KeyMapCreator] ended to
         * create a key map.
         */
        private var mKeyMapTo: Int = -1

        /**
         * The version code from the Android manifest.
         */
        private var mVersionCode: String? = null

        /**
         * If NFC is disabled and the user chose to use MCT in editor only mode,
         * the choice is remembered here.
         */
        private var mUseAsEditorOnly: Boolean = false

        /**
         * 1 if the device does support MIFARE Classic. -1 if it doesn't support
         * it. 0 if the support check was not yet performed.
         */
        private var mHasMifareClassicSupport: Int = 0

        /**
         * The component name of the activity that is in foreground and
         * should receive the new detected tag object by an external reader.
         */
        private var mPendingComponentName: ComponentName? = null

        private var mNfcAdapter: NfcAdapter? = null
        private var mAppContext: Context? = null
        private var mScale: Float = 0f

        /**
         * Check if this is the first installation of this app or just an update.
         * @return True if app was not installed before. False otherwise.
         */
        @JvmStatic
        fun isFirstInstall(): Boolean {
            val context = mAppContext ?: return true
            return try {
                val firstInstallTime: Long
                val lastUpdateTime: Long
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val pkgInfo = context.packageManager.getPackageInfo(
                        context.packageName,
                        PackageManager.PackageInfoFlags.of(0)
                    )
                    firstInstallTime = pkgInfo.firstInstallTime
                    lastUpdateTime = pkgInfo.lastUpdateTime
                } else {
                    @Suppress("DEPRECATION")
                    val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    firstInstallTime = pkgInfo.firstInstallTime
                    lastUpdateTime = pkgInfo.lastUpdateTime
                }
                firstInstallTime == lastUpdateTime
            } catch (e: PackageManager.NameNotFoundException) {
                true
            }
        }

        /**
         * Create a File object with a path that consists of the apps file
         * directory, the [HOME_DIR] and the relative path.
         * @param relativePath The relative path that gets appended to the base path.
         * @return A File object with the absolute path of app file directory + [HOME_DIR] + relativePath.
         */
        @JvmStatic
        fun getFile(relativePath: String): File {
            return File(mAppContext!!.filesDir.toString() + HOME_DIR + "/" + relativePath)
        }

        /**
         * Read a file line by line. The file should be a simple text file.
         * Empty lines will not be read.
         * @param file The file to read.
         * @param readAll If true, comments and empty lines will be read too.
         * @param context The context in which the possible "Out of memory"-Toast will be shown.
         * @return Array of strings representing the lines of the file.
         */
        @JvmStatic
        fun readFileLineByLine(file: File?, readAll: Boolean, context: Context?): Array<String>? {
            if (file == null || !file.exists()) {
                return null
            }
            var ret: Array<String>?
            var reader: BufferedReader? = null
            try {
                reader = BufferedReader(FileReader(file))
                ret = readLineByLine(reader, readAll, context)
            } catch (ex: FileNotFoundException) {
                ret = null
            } finally {
                if (reader != null) {
                    try {
                        reader.close()
                    } catch (e: IOException) {
                        Log.e(LOG_TAG, "Error while closing file.", e)
                        ret = null
                    }
                }
            }
            return ret
        }

        /**
         * Read the URI source line by line.
         * @param uri The URI to read from.
         * @param readAll If true, comments and empty lines will be read too.
         * @param context The context for the content resolver and in which error/info Toasts are shown.
         * @return The content of the URI, each line representing an array item or Null in case of an read error.
         */
        @JvmStatic
        fun readUriLineByLine(uri: Uri?, readAll: Boolean, context: Context?): Array<String>? {
            if (uri == null || context == null) {
                return null
            }
            val contentStream: InputStream = try {
                context.contentResolver.openInputStream(uri) ?: return null
            } catch (ex: Exception) {
                when (ex) {
                    is FileNotFoundException, is SecurityException -> return null
                    else -> throw ex
                }
            }

            val reader = BufferedReader(InputStreamReader(contentStream))
            val ret = readLineByLine(reader, readAll, context)
            try {
                reader.close()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "Error while closing file.", e)
                return null
            }
            return ret
        }

        /**
         * Read the URI as raw bytes.
         * @param uri The URI to read from.
         * @param context The context for the content resolver.
         * @return The content of the URI as raw bytes or Null in case of an read error.
         */
        @JvmStatic
        fun readUriRaw(uri: Uri?, context: Context?): ByteArray? {
            if (uri == null || context == null) {
                return null
            }
            val contentStream: InputStream = try {
                context.contentResolver.openInputStream(uri) ?: return null
            } catch (ex: Exception) {
                when (ex) {
                    is FileNotFoundException, is SecurityException -> return null
                    else -> throw ex
                }
            }

            var len: Int
            val data = ByteArray(16384)
            val buffer = ByteArrayOutputStream()
            try {
                while (contentStream.read(data, 0, data.size).also { len = it } != -1) {
                    buffer.write(data, 0, len)
                }
            } catch (e: IOException) {
                Log.e(LOG_TAG, "Error while reading from file.", e)
                return null
            }

            try {
                contentStream.close()
            } catch (e: IOException) {
                return null
            }
            return buffer.toByteArray()
        }

        /**
         * Read as BufferedReader line by line with some exceptions.
         * Empty lines and leading/tailing whitespaces will be ignored.
         */
        private fun readLineByLine(
            reader: BufferedReader,
            readAll: Boolean,
            context: Context?
        ): Array<String>? {
            var line: String?
            val linesArray = ArrayList<String>()
            try {
                while (reader.readLine().also { line = it } != null) {
                    var currentLine = line!!.trim()
                    if (!readAll) {
                        if (currentLine.startsWith("#") || currentLine.isEmpty()) {
                            continue
                        }
                        currentLine = currentLine.split("#")[0].trim()
                    }
                    try {
                        linesArray.add(currentLine)
                    } catch (e: OutOfMemoryError) {
                        if (context != null) {
                            Toast.makeText(context, R.string.info_file_too_big, Toast.LENGTH_LONG).show()
                        }
                        return null
                    }
                }
            } catch (ex: IOException) {
                Log.e(LOG_TAG, "Error while reading from file.", ex)
                return null
            }
            return if (!linesArray.isEmpty()) {
                linesArray.toTypedArray()
            } else {
                arrayOf("")
            }
        }

        /**
         * Get the file name from an URI object.
         * Taken from https://stackoverflow.com/a/25005243
         * @param uri The URI to get the file name from.
         * @param context The Context for the content resolver.
         * @return The file name of the URI object.
         */
        @JvmStatic
        fun getFileName(uri: Uri, context: Context): String? {
            var result: String? = null
            if (uri.scheme == "content") {
                try {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (index >= 0) {
                                result = cursor.getString(index)
                            }
                        }
                    }
                } catch (ignored: Exception) {
                }
            }
            if (result == null) {
                result = uri.path
                if (result != null) {
                    val cut = result!!.lastIndexOf('/')
                    if (cut != -1) {
                        result = result!!.substring(cut + 1)
                    }
                }
            }
            return result
        }

        /**
         * Check if the file already exists. If so, present a dialog to the user
         * with the options: "Replace", "Append" and "Cancel".
         */
        @JvmStatic
        fun checkFileExistenceAndSave(
            file: File,
            lines: Array<String>,
            isDump: Boolean,
            context: Context,
            activity: IActivityThatReactsToSave
        ) {
            if (file.exists()) {
                val message = if (isDump) {
                    R.string.dialog_save_conflict_dump
                } else {
                    R.string.dialog_save_conflict_keyfile
                }

                AlertDialog.Builder(context)
                    .setTitle(R.string.dialog_save_conflict_title)
                    .setMessage(message)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(R.string.action_replace) { _, _ ->
                        if (saveFile(file, lines, false)) {
                            Toast.makeText(context, R.string.info_save_successful, Toast.LENGTH_LONG).show()
                            activity.onSaveSuccessful()
                        } else {
                            Toast.makeText(context, R.string.info_save_error, Toast.LENGTH_LONG).show()
                            activity.onSaveFailure()
                        }
                    }
                    .setNeutralButton(R.string.action_append) { _, _ ->
                        if (saveFileAppend(file, lines, isDump)) {
                            Toast.makeText(context, R.string.info_save_successful, Toast.LENGTH_LONG).show()
                            activity.onSaveSuccessful()
                        } else {
                            Toast.makeText(context, R.string.info_save_error, Toast.LENGTH_LONG).show()
                            activity.onSaveFailure()
                        }
                    }
                    .setNegativeButton(R.string.action_cancel) { _, _ ->
                        activity.onSaveFailure()
                    }
                    .show()
            } else {
                if (saveFile(file, lines, false)) {
                    Toast.makeText(context, R.string.info_save_successful, Toast.LENGTH_LONG).show()
                    activity.onSaveSuccessful()
                } else {
                    Toast.makeText(context, R.string.info_save_error, Toast.LENGTH_LONG).show()
                    activity.onSaveFailure()
                }
            }
        }

        /**
         * Append an array of strings (each field is one line) to a given file.
         */
        @JvmStatic
        fun saveFileAppend(file: File, lines: Array<String>, comment: Boolean): Boolean {
            var targetLines = lines
            if (comment) {
                val newLines = arrayOfNulls<String>(lines.size + 4)
                System.arraycopy(lines, 0, newLines, 4, lines.size)
                newLines[0] = "" // Notice: In Java line 557: newLines[1] = "", [2] = "# Append", [3] = ""
                newLines[1] = ""
                newLines[2] = "# Append #######################"
                newLines[3] = ""
                @Suppress("UNCHECKED_CAST")
                targetLines = newLines as Array<String>
            }
            return saveFile(file, targetLines, true)
        }

        /**
         * Write an array of strings (each field is one line) to a given file.
         */
        @JvmStatic
        fun saveFile(file: File?, lines: Array<String>?, append: Boolean): Boolean {
            var error = false
            if (file != null && lines != null && lines.isNotEmpty()) {
                var bw: BufferedWriter? = null
                try {
                    bw = BufferedWriter(FileWriter(file, append))
                    if (append) {
                        bw.newLine()
                    }
                    val iLast = lines.size - 1
                    for (i in 0 until iLast) {
                        bw.write(lines[i])
                        bw.newLine()
                    }
                    bw.write(lines[iLast])
                } catch (ex: Exception) {
                    when (ex) {
                        is IOException, is NullPointerException -> {
                            Log.e(LOG_TAG, "Error while writing to '" + file.name + "' file.", ex)
                            error = true
                        }
                        else -> throw ex
                    }
                } finally {
                    if (bw != null) {
                        try {
                            bw.close()
                        } catch (e: IOException) {
                            Log.e(LOG_TAG, "Error while closing file.", e)
                            error = true
                        }
                    }
                }
            } else {
                error = true
            }
            return !error
        }

        /**
         * Write text lines to a given content URI.
         */
        @JvmStatic
        fun saveFile(contentUri: Uri?, lines: Array<String>?, context: Context?): Boolean {
            if (contentUri == null || lines == null || context == null || lines.isEmpty()) {
                return false
            }
            val concatenatedLines = TextUtils.join(
                System.getProperty("line.separator") ?: "\n",
                lines
            )
            val bytes = concatenatedLines.toByteArray()
            return saveFile(contentUri, bytes, context)
        }

        /**
         * Write an array of bytes (raw data) to a given content URI.
         */
        @JvmStatic
        fun saveFile(contentUri: Uri?, bytes: ByteArray?, context: Context?): Boolean {
            if (contentUri == null || bytes == null || context == null || bytes.isEmpty()) {
                return false
            }
            val output: OutputStream? = try {
                context.contentResolver.openOutputStream(contentUri, "rw")
            } catch (ex: FileNotFoundException) {
                return false
            }
            if (output != null) {
                try {
                    output.write(bytes)
                    output.flush()
                    output.close()
                } catch (ex: IOException) {
                    return false
                }
            }
            return true
        }

        /**
         * Get the shared preferences with application context for saving
         * and loading ("global") values.
         */
        @JvmStatic
        fun getPreferences(): SharedPreferences {
            return PreferenceManager.getDefaultSharedPreferences(mAppContext!!)
        }

        /**
         * Enables the NFC foreground dispatch system for the given Activity.
         */
        @JvmStatic
        fun enableNfcForegroundDispatch(targetActivity: Activity) {
            val adapter = mNfcAdapter
            if (adapter != null && adapter.isEnabled) {
                val intent = Intent(targetActivity, targetActivity.javaClass)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                val pendingIntent = PendingIntent.getActivity(
                    targetActivity, 0, intent, PendingIntent.FLAG_MUTABLE
                )
                try {
                    adapter.enableForegroundDispatch(
                        targetActivity,
                        pendingIntent,
                        null,
                        arrayOf(arrayOf(NfcA::class.java.name))
                    )
                } catch (ex: Exception) {
                    when (ex) {
                        is IllegalStateException, is SecurityException -> {
                            Log.d(
                                LOG_TAG,
                                "Error: Could not enable the NFC foreground" +
                                        "dispatch system. The activity was not in foreground."
                            )
                        }
                        else -> throw ex
                    }
                }
            }
        }

        /**
         * Disable the NFC foreground dispatch system for the given Activity.
         */
        @JvmStatic
        fun disableNfcForegroundDispatch(targetActivity: Activity) {
            val adapter = mNfcAdapter
            if (adapter != null && adapter.isEnabled) {
                try {
                    adapter.disableForegroundDispatch(targetActivity)
                } catch (ex: IllegalStateException) {
                    Log.d(
                        LOG_TAG,
                        "Error: Could not disable the NFC foreground" +
                                "dispatch system. The activity was not in foreground."
                    )
                }
            }
        }

        /**
         * Log the UID to a file.
         */
        @JvmStatic
        fun logUid(uid: String) {
            val log = File(mAppContext!!.filesDir, HOME_DIR + File.separator + UID_LOG_FILE)
            val calendar = GregorianCalendar()
            val fmt = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            fmt.calendar = calendar
            val dateFormatted = fmt.format(calendar.time)
            val logEntry = arrayOf("$dateFormatted: $uid")
            saveFile(log, logEntry, true)
        }

        /**
         * For Activities which want to treat new Intents as Intents with a new
         * Tag attached.
         */
        @JvmStatic
        fun treatAsNewTag(intent: Intent, context: Context): Int {
            if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
                val tag: Tag?
                try {
                    tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
                    }
                } catch (ex: RuntimeException) {
                    return -3
                }
                if (tag == null) {
                    return -3
                }
                setTag(tag)
                logUid(bytes2Hex(tag.id))

                val isCopyUID = getPreferences().getBoolean(AutoCopyUID.toString(), false)
                if (isCopyUID) {
                    val format = getPreferences().getInt(UIDFormat.toString(), 0)
                    val fmtUID = byte2FmtString(tag.id, format)
                    Toast.makeText(
                        context,
                        "UID " + context.resources.getString(R.string.info_copied_to_clipboard)
                            .lowercase(Locale.getDefault()) + " (" + fmtUID + ")",
                        Toast.LENGTH_SHORT
                    ).show()
                    copyToClipboard(fmtUID, context, false)
                } else {
                    var id = context.resources.getString(R.string.info_new_tag_found) + " (UID: "
                    id += bytes2Hex(tag.id)
                    id += ")"
                    Toast.makeText(context, id, Toast.LENGTH_LONG).show()
                }
                return checkMifareClassicSupport(tag, context)
            }
            return -4
        }

        /**
         * Check if the device supports the MIFARE Classic technology.
         */
        @JvmStatic
        fun hasMifareClassicSupport(): Boolean {
            if (mHasMifareClassicSupport != 0) {
                return mHasMifareClassicSupport == 1
            }

            if (NfcAdapter.getDefaultAdapter(mAppContext) == null) {
                mUseAsEditorOnly = true
                mHasMifareClassicSupport = -1
                return false
            }

            val isLenovoP2 = Build.MANUFACTURER == "LENOVO" && Build.MODEL == "Lenovo P2a42"
            var device = File("/dev/bcm2079x-i2c")
            if (!isLenovoP2 && device.exists()) {
                mHasMifareClassicSupport = -1
                return false
            }

            device = File("/dev/pn544")
            if (device.exists()) {
                mHasMifareClassicSupport = 1
                return true
            }

            var libsFolder = File("/system/lib")
            if (!libsFolder.exists()) {
                libsFolder = File("/system/lib64")
            }
            val libs = libsFolder.listFiles()
            if (libs != null) {
                for (lib in libs) {
                    if (lib.isFile && lib.name.startsWith("libnfc") && lib.name.contains("brcm")) {
                        mHasMifareClassicSupport = -1
                        return false
                    }
                }
            }

            mHasMifareClassicSupport = 1
            return true
        }

        /**
         * Check if the tag and the device support the MIFARE Classic technology.
         */
        @JvmStatic
        fun checkMifareClassicSupport(tag: Tag?, context: Context?): Int {
            if (tag == null || context == null) {
                return -3
            }

            if (Arrays.asList(*tag.techList).contains(MifareClassic::class.java.name)) {
                try {
                    MifareClassic.get(tag)
                } catch (ex: RuntimeException) {
                    return -2
                }
                return 0
            } else {
                val nfca = NfcA.get(tag) ?: return -2
                val sak = nfca.sak.toByte()
                return if (((sak.toInt() shr 1) and 1) == 1) {
                    -2
                } else {
                    if (((sak.toInt() shr 3) and 1) == 1) {
                        -1
                    } else {
                        -2
                    }
                }
            }
        }

        /**
         * Open another app.
         */
        @JvmStatic
        fun openApp(context: Context, packageName: String): Boolean {
            val manager = context.packageManager
            return try {
                val i = manager.getLaunchIntentForPackage(packageName) ?: return false
                i.addCategory(Intent.CATEGORY_LAUNCHER)
                context.startActivity(i)
                true
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Check whether the service of the "External NFC" app is running or not.
         */
        @JvmStatic
        fun isExternalNfcServiceRunning(context: Context): Int {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                @Suppress("DEPRECATION")
                for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                    if ("eu.dedb.nfc.service.NfcService" == service.service.className) {
                        return 1
                    }
                }
                return 0
            }
            return -1
        }

        /**
         * Find out whether the "External NFC" app is installed or not.
         */
        @JvmStatic
        fun hasExternalNfcInstalled(context: Context): Boolean {
            return isAppInstalled("eu.dedb.nfc.service", context)
        }

        /**
         * Check whether an app is installed or not.
         */
        @JvmStatic
        fun isAppInstalled(uri: String, context: Context): Boolean {
            val pm = context.packageManager
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(uri, PackageManager.PackageInfoFlags.of(PackageManager.GET_ACTIVITIES.toLong()))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES)
                }
                true
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Create a connected [MCReader] if there is a present MIFARE Classic tag.
         */
        @JvmStatic
        fun checkForTagAndCreateReader(context: Context): MCReader? {
            val currentTag = mTag
            val reader = if (currentTag != null) MCReader.get(currentTag) else null
            if (reader != null) {
                var tagLost = false
                try {
                    reader.connect()
                } catch (e: Exception) {
                    tagLost = true
                }
                if (!tagLost && !reader.isConnected) {
                    reader.close()
                    tagLost = true
                }
                if (!tagLost) {
                    return reader
                }
            }

            Toast.makeText(context, R.string.info_no_tag_found, Toast.LENGTH_LONG).show()
            return null
        }

        /**
         * Depending on the provided Access Conditions, this method will return
         * which key is required to achieve the operation ([Operation]).
         */
        @JvmStatic
        fun getOperationRequirements(
            c1: Byte,
            c2: Byte,
            c3: Byte,
            op: Operation,
            isSectorTrailer: Boolean,
            isKeyBReadable: Boolean
        ): Int {
            val b1 = c1.toInt()
            val b2 = c2.toInt()
            val b3 = c3.toInt()
            if (isSectorTrailer) {
                if (op != Operation.ReadKeyA && op != Operation.ReadKeyB
                    && op != Operation.ReadAC
                    && op != Operation.WriteKeyA
                    && op != Operation.WriteKeyB
                    && op != Operation.WriteAC
                ) {
                    return 4
                }
                return if (b1 == 0 && b2 == 0 && b3 == 0) {
                    if (op == Operation.WriteKeyA || op == Operation.WriteKeyB
                        || op == Operation.ReadKeyB || op == Operation.ReadAC
                    ) {
                        1
                    } else {
                        0
                    }
                } else if (b1 == 0 && b2 == 1 && b3 == 0) {
                    if (op == Operation.ReadKeyB || op == Operation.ReadAC) {
                        1
                    } else {
                        0
                    }
                } else if (b1 == 1 && b2 == 0 && b3 == 0) {
                    if (op == Operation.WriteKeyA || op == Operation.WriteKeyB) {
                        2
                    } else if (op == Operation.ReadAC) {
                        3
                    } else {
                        0
                    }
                } else if (b1 == 1 && b2 == 1 && b3 == 0) {
                    if (op == Operation.ReadAC) {
                        3
                    } else {
                        0
                    }
                } else if (b1 == 0 && b2 == 0 && b3 == 1) {
                    if (op == Operation.ReadKeyA) {
                        0
                    } else {
                        1
                    }
                } else if (b1 == 0 && b2 == 1 && b3 == 1) {
                    if (op == Operation.ReadAC) {
                        3
                    } else if (op == Operation.ReadKeyA || op == Operation.ReadKeyB) {
                        0
                    } else {
                        2
                    }
                } else if (b1 == 1 && b2 == 0 && b3 == 1) {
                    if (op == Operation.ReadAC) {
                        3
                    } else if (op == Operation.WriteAC) {
                        2
                    } else {
                        0
                    }
                } else if (b1 == 1 && b2 == 1 && b3 == 1) {
                    if (op == Operation.ReadAC) {
                        3
                    } else {
                        0
                    }
                } else {
                    -1
                }
            } else {
                if (op != Operation.Read && op != Operation.Write
                    && op != Operation.Increment
                    && op != Operation.DecTransRest
                ) {
                    return -1
                }
                return if (b1 == 0 && b2 == 0 && b3 == 0) {
                    if (isKeyBReadable) 1 else 3
                } else if (b1 == 0 && b2 == 1 && b3 == 0) {
                    if (op == Operation.Read) {
                        if (isKeyBReadable) 1 else 3
                    } else {
                        0
                    }
                } else if (b1 == 1 && b2 == 0 && b3 == 0) {
                    if (op == Operation.Read) {
                        if (isKeyBReadable) 1 else 3
                    } else if (op == Operation.Write) {
                        2
                    } else {
                        0
                    }
                } else if (b1 == 1 && b2 == 1 && b3 == 0) {
                    if (op == Operation.Read || op == Operation.DecTransRest) {
                        if (isKeyBReadable) 1 else 3
                    } else {
                        2
                    }
                } else if (b1 == 0 && b2 == 0 && b3 == 1) {
                    if (op == Operation.Read || op == Operation.DecTransRest) {
                        if (isKeyBReadable) 1 else 3
                    } else {
                        0
                    }
                } else if (b1 == 0 && b2 == 1 && b3 == 1) {
                    if (op == Operation.Read || op == Operation.Write) {
                        2
                    } else {
                        0
                    }
                } else if (b1 == 1 && b2 == 0 && b3 == 1) {
                    if (op == Operation.Read) {
                        2
                    } else {
                        0
                    }
                } else if (b1 == 1 && b2 == 1 && b3 == 1) {
                    0
                } else {
                    -1
                }
            }
        }

        /**
         * Check if key B is readable.
         */
        @JvmStatic
        fun isKeyBReadable(c1: Byte, c2: Byte, c3: Byte): Boolean {
            val b1 = c1.toInt()
            val b2 = c2.toInt()
            val b3 = c3.toInt()
            return b1 == 0 && ((b2 == 0 && b3 == 0) || (b2 == 1 && b3 == 0) || (b2 == 0 && b3 == 1))
        }

        /**
         * Convert the Access Condition bytes to a matrix containing the
         * resolved C1, C2 and C3 for each block.
         */
        @JvmStatic
        fun acBytesToACMatrix(acBytes: ByteArray?): Array<ByteArray>? {
            if (acBytes == null || acBytes.size < 3) {
                return null
            }
            val b0 = acBytes[0].toInt() and 0xFF
            val b1 = acBytes[1].toInt() and 0xFF
            val b2 = acBytes[2].toInt() and 0xFF

            if (((b1 ushr 4) and 0x0F) == ((b0 xor 0xFF) and 0x0F) &&
                (b2 and 0x0F) == (((b0 xor 0xFF) ushr 4) and 0x0F) &&
                ((b2 ushr 4) and 0x0F) == ((b1 xor 0xFF) and 0x0F)
            ) {
                val acMatrix = Array(3) { ByteArray(4) }
                for (i in 0 until 4) {
                    acMatrix[0][i] = ((b1 ushr (4 + i)) and 0x01).toByte()
                }
                for (i in 0 until 4) {
                    acMatrix[1][i] = ((b2 ushr i) and 0x01).toByte()
                }
                for (i in 0 until 4) {
                    acMatrix[2][i] = ((b2 ushr (4 + i)) and 0x01).toByte()
                }
                return acMatrix
            }
            return null
        }

        /**
         * Convert a matrix with Access Conditions bits into normal 3
         * Access Condition bytes.
         */
        @JvmStatic
        fun acMatrixToACBytes(acMatrix: Array<ByteArray>?): ByteArray? {
            if (acMatrix == null || acMatrix.size != 3) {
                return null
            }
            for (i in 0 until 3) {
                if (acMatrix[i].size != 4) {
                    return null
                }
            }
            val acBytes = ByteArray(3)
            val m0 = acMatrix[0]
            val m1 = acMatrix[1]
            val m2 = acMatrix[2]

            // Byte 6, Bit 0-3.
            var b0 = (m0[0].toInt() xor 0xFF) and 0x01
            b0 = b0 or (((m0[1].toInt() xor 0xFF) shl 1) and 0x02)
            b0 = b0 or (((m0[2].toInt() xor 0xFF) shl 2) and 0x04)
            b0 = b0 or (((m0[3].toInt() xor 0xFF) shl 3) and 0x08)
            // Byte 6, Bit 4-7.
            b0 = b0 or (((m1[0].toInt() xor 0xFF) shl 4) and 0x10)
            b0 = b0 or (((m1[1].toInt() xor 0xFF) shl 5) and 0x20)
            b0 = b0 or (((m1[2].toInt() xor 0xFF) shl 6) and 0x40)
            b0 = b0 or (((m1[3].toInt() xor 0xFF) shl 7) and 0x80)
            acBytes[0] = b0.toByte()

            // Byte 7, Bit 0-3.
            var b1 = (m2[0].toInt() xor 0xFF) and 0x01
            b1 = b1 or (((m2[1].toInt() xor 0xFF) shl 1) and 0x02)
            b1 = b1 or (((m2[2].toInt() xor 0xFF) shl 2) and 0x04)
            b1 = b1 or (((m2[3].toInt() xor 0xFF) shl 3) and 0x08)
            // Byte 7, Bit 4-7.
            b1 = b1 or ((m0[0].toInt() shl 4) and 0x10)
            b1 = b1 or ((m0[1].toInt() shl 5) and 0x20)
            b1 = b1 or ((m0[2].toInt() shl 6) and 0x40)
            b1 = b1 or ((m0[3].toInt() shl 7) and 0x80)
            acBytes[1] = b1.toByte()

            // Byte 8, Bit 0-3.
            var b2 = m1[0].toInt() and 0x01
            b2 = b2 or ((m1[1].toInt() shl 1) and 0x02)
            b2 = b2 or ((m1[2].toInt() shl 2) and 0x04)
            b2 = b2 or ((m1[3].toInt() shl 3) and 0x08)
            // Byte 8, Bit 4-7.
            b2 = b2 or ((m2[0].toInt() shl 4) and 0x10)
            b2 = b2 or ((m2[1].toInt() shl 5) and 0x20)
            b2 = b2 or ((m2[2].toInt() shl 6) and 0x40)
            b2 = b2 or ((m2[3].toInt() shl 7) and 0x80)
            acBytes[2] = b2.toByte()

            return acBytes
        }

        /**
         * Check if a (hex) string is pure hex (0-9, A-F, a-f) and 16 byte
         * (32 chars) long. If not show an error Toast in the context.
         */
        @JvmStatic
        fun isHexAnd16Byte(hexString: String, context: Context): Boolean {
            val isHex = isHex(hexString, context)
            if (!isHex) {
                return false
            }
            if (hexString.length != 32) {
                Toast.makeText(context, R.string.info_not_16_byte, Toast.LENGTH_LONG).show()
                return false
            }
            return true
        }

        /**
         * Check if a (hex) string is pure hex (0-9, A-F, a-f).
         * If not show an error Toast in the context.
         */
        @JvmStatic
        fun isHex(hex: String?, context: Context): Boolean {
            if (!(hex != null && hex.length % 2 == 0 && hex.matches(Regex("[0-9A-Fa-f]+")))) {
                Toast.makeText(context, R.string.info_not_hex_data, Toast.LENGTH_LONG).show()
                return false
            }
            return true
        }

        /**
         * Check if the given block (hex string) is a value block.
         */
        @JvmStatic
        fun isValueBlock(hexString: String?): Boolean {
            if (hexString == null) return false
            val b = hex2Bytes(hexString)
            if (b != null && b.size == 16) {
                return (b[0] == b[8] && (b[0].toInt() xor 0xFF).toByte() == b[4]) &&
                        (b[1] == b[9] && (b[1].toInt() xor 0xFF).toByte() == b[5]) &&
                        (b[2] == b[10] && (b[2].toInt() xor 0xFF).toByte() == b[6]) &&
                        (b[3] == b[11] && (b[3].toInt() xor 0xFF).toByte() == b[7]) &&
                        (b[12] == b[14] && b[13] == b[15] && (b[12].toInt() xor 0xFF).toByte() == b[13])
            }
            return false
        }

        /**
         * Check if all blocks (lines) contain valid data.
         */
        @JvmStatic
        fun isValidDump(lines: Array<String>?, ignoreAsterisk: Boolean): Int {
            val knownSectors = ArrayList<Int>()
            var blocksSinceLastSectorHeader = 4
            var is16BlockSector = false
            if (lines == null || lines.isEmpty()) {
                return 6
            }
            for (line in lines) {
                if ((!is16BlockSector && blocksSinceLastSectorHeader == 4)
                    || (is16BlockSector && blocksSinceLastSectorHeader == 16)
                ) {
                    if (!line.matches(Regex("^\\+Sector: [0-9]{1,2}$"))) {
                        return 1
                    }
                    val sector: Int
                    try {
                        sector = line.split(": ")[1].toInt()
                    } catch (ex: Exception) {
                        return 1
                    }
                    if (sector < 0 || sector > 39) {
                        return 4
                    }
                    if (knownSectors.contains(sector)) {
                        return 5
                    }
                    knownSectors.add(sector)
                    is16BlockSector = (sector >= 32)
                    blocksSinceLastSectorHeader = 0
                    continue
                }
                if (line.startsWith("*") && ignoreAsterisk) {
                    is16BlockSector = false
                    blocksSinceLastSectorHeader = 4
                    continue
                }
                if (!line.matches(Regex("[0-9A-Fa-f-]+"))) {
                    return 2
                }
                if (line.length != 32) {
                    return 3
                }
                blocksSinceLastSectorHeader++
            }
            return 0
        }

        /**
         * Check if the user input is a valid key file.
         */
        @JvmStatic
        fun isValidKeyFile(lines: Array<String>?): Int {
            var keyFound = false
            if (lines == null || lines.isEmpty()) {
                return 1
            }
            for (rawLine in lines) {
                var line = rawLine
                if (line.startsWith("#")) {
                    continue
                }
                line = line.split("#")[0].trim()
                if (line.isEmpty()) {
                    continue
                }
                if (!line.matches(Regex("[0-9A-Fa-f]+"))) {
                    return 2
                }
                if (line.length != 12) {
                    return 3
                }
                keyFound = true
            }

            if (!keyFound) {
                return 1
            }
            return 0
        }

        /**
         * Show a Toast message with error information according to [isValidDump].
         */
        @JvmStatic
        fun isValidDumpErrorToast(errorCode: Int, context: Context) {
            when (errorCode) {
                1 -> Toast.makeText(context, R.string.info_valid_dump_not_4_or_16_lines, Toast.LENGTH_LONG).show()
                2 -> Toast.makeText(context, R.string.info_valid_dump_not_hex, Toast.LENGTH_LONG).show()
                3 -> Toast.makeText(context, R.string.info_valid_dump_not_16_bytes, Toast.LENGTH_LONG).show()
                4 -> Toast.makeText(context, R.string.info_valid_dump_sector_range, Toast.LENGTH_LONG).show()
                5 -> Toast.makeText(context, R.string.info_valid_dump_double_sector, Toast.LENGTH_LONG).show()
                6 -> Toast.makeText(context, R.string.info_valid_dump_empty_dump, Toast.LENGTH_LONG).show()
            }
        }

        /**
         * Show a Toast message with error information according to [isValidKeyFile].
         */
        @JvmStatic
        fun isValidKeyFileErrorToast(errorCode: Int, context: Context): Boolean {
            when (errorCode) {
                0 -> return true
                1 -> Toast.makeText(context, R.string.info_valid_keys_no_keys, Toast.LENGTH_LONG).show()
                2 -> Toast.makeText(context, R.string.info_valid_keys_not_hex, Toast.LENGTH_LONG).show()
                3 -> Toast.makeText(context, R.string.info_valid_keys_not_6_byte, Toast.LENGTH_LONG).show()
            }
            return false
        }

        /**
         * Check if a block 0 contains valid data.
         */
        @JvmStatic
        fun isValidBlock0(block0: String?, uidLen: Int, tagSize: Int, skipBccCheck: Boolean): Boolean {
            if (block0 == null || block0.length != 32 || (uidLen != 4 && uidLen != 7 && uidLen != 10)) {
                return false
            }
            val upperBlock0 = block0.uppercase(Locale.ROOT)
            val byte0 = upperBlock0.substring(0, 2)
            val bcc = upperBlock0.substring(8, 10)
            val sakStart = if (uidLen == 4) uidLen * 2 + 2 else uidLen * 2
            val sak = upperBlock0.substring(sakStart, sakStart + 2)
            val atqa = upperBlock0.substring(sakStart + 2, sakStart + 6)
            var valid = true

            // BCC.
            if (!skipBccCheck && valid && uidLen == 4) {
                val byteBcc = hex2Bytes(bcc)!![0]
                val uid = hex2Bytes(upperBlock0.substring(0, 8))!!
                valid = isValidBcc(uid, byteBcc)
            }
            // Byte0.
            if (valid && uidLen == 4) {
                valid = byte0 != "88"
            }
            if (valid && uidLen == 4) {
                valid = byte0 != "F8"
            }
            if (valid && (uidLen == 7 || uidLen == 10)) {
                val firstByte = hex2Bytes(byte0)!![0].toInt() and 0xFF
                valid = (firstByte < 0x81 || firstByte > 0xFE)
            }
            if (valid && (uidLen == 7 || uidLen == 10)) {
                valid = byte0 != "00"
            }
            // Byte3.
            if (valid && (uidLen == 7 || uidLen == 10)) {
                valid = !upperBlock0.startsWith("88", 4)
            }
            // ATQA.
            if (valid && (atqa.matches(Regex("040[1-9A-F]")) ||
                        atqa.matches(Regex("020[1-9A-F]")) ||
                        atqa.matches(Regex("480.")) ||
                        atqa.matches(Regex("010F")))
            ) {
                // Special ATQA value found.
            } else if (valid) {
                if (uidLen == 4 && (tagSize == MifareClassic.SIZE_1K ||
                            tagSize == MifareClassic.SIZE_2K ||
                            tagSize == MifareClassic.SIZE_MINI)
                ) {
                    valid = atqa == "0400"
                } else if (uidLen == 4 && tagSize == MifareClassic.SIZE_4K) {
                    valid = atqa == "0200"
                } else if (uidLen == 7 && (tagSize == MifareClassic.SIZE_1K ||
                            tagSize == MifareClassic.SIZE_2K ||
                            tagSize == MifareClassic.SIZE_MINI)
                ) {
                    valid = atqa == "4400"
                } else if (uidLen == 7 && tagSize == MifareClassic.SIZE_4K) {
                    valid = atqa == "4200"
                }
            }
            // SAK.
            val byteSak = hex2Bytes(sak)!![0].toInt() and 0xFF
            var validSak = false
            if (valid) {
                if (((byteSak shr 1) and 1) == 0) {
                    if (((byteSak shr 3) and 1) == 1) {
                        if (((byteSak shr 4) and 1) == 1) {
                            validSak = (tagSize == MifareClassic.SIZE_2K || tagSize == MifareClassic.SIZE_4K)
                        } else {
                            if ((byteSak and 1) == 1) {
                                validSak = tagSize == MifareClassic.SIZE_MINI
                            } else {
                                validSak = (tagSize == MifareClassic.SIZE_2K || tagSize == MifareClassic.SIZE_1K)
                            }
                        }
                    }
                }
            }
            valid = validSak
            return valid
        }

        /**
         * Reverse a byte Array (e.g. Little Endian -> Big Endian) in-place.
         */
        @JvmStatic
        fun reverseByteArrayInPlace(array: ByteArray) {
            val half = array.size / 2
            for (i in 0 until half) {
                val temp = array[i]
                array[i] = array[array.size - i - 1]
                array[array.size - i - 1] = temp
            }
        }

        /**
         * Convert byte array to a string of the specified format.
         */
        @JvmStatic
        fun byte2FmtString(bytes: ByteArray, fmt: Int): String {
            when (fmt) {
                2 -> {
                    val revBytes = bytes.clone()
                    reverseByteArrayInPlace(revBytes)
                    return hex2DecBE(bytes2Hex(revBytes)) ?: ""
                }
                1 -> return hex2DecBE(bytes2Hex(bytes)) ?: ""
            }
            return bytes2Hex(bytes)
        }

        /**
         * Convert a hexadecimal string to a decimal string.
         */
        @JvmStatic
        fun hex2DecBE(hex: String?): String? {
            if (!(hex != null && hex.length % 2 == 0 && hex.matches(Regex("[0-9A-Fa-f]+")))) {
                return null
            }
            return if (hex.isEmpty()) {
                "0"
            } else if (hex.length <= 14) {
                hex.toLong(16).toString()
            } else {
                BigInteger(hex, 16).toString()
            }
        }

        /**
         * Convert a little endian hexadecimal string to a decimal string.
         */
        @JvmStatic
        fun hex2DecLE(hex: String?): String? {
            if (!(hex != null && hex.length % 2 == 0 && hex.matches(Regex("[0-9A-Fa-f]+")))) {
                return null
            }
            val reversedHex = StringBuilder(hex.length)
            var i = hex.length - 2
            while (i >= 0) {
                reversedHex.append(hex.substring(i, i + 2))
                i -= 2
            }
            val leHex = reversedHex.toString()
            return if (leHex.isEmpty()) {
                "0"
            } else if (leHex.length <= 14) {
                leHex.toLong(16).toString()
            } else {
                BigInteger(leHex, 16).toString()
            }
        }

        /**
         * Convert an array of bytes into a string of hex values.
         */
        @JvmStatic
        fun bytes2Hex(bytes: ByteArray?): String {
            val ret = StringBuilder()
            if (bytes != null) {
                for (b in bytes) {
                    ret.append(String.format("%02X", b.toInt() and 0xFF))
                }
            }
            return ret.toString()
        }

        /**
         * Convert a string of hex data into a byte array.
         */
        @JvmStatic
        fun hex2Bytes(hex: String?): ByteArray? {
            if (!(hex != null && hex.length % 2 == 0 && hex.matches(Regex("[0-9A-Fa-f]+")))) {
                return null
            }
            val len = hex.length
            val data = ByteArray(len / 2)
            try {
                var i = 0
                while (i < len) {
                    data[i / 2] = ((Character.digit(hex[i], 16) shl 4) +
                            Character.digit(hex[i + 1], 16)).toByte()
                    i += 2
                }
            } catch (e: Exception) {
                Log.d(LOG_TAG, "Argument(s) for hexStringToByteArray(String s) was not a hex string")
            }
            return data
        }

        /**
         * Convert a hex string to ASCII string.
         */
        @JvmStatic
        fun hex2Ascii(hex: String?): String? {
            if (!(hex != null && hex.length % 2 == 0 && hex.matches(Regex("[0-9A-Fa-f]+")))) {
                return null
            }
            val bytes = hex2Bytes(hex) ?: return null
            for (i in bytes.indices) {
                if (bytes[i] < 0x20.toByte() || bytes[i] == 0x7F.toByte()) {
                    bytes[i] = 0x2E.toByte()
                }
            }
            return String(bytes, StandardCharsets.US_ASCII)
        }

        /**
         * Convert an ASCII string to a hex string.
         */
        @JvmStatic
        fun ascii2Hex(ascii: String?): String? {
            if (ascii.isNullOrEmpty()) {
                return null
            }
            val chars = ascii.toCharArray()
            val hex = StringBuilder()
            for (aChar in chars) {
                hex.append(String.format("%02X", aChar.code))
            }
            return hex.toString()
        }

        /**
         * Convert a hex string to a binary string (with leading zeros).
         */
        @JvmStatic
        fun hex2Bin(hex: String?): String? {
            if (!(hex != null && hex.length % 2 == 0 && hex.matches(Regex("[0-9A-Fa-f]+")))) {
                return null
            }
            var bin = BigInteger(hex, 16).toString(2)
            if (bin.length < hex.length * 4) {
                val diff = hex.length * 4 - bin.length
                val pad = StringBuilder()
                for (i in 0 until diff) {
                    pad.append("0")
                }
                pad.append(bin)
                bin = pad.toString()
            }
            return bin
        }

        /**
         * Convert a binary string to a hex string.
         */
        @JvmStatic
        fun bin2Hex(bin: String?): String? {
            if (!(bin != null && bin.length % 8 == 0 && bin.matches(Regex("[0-1]+")))) {
                return null
            }
            var hex = BigInteger(bin, 2).toString(16)
            if (hex.length % 2 != 0) {
                hex = "0$hex"
            }
            return hex
        }

        /**
         * Convert a decimal string to a hex string.
         */
        @JvmStatic
        fun decBE2Hex(dec: String?): String? {
            if (dec.isNullOrEmpty() || !dec.matches(Regex("[0-9]+"))) {
                return null
            }
            var hex: String = if (dec.length <= 18) {
                dec.toLong(10).toString(16).uppercase(Locale.ROOT)
            } else {
                BigInteger(dec, 10).toString(16).uppercase(Locale.ROOT)
            }
            if (hex.length % 2 != 0) {
                hex = "0$hex"
            }
            return hex
        }

        /**
         * Convert a decimal string to a little endian hex string.
         */
        @JvmStatic
        fun decLE2Hex(dec: String?): String? {
            if (dec.isNullOrEmpty() || !dec.matches(Regex("[0-9]+"))) {
                return null
            }
            var hex: String = if (dec.length <= 18) {
                dec.toLong(10).toString(16).uppercase(Locale.ROOT)
            } else {
                BigInteger(dec, 10).toString(16).uppercase(Locale.ROOT)
            }
            if (hex.length % 2 != 0) {
                hex = "0$hex"
            }
            val reversedHex = StringBuilder(hex.length)
            var i = hex.length - 2
            while (i >= 0) {
                reversedHex.append(hex.substring(i, i + 2))
                i -= 2
            }
            return reversedHex.toString()
        }

        /**
         * Create a colored string.
         */
        @JvmStatic
        fun colorString(data: String, color: Int): SpannableString {
            val ret = SpannableString(data)
            ret.setSpan(ForegroundColorSpan(color), 0, data.length, 0)
            return ret
        }

        /**
         * Get the current theme's accent color.
         */
        @JvmStatic
        fun getThemeAccentColor(context: Context): Int {
            val value = TypedValue()
            context.theme.resolveAttribute(androidx.appcompat.R.attr.colorAccent, value, true)
            return value.data
        }

        /**
         * Copy a text to the Android clipboard.
         */
        @JvmStatic
        fun copyToClipboard(text: String, context: Context, showMsg: Boolean) {
            if (text.isNotEmpty()) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("MIFARE Classic Tool data", text)
                clipboard.setPrimaryClip(clip)
                if (showMsg) {
                    Toast.makeText(context, R.string.info_copied_to_clipboard, Toast.LENGTH_SHORT).show()
                }
            }
        }

        /**
         * Get the content of the Android clipboard (if it is plain text).
         */
        @JvmStatic
        fun getFromClipboard(context: Context): String? {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val primaryClip = clipboard.primaryClip
            val primaryDescription = clipboard.primaryClipDescription
            if (primaryClip != null && primaryClip.itemCount > 0 &&
                primaryDescription != null && primaryDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) &&
                primaryClip.getItemAt(0) != null && primaryClip.getItemAt(0).text != null
            ) {
                return primaryClip.getItemAt(0).text.toString()
            }
            return null
        }

        /**
         * Share a file from the "tmp" directory as attachment.
         */
        @JvmStatic
        fun shareTextFile(context: Context, file: File) {
            val intent = Intent(Intent.ACTION_SEND)
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            val uri: Uri = try {
                FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
            } catch (ex: IllegalArgumentException) {
                Toast.makeText(context, R.string.info_share_error, Toast.LENGTH_SHORT).show()
                return
            }
            intent.setDataAndType(uri, "text/plain")
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            context.startActivity(Intent.createChooser(intent, context.getText(R.string.dialog_share_title)))
        }

        /**
         * Copy file.
         */
        @JvmStatic
        @Throws(IOException::class)
        fun copyFile(`in`: InputStream, out: OutputStream) {
            val buffer = ByteArray(1024)
            var read: Int
            while (`in`.read(buffer).also { read = it } != -1) {
                out.write(buffer, 0, read)
            }
        }

        /**
         * Convert Dips to pixels.
         */
        @JvmStatic
        fun dpToPx(dp: Int): Int {
            return (dp * mScale + 0.5f).toInt()
        }

        /**
         * Get the current active (last detected) Tag.
         */
        @JvmStatic
        fun getTag(): Tag? {
            return mTag
        }

        /**
         * Set the new active Tag (and update [mUID]).
         */
        @JvmStatic
        fun setTag(tag: Tag?) {
            mTag = tag
            mUID = tag?.id
        }

        /**
         * Get the App wide used NFC adapter.
         */
        @JvmStatic
        fun getNfcAdapter(): NfcAdapter? {
            return mNfcAdapter
        }

        /**
         * Set the App wide used NFC adapter.
         */
        @JvmStatic
        fun setNfcAdapter(nfcAdapter: NfcAdapter?) {
            mNfcAdapter = nfcAdapter
        }

        /**
         * Remember the choice whether to use MCT in editor only mode or not.
         */
        @JvmStatic
        fun setUseAsEditorOnly(value: Boolean) {
            mUseAsEditorOnly = value
        }

        /**
         * If NFC is disabled and the user chose to use MCT in editor only mode,
         * this method will return true.
         */
        @JvmStatic
        fun useAsEditorOnly(): Boolean {
            return mUseAsEditorOnly
        }

        /**
         * Get the key map generated by [de.syss.MifareClassicTool.Activities.KeyMapCreator].
         */
        @JvmStatic
        fun getKeyMap(): SparseArray<Array<ByteArray?>>? {
            return mKeyMap
        }

        /**
         * Set the key map.
         */
        @JvmStatic
        fun setKeyMap(value: SparseArray<Array<ByteArray?>>?) {
            mKeyMap = value
        }

        /**
         * Set [mKeyMapFrom] and [mKeyMapTo].
         */
        @JvmStatic
        fun setKeyMapRange(from: Int, to: Int) {
            mKeyMapFrom = from
            mKeyMapTo = to
        }

        /**
         * Get the key map start point.
         */
        @JvmStatic
        fun getKeyMapRangeFrom(): Int {
            return mKeyMapFrom
        }

        /**
         * Get the key map end point.
         */
        @JvmStatic
        fun getKeyMapRangeTo(): Int {
            return mKeyMapTo
        }

        /**
         * Set the component name of a new pending activity.
         */
        @JvmStatic
        fun setPendingComponentName(pendingActivity: ComponentName?) {
            mPendingComponentName = pendingActivity
        }

        /**
         * Get the component name of the current pending activity.
         */
        @JvmStatic
        fun getPendingComponentName(): ComponentName? {
            return mPendingComponentName
        }

        /**
         * Get the UID of the current tag.
         */
        @JvmStatic
        fun getUID(): ByteArray? {
            return mUID
        }

        /**
         * Check whether the provided BCC is valid for the UID or not.
         */
        @JvmStatic
        fun isValidBcc(uid: ByteArray, bcc: Byte): Boolean {
            return calcBcc(uid) == bcc
        }

        /**
         * Calculate the BCC of a 4 byte UID.
         */
        @JvmStatic
        @Throws(IllegalArgumentException::class)
        fun calcBcc(uid: ByteArray): Byte {
            if (uid.size != 4) {
                throw IllegalArgumentException("UID length is not 4 bytes.")
            }
            var bcc = uid[0]
            for (i in 1 until uid.size) {
                bcc = (bcc.toInt() xor uid[i].toInt()).toByte()
            }
            return bcc
        }

        /**
         * Get the version code.
         */
        @JvmStatic
        fun getVersionCode(): String? {
            return mVersionCode
        }
    }
}
