package eu.flashsoft.automatic_data_disconnect.ui.logs


import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import eu.flashsoft.automatic_data_disconnect.R
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter


class LogsFragment : Fragment() {


    private lateinit var logsViewModel: LogsViewModel

    private lateinit var logTxtBox: TextView
    private lateinit var clearLogsBtn : Button
    private lateinit var logsProgressBar : ProgressBar
    private lateinit var errorLogsTxt: TextView
    private lateinit var errorImg: ImageView


    private fun removeLines(fileName: String, startLine: Int, numLines: Int) {
        require(fileName.isNotEmpty() && startLine >= 1 && numLines >= 1)
        val f = File(fileName)
        if (!f.exists()) {
            println("$fileName does not exist")
            return
        }
        var lines = f.readLines()
        val size = lines.size
        if (startLine > size) {
            println("The starting line is beyond the length of the file")
            return
        }
        var n = numLines
        if (startLine + numLines - 1 > size) {
            println("Attempting to remove some lines which are beyond the end of the file")
            n = size - startLine + 1
        }
        lines = lines.take(startLine - 1) + lines.drop(startLine + n - 1)
        val text = lines.joinToString(System.lineSeparator())
        f.writeText(text)
    }

    private fun asyncClearLogs(){
        clearLogsBtn.isGone = true
        logsProgressBar.isGone = false

        lifecycleScope.launch {
            whenStarted {
                withContext(Dispatchers.IO) {
                FileWriter(File(activity?.applicationContext?.filesDir, "logs.txt"), false)
                }
            }
            logsProgressBar.isGone = true
            clearLogsBtn.isGone = false
            checkLogs()
        }
    }

   private fun asyncPruneLogs(){
       lifecycleScope.launch {
           pruneLogs()
       }
   }

    private fun pruneLogs(){

        val logFile = File(activity?.applicationContext?.filesDir, "logs.txt").path.toString()
        val reader = BufferedReader(FileReader(logFile))
        var lines = 0
        while (reader.readLine() != null) lines++
        reader.close()

        if (lines > 100) {
            val difLines = lines - 100
            removeLines(logFile, 0, difLines)
        }

    }

    private fun hideLogs(){
        clearLogsBtn.isGone  = true
        logTxtBox.isGone = true
    }


    @SuppressLint("SetTextI18n")
    private fun checkLogs(){
        val sharedPrefs = activity?.applicationContext?.getSharedPreferences("app_settings", AppCompatActivity.MODE_PRIVATE)
        val boolLogsEnabled = sharedPrefs?.getBoolean("enableLogs", false)
        if(!boolLogsEnabled!!){
            hideLogs()
            errorLogsTxt.text = "Logs are Disabled"
            errorLogsTxt.isGone = false
            errorImg.isGone = false
        }



        val filename = "logs.txt"
        val file = File(activity?.applicationContext?.filesDir, filename)

        val contents = file.readText() // Read file

        if(contents == ""){
            errorLogsTxt.text = ""
            hideLogs()
            errorLogsTxt.text = "Log File is empty!\n\n      Check later."
            errorLogsTxt.isGone = false
            errorImg.isGone = false
        }
        else logTxtBox.text = fromHtml(contents, FROM_HTML_MODE_LEGACY)


    }


    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        logsViewModel =
                ViewModelProvider(this).get(LogsViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_logs, container, false)
        logTxtBox = root.findViewById(R.id.logTextView)
        clearLogsBtn = root.findViewById(R.id.clearLogsBtn)
        logsProgressBar = root.findViewById(R.id.aboutProgressBar)
        errorLogsTxt  = root.findViewById(R.id.logsErrorTxt)
        errorImg  = root.findViewById(R.id.imageView)

        logsProgressBar.isIndeterminate = true
        logsProgressBar.indeterminateDrawable.colorFilter = PorterDuffColorFilter( getColor(resources, R.color.black_overlay ,activity?.theme ), PorterDuff.Mode.MULTIPLY)

        val file = File(activity?.applicationContext?.filesDir, "logs.txt")
        if (!file.exists()) FileWriter(File(activity?.applicationContext?.filesDir, "logs.txt"), false)


        asyncPruneLogs()

        clearLogsBtn.setOnClickListener  {
            it.isEnabled = false
            it.isClickable = false
            asyncClearLogs()
            it.isEnabled = true
            it.isClickable = true
        }

        checkLogs()




        /*
       val textView: TextView = root.findViewById(R.id.text_dashboard)
       dashboardViewModel.text.observe(viewLifecycleOwner, Observer {
           textView.text = it
       })
       root.findViewById<Switch>(R.id.switch1).setOnCheckedChangeListener { _, isChecked ->
           val da = if (isChecked) "DA" else "NU"
           Toast.makeText(activity, da as CharSequence, Toast.LENGTH_SHORT).show()
       }*/


        return root
    }
}