package jp.daisen_solution.printsampleapp.print

import android.app.Activity
import android.util.Log
import jp.co.toshibatec.bcp.library.BCPControl
import jp.co.toshibatec.bcp.library.LongRef
import jp.co.toshibatec.bcp.library.StringRef
import jp.daisen_solution.printsampleapp.R

class PrintExecuteTask(context: Activity?, bcpControl: BCPControl?, printData: PrintData?) {

    private var mContext = context
    private var mBcpControl = bcpControl
    private var mPrintData = printData

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // 印刷処理　（非同期）
    ////////////////////////////////////////////////////////////////////////////////////////////////
    suspend fun print(): String {

        Log.i("print","印刷処理：開始")
        mPrintData!!.result = 0
        mPrintData!!.statusMessage = ""

        // Load lfm file
        Log.i("print", "--------loadLfmFile start")
        var result = LongRef(0)
        Log.v("print",mPrintData!!.lfmFileFullPath)
        if (!mBcpControl!!.LoadLfmFile(mPrintData!!.lfmFileFullPath, result)) {
            Log.v("print",String.format("loadLfmFile Error = %08x %s", result.longValue, mPrintData!!.lfmFileFullPath))
            return String.format("loadLfmFile Error = %08x %s", result.longValue, mPrintData!!.lfmFileFullPath)
        }

        // set object
        Log.i("print", "--------setObjectDataEX start")
        // 全てのキー値を取得
        val keySet: Set<*> = mPrintData!!.objectDataList!!.keys
        val keyIte = keySet.iterator()
        // ループ。反復子iteratorによるキー取得
        while (keyIte.hasNext()) {
            val key = keyIte.next() as String
            if (!mBcpControl!!.SetObjectDataEx(key, mPrintData!!.objectDataList!![key], result)
            ) {
                Log.v("print",String.format("setObjectDataEX Error = %08x", result.longValue))
                return String.format("setObjectDataEX Error = %08x", result.longValue)
            }
        }

        // print
        val printerStatus = StringRef("")
        val cutInterval = 10 // 10msec
        Log.i("print", "--------issue start")
        if (!mBcpControl!!.Issue(mPrintData!!.printCount, cutInterval, printerStatus, result)) {
            mPrintData!!.result = result.longValue
            val message = StringRef("")
            if (result.longValue == 0x800A044EL) {     // プリンタからステータスを受信
                val errCode = printerStatus.getStringValue().substring(0, 2)
                mBcpControl!!.GetMessage(errCode, message)
            } else {
                if (!mBcpControl!!.GetMessage(result.longValue, message)) {
                    message.setStringValue(String.format("executePrint Error = %08x", result.longValue))
                }
            }
            mPrintData!!.statusMessage = message.getStringValue()

            // リトライ可能エラー有無の確認
            return if (mBcpControl!!.IsIssueRetryError()) {
                mContext!!.getString(R.string.msg_RetryError)
            } else {
                mContext!!.getString(R.string.error)
            }
        } else {
            mPrintData!!.result = result.longValue
            mPrintData!!.statusMessage = mContext!!.getString(R.string.msg_success)
            return mContext!!.getString(R.string.msg_success)
        }

    }



}