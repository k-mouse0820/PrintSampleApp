package jp.daisen_solution.printsampleapp.print

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.util.Log
import android.widget.Button
import jp.co.toshibatec.bcp.library.BCPControl
import jp.co.toshibatec.bcp.library.LongRef
import jp.co.toshibatec.bcp.library.StringRef
import jp.daisen_solution.printsampleapp.Consts
import jp.daisen_solution.printsampleapp.R
import java.util.concurrent.atomic.AtomicBoolean

class OpenPortTask(context: Activity?, bcpControl: BCPControl?, connectionData: ConnectionData?) {

    private var mContext = context
    private var mBcpControl = bcpControl
    private var mConnectionData = connectionData
    private var bluetoothDeviceExtra: String = ""

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // プリンタのBluetoothポートをオープンするメソッド
    ////////////////////////////////////////////////////////////////////////////////////////////////
    suspend fun openBluetoothPort(): String {

        // プリンタのBluetoothポートをオープン
        mBcpControl!!.portSetting = mConnectionData!!.portSetting
        val result = LongRef(0)
        Log.v("openPort", "ポートオープン処理：開始")
        val resultOpen = mBcpControl!!.OpenPort(mConnectionData!!.issueMode, result)
        Log.v("openPort", "result : ${result.longValue}")
        mConnectionData!!.isOpen = AtomicBoolean(resultOpen)

        var resultMessage =""
        if (!resultOpen) {
            val message = StringRef("")
            if (!mBcpControl!!.GetMessage(result.longValue, message)) {
                resultMessage = mContext!!.getString(R.string.msg_OpenPorterror)
                Log.e("openPort",mContext!!.getString(R.string.msg_OpenPorterror))
            } else {
                resultMessage = message.getStringValue()
                Log.e("openPort", message.getStringValue())
            }
        } else {
            resultMessage = mContext!!.getString(R.string.msg_success)
            mContext!!.getSharedPreferences(Consts.bcpSectionName, Context.MODE_PRIVATE).edit()
                .putString(Consts.pairingNameKey, bluetoothDeviceExtra).apply()
            Log.i("openPort","ポートオープン処理：成功")
        }

        // util.showAlertDialog(mContext!!, resultMessage)
        mContext = null
        mBcpControl = null
        return resultMessage
    }

}