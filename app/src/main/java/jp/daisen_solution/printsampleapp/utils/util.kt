package jp.daisen_solution.printsampleapp.utils

import android.content.Context
import androidx.appcompat.app.AlertDialog
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream

object util {


    fun showAlertDialog(context: Context, strMessage: String?) {
        val Alertbuilder = AlertDialog.Builder(context)
        Alertbuilder.setMessage(strMessage)
        Alertbuilder.setCancelable(false)
        Alertbuilder.setPositiveButton("YES", null)
        val alertDialog = Alertbuilder.create()
        alertDialog.show()
    }

    // assetリソースのファイル保存
    fun asset2file (context: Context, inputFileName: String?, folderName: String, outputFileName: String) {

        val assets = context.resources.assets
        var fis: InputStream? = null
        var fos: FileOutputStream? = null
        var size: Int
        val ba = ByteArray(1024)

        try {
            fis = assets.open(inputFileName!!)
            val fileFullPath = "$folderName/$outputFileName"
            fos = FileOutputStream(fileFullPath)
            while (true) {
                size = fis.read(ba)
                if (size <= 0) break
                fos.write(ba, 0, size)
            }
            fos.close()
            fis.close()
        } catch (e: FileNotFoundException) {
            try {
                fis?.close()
                fos?.close()
            } catch (e2: Exception) {
            }
            throw e
        } catch (e: Exception) {
            try {
                fis?.close()
                fos?.close()
            } catch (e2: Exception) {
            }
            throw e
        }
    }

}