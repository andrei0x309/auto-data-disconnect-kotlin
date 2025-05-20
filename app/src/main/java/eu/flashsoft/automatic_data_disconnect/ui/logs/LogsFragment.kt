package eu.flashsoft.automatic_data_disconnect.ui.logs

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat.getColor
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import androidx.core.text.HtmlCompat.fromHtml
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope // Use lifecycleScope for coroutines tied to fragment lifecycle
import eu.flashsoft.automatic_data_disconnect.AppConstants
import eu.flashsoft.automatic_data_disconnect.R
import kotlinx.coroutines.* // Import coroutine dependencies
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException // Import IOException


class LogsFragment : Fragment() {

    // ViewModel is typically used for data that survives configuration changes
    // For simple UI state like progress/button visibility, direct management is fine.
    private lateinit var logsViewModel: LogsViewModel // Keep ViewModel if it holds other data

    private lateinit var logTxtBox: TextView
    private lateinit var clearLogsBtn : Button
    private lateinit var logsProgressBar : ProgressBar
    private lateinit var errorLogsTxt: TextView
    private lateinit var errorImg: ImageView



    /**
     * Removes a specified number of lines from a file, starting from a given line number.
     * This is a blocking file operation and should be called on a background thread (IO dispatcher).
     *
     * @param file The File object to modify.
     * @param startLine The line number to start removing from (1-based index).
     * @param numLines The number of lines to remove.
     */
    @Suppress("SameParameterValue")
    private fun removeLines(file: File, startLine: Int, numLines: Int) {
        require(startLine >= 1) { "startLine must be 1 or greater" }
        require(numLines >= 1) { "numLines must be 1 or greater" }

        if (!file.exists()) {
            Log.d(AppConstants.FRAGMENT_LOGS_TAG, "${file.path} does not exist, cannot remove lines")
            return
        }

        try {
            // Read all lines (blocking operation)
            val lines = file.readLines()
            val size = lines.size

            if (startLine > size) {
                Log.d(AppConstants.FRAGMENT_LOGS_TAG, "The starting line ($startLine) is beyond the length of the file ($size)")
                return
            }

            // Calculate the actual number of lines to remove from the starting point
            val actualNumLinesToRemove = minOf(numLines, size - startLine + 1)

            // Take lines before the start line and drop lines from the start line onwards
            val remainingLines = lines.take(startLine - 1) + lines.drop(startLine + actualNumLinesToRemove - 1)

            // Write the remaining lines back to the file (blocking operation)
            file.writeText(remainingLines.joinToString(System.lineSeparator()))

            Log.d(AppConstants.FRAGMENT_LOGS_TAG, "Removed $actualNumLinesToRemove lines from ${file.path} starting at line $startLine")

        } catch (e: IOException) {
            Log.e(AppConstants.FRAGMENT_LOGS_TAG, "Error removing lines from file: ${file.path}", e)
            // Handle the error (e.g., show a Toast or update UI)
        }
    }

    /**
     * Asynchronously clears the log file.
     * Shows progress bar and hides button during operation.
     * Uses lifecycleScope for coroutine management.
     */
    @SuppressLint("SetTextI18n")
    private fun asyncClearLogs(){
        // Update UI on the main thread immediately
        clearLogsBtn.isGone = true
        logsProgressBar.isGone = false

        // Launch a coroutine tied to the fragment's lifecycle
        lifecycleScope.launch {
            try {
                // Perform file writing on the IO dispatcher (blocking operation)
                withContext(Dispatchers.IO) {
                    val logFile = File(requireContext().applicationContext.filesDir, "logs.txt")
                    // Overwrite the file to clear its contents
                    FileWriter(logFile, false).close() // Use close() to ensure resource is freed
                    Log.d(AppConstants.FRAGMENT_LOGS_TAG, "Log file cleared.")
                }
            } catch (e: IOException) {
                Log.e(AppConstants.FRAGMENT_LOGS_TAG, "Error clearing log file", e)
                // Handle the error (e.g., show a Toast or update UI)
                withContext(Dispatchers.Main) {
                    errorLogsTxt.text = "Error clearing log file."
                    errorLogsTxt.isGone = false
                    errorImg.isGone = false
                    hideLogs() // Hide log box and clear button
                }
            } finally {
                // Code in finally block runs regardless of success or failure
                // Update UI on the main thread after IO operation
                withContext(Dispatchers.Main) {
                    logsProgressBar.isGone = true // Hide progress bar
                    clearLogsBtn.isGone = false // Show clear button
                    checkLogs() // Refresh the log display
                }
            }
        }
    }

    /**
     * Prunes old logs by removing lines if the file exceeds a certain size.
     * This is a suspend function because it performs blocking IO.
     */
    private suspend fun pruneLogs(){
        val logFile = File(requireContext().applicationContext.filesDir, "logs.txt")
        if (!logFile.exists()) {
            Log.d(AppConstants.FRAGMENT_LOGS_TAG, "Log file does not exist, nothing to prune.")
            return
        }

        try {
            // Perform file operations on the IO dispatcher
            withContext(Dispatchers.IO) {
                val reader = BufferedReader(FileReader(logFile))
                var lines = 0
                // Counting lines is a blocking operation
                while (reader.readLine() != null) lines++
                reader.close()

                if (lines > 100) { // Check if lines exceed the limit
                    val difLines = lines - 100 // Number of lines to remove from the start
                    Log.d(AppConstants.FRAGMENT_LOGS_TAG, "Log file has $lines lines, pruning $difLines oldest lines.")
                    // removeLines is also a blocking operation, call it within IO context
                    removeLines(logFile, 1, difLines)
                } else {
                    Log.d(AppConstants.FRAGMENT_LOGS_TAG, "Log file has $lines lines, no pruning needed.")
                }
            }
        } catch (e: IOException) {
            Log.e(AppConstants.FRAGMENT_LOGS_TAG, "Error pruning log file", e)
            // Handle the error (e.g., show a Toast or update UI)
        }
    }

    /**
     * Hides the log display elements.
     */
    private fun hideLogs(){
        clearLogsBtn.isGone  = true
        logTxtBox.isGone = true
        // Also hide the progress bar and show error elements
        logsProgressBar.isGone = true
        errorLogsTxt.isGone = false
        errorImg.isGone = false
    }

    /**
     * Checks the log file and updates the UI display.
     * This function launches a coroutine to perform file reading on IO.
     */
    @SuppressLint("SetTextI18n")
    private fun checkLogs(){
        // Launch a coroutine tied to the fragment's lifecycle
        lifecycleScope.launch {
            // Get shared preferences on the main thread (usually fast) or IO if preferred
            val sharedPrefs = requireContext().applicationContext.getSharedPreferences(AppConstants.PREFS_NAME, AppCompatActivity.MODE_PRIVATE)
            val boolLogsEnabled = sharedPrefs.getBoolean(AppConstants.IS_LOGS_ENABLED_KEY, false)

            if (!boolLogsEnabled) {
                // Update UI on the main thread
                withContext(Dispatchers.Main) {
                    hideLogs() // Hide log display elements
                    errorLogsTxt.text = "Logs are Disabled" // Set specific error text
                    errorLogsTxt.isGone = false // Show error text
                    errorImg.isGone = false // Show error image
                    logsProgressBar.isGone = true // Ensure progress bar is hidden
                    clearLogsBtn.isGone = true // Ensure clear button is hidden
                    logTxtBox.isGone = true // Ensure log box is hidden
                }
                return@launch // Exit the coroutine
            }

            val filename = "logs.txt"
            val file = File(requireContext().applicationContext.filesDir, filename)

            // Perform file read on IO dispatcher
            val contents = try {
                withContext(Dispatchers.IO) {
                    if (file.exists()) {
                        file.readText() // Blocking file read
                    } else {
                        "" // Return empty string if file doesn't exist
                    }
                }
            } catch (e: IOException) {
                Log.e(AppConstants.FRAGMENT_LOGS_TAG, "Error reading log file", e)
                // Handle read error, maybe show an error message in UI
                withContext(Dispatchers.Main) {
                    errorLogsTxt.text = "Error reading log file."
                    errorLogsTxt.isGone = false
                    errorImg.isGone = false
                    hideLogs() // Hide log box and clear button
                }
                return@launch // Exit the coroutine
            }

            // Update UI on the main thread
            withContext(Dispatchers.Main) {
                if (contents.isEmpty()) {
                    hideLogs() // Hide log display elements
                    errorLogsTxt.text = "Log File is empty!\n\n      Check later." // Set specific empty message
                    errorLogsTxt.isGone = false // Show error text
                    errorImg.isGone = false // Show error image
                    logsProgressBar.isGone = true // Ensure progress bar is hidden
                    clearLogsBtn.isGone = true // Ensure clear button is hidden
                    logTxtBox.isGone = true // Ensure log box is hidden
                } else {
                    logTxtBox.text = fromHtml(contents, FROM_HTML_MODE_LEGACY) // Set log content
                    // Ensure log box and clear button are visible if logs are enabled and not empty
                    logTxtBox.isGone = false
                    clearLogsBtn.isGone = false
                    errorLogsTxt.isGone = true // Hide error text if logs are displayed
                    errorImg.isGone = true // Hide error image
                    logsProgressBar.isGone = true // Ensure progress bar is hidden
                }
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {



        logsViewModel =
            ViewModelProvider(this)[LogsViewModel::class.java]
        val root = inflater.inflate(R.layout.fragment_logs, container, false)

        // Initialize UI elements using the 'root' view
        logTxtBox = root.findViewById(R.id.logTextView)
        clearLogsBtn = root.findViewById(R.id.clearLogsBtn)
        logsProgressBar = root.findViewById(R.id.aboutProgressBar) // Check if this ID is correct for logs fragment
        errorLogsTxt  = root.findViewById(R.id.logsErrorTxt)
        errorImg  = root.findViewById(R.id.imageView) // Check if this ID is correct for logs fragment


        logsProgressBar.isIndeterminate = true
        // Use requireContext() for color resource and theme
        logsProgressBar.indeterminateDrawable.colorFilter = PorterDuffColorFilter(
            getColor(requireContext().resources, R.color.black_overlay ,requireContext().theme ),
            PorterDuff.Mode.MULTIPLY
        )

        // Set initial UI state before loading logs
        logsProgressBar.isGone = false // Show progress bar
        clearLogsBtn.isGone = true // Hide clear button
        logTxtBox.isGone = true // Hide log box
        errorLogsTxt.isGone = true // Hide error text
        errorImg.isGone = true // Hide error image


        // Launch a coroutine to handle initial file setup, pruning, and display
        // This ensures blocking file operations don't happen on the main thread during onCreateView
        lifecycleScope.launch {
            // Ensure file exists and prune logs on IO dispatcher
            withContext(Dispatchers.IO) {
                val file = File(requireContext().applicationContext.filesDir, "logs.txt")
                if (!file.exists()) {
                    try {
                        // Create the file if it doesn't exist
                        FileWriter(file, false).close()
                        Log.d(AppConstants.FRAGMENT_LOGS_TAG, "Log file created.")
                    } catch (e: IOException) {
                        Log.e(AppConstants.FRAGMENT_LOGS_TAG, "Error creating log file", e)
                        // Handle error creating file if necessary
                    }
                }
                pruneLogs() // Prune logs after ensuring file exists (pruneLogs is a suspend function)
            }
            // After IO work (file creation/pruning), check and display logs on the main thread
            // checkLogs() launches its own coroutine for reading, but its UI updates are on Main
            checkLogs()
        }


        clearLogsBtn.setOnClickListener  {
            // Disable button immediately on click
            it.isEnabled = false
            it.isClickable = false
            asyncClearLogs() // This will now be fixed and re-enable the button on completion
        }

        // No initial checkLogs call needed here, it's handled in the launch block above


        return root
    }

    // onResume and onPause can be added if you need to refresh logs when the fragment becomes visible/invisible
    // override fun onResume() {
    //     super.onResume()
    //     // Optionally refresh logs when the fragment becomes visible
    //     // checkLogs() // This will launch a new coroutine to check logs
    // }

    // override fun onPause() {
    //     super.onPause()
    //     // Optionally stop log updates or clear display when fragment is paused
    // }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear references to views to prevent memory leaks
        // The lifecycleScope tied to viewLifecycleOwner will be cancelled automatically
        // when the view is destroyed, stopping any running coroutines launched with it.
        // Explicitly setting view references to null is a good practice.
        // logTxtBox = null // If using nullable vars
        // clearLogsBtn = null
        // logsProgressBar = null
        // errorLogsTxt = null
        // errorImg = null
    }
}
