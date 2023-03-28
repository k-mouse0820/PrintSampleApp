package jp.daisen_solution.printsampleapp.print

import android.app.Activity
import android.app.Dialog
import androidx.appcompat.app.AlertDialog
import jp.co.toshibatec.bcp.library.BCPControl
import jp.daisen_solution.printsampleapp.R

class PrintDialogDelegate (val mActivity: Activity, val mBcpControl: BCPControl, val mPrintData: PrintData?) {

    companion object {
        const val RETRYERRORMESSAGE_DIALOG = 1 // retry error messsage dialog
        const val PRINT_COMPLETEMESSAGE_DIALOG = 2 // print finish messsage dialog
        const val ERRORMESSAGE_DIALOG = 3 // error message dialog
    }

    fun prepareDialog(id: Int, dialog: Dialog): Boolean {
        return when (id) {
            RETRYERRORMESSAGE_DIALOG -> {
                (dialog as AlertDialog).setMessage(getDialogMessage(mPrintData))
                true
            }
            else -> false
        }
    }
    fun createDialog(id: Int): Dialog? {
        return when (id) {
            RETRYERRORMESSAGE_DIALOG -> createRetryErrormessageDialog()
            PRINT_COMPLETEMESSAGE_DIALOG -> createPrintCompletemessageDialog()
            ERRORMESSAGE_DIALOG -> createErrormessageDialog()
            else -> null
        }
    }

    private fun createRetryErrormessageDialog(): Dialog {
        val bd = AlertDialog.Builder(mActivity)
        bd.setTitle(R.string.error)
        bd.setIcon(android.R.drawable.ic_dialog_alert)
        bd.setMessage(getDialogMessage(mPrintData))
        return bd.create()
    }
    private fun createPrintCompletemessageDialog(): Dialog {
        val bd = android.app.AlertDialog.Builder(mActivity)
        bd.setTitle(R.string.processComplete)
        bd.setMessage(mPrintData!!.statusMessage)
        bd.setPositiveButton(
            R.string.msg_Ok
        ) { _, _ ->
            mActivity.dismissDialog(PRINT_COMPLETEMESSAGE_DIALOG)
        }
        return bd.create()
    }
    private fun createErrormessageDialog(): Dialog {
        val bd = android.app.AlertDialog.Builder(mActivity)
        bd.setTitle(R.string.error)
        bd.setMessage(mPrintData!!.statusMessage)
        bd.setPositiveButton(
            R.string.msg_Ok
        ) { _, _ -> //
            mActivity.dismissDialog(ERRORMESSAGE_DIALOG)
        }
        return bd.create()
    }





    private fun getDialogMessage(mPrintData: PrintData?): String {
        val result = mPrintData!!.result
        val sb = StringBuilder()
        // プリンタからの応答なし
        if (result == 0x800A044CL) {
            sb.insert(0, mActivity.getString(R.string.noResponseFromPrinter))
        } else if (result == 0x800A03EBL) {  // 送信タイムアウトエラー
            sb.insert(0, mActivity.getString(R.string.writeFailed))
        } else if (result == 0x13L) {        // 用紙が終了した
            sb.insert(0, mActivity.getString(R.string.paperEnd))
        } else if (result == 0x09L) {        // 用紙が終了した
            sb.insert(0, mActivity.getString(R.string.issueEndAndpaperEnd))
        } else {
            sb.insert(0, mPrintData!!.statusMessage)
        }
        return sb.toString()
    }
}