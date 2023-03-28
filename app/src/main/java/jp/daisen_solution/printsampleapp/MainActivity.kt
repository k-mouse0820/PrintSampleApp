package jp.daisen_solution.printsampleapp

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import jp.co.toshibatec.bcp.library.BCPControl
import jp.co.toshibatec.bcp.library.LongRef
import jp.co.toshibatec.bcp.library.StringRef
import jp.daisen_solution.printsampleapp.databinding.ActivityMainBinding
import jp.daisen_solution.printsampleapp.databinding.ActivityMainBinding.*
import jp.daisen_solution.printsampleapp.print.*
import jp.daisen_solution.printsampleapp.utils.util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean


class MainActivity : AppCompatActivity(),

    BCPControl.LIBBcpControlCallBack {

    //Printer
    private var isPrinterConnected = false
    private var mBcpControl: BCPControl? = null
    private var mConnectionData: ConnectionData? = ConnectionData()
    private var mPrintData: PrintData? = PrintData()
    private var mPrintDialogDelegate: PrintDialogDelegate? = null
    private var systemPath: String = ""


    private lateinit var context: Context
    private lateinit var mActivity: Activity
    private lateinit var mOptionsMenu: Menu


    private val printerSelectResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val printerBluetoothDeviceExtra = it.data?.getStringExtra(Consts.bluetoothDeviceExtra)
            Log.v("DEBUG", printerBluetoothDeviceExtra!!)
            val printerBdAddress = printerBluetoothDeviceExtra!!.substring(
                printerBluetoothDeviceExtra!!.indexOf("(") + 1,
                printerBluetoothDeviceExtra!!.indexOf(")")
            )
            if (printerBdAddress == null || printerBdAddress.isEmpty()) {
                util.showAlertDialog(this, this.getString(R.string.bdAddrNotSet) )
                confirmationEndDialog(this)
            }

            /////////////////////////////////////////////////////////////////////////////////////
            // プリンタ初期設定
            /////////////////////////////////////////////////////////////////////////////////////
            mBcpControl = BCPControl(this)
            mPrintDialogDelegate = PrintDialogDelegate(this, mBcpControl!!, mPrintData)

            // systemPathを設定
            systemPath = Environment.getDataDirectory().path + "/data/" + this.packageName
            Log.i("set systemPath", systemPath)
            mBcpControl!!.systemPath = systemPath

            var continueFlag = true

            // プリンタ設定ファイル、ラベルフォーマットファイルのセット
            val newfile = File(systemPath)
            if (!newfile.exists()) {
                if (newfile.mkdirs()) {}
            }
            try {
                util.asset2file(applicationContext, "PrtList.ini", systemPath, "PrtList.ini")
                util.asset2file(applicationContext, "PRTEP2G.ini", systemPath, "PRTEP2G.ini")
                util.asset2file(applicationContext, "PRTEP4T.ini", systemPath, "PRTEP4T.ini")
                util.asset2file(applicationContext, "PRTEP2GQM.ini", systemPath, "PRTEP2GQM.ini")
                util.asset2file(applicationContext, "PRTEP4GQM.ini", systemPath, "PRTEP4GQM.ini")
                util.asset2file(applicationContext, "PRTEV4TT.ini", systemPath, "PRTEV4TT.ini")
                util.asset2file(applicationContext, "PRTEV4TG.ini", systemPath, "PRTEV4TG.ini")
                util.asset2file(applicationContext, "PRTLV4TT.ini", systemPath, "PRTLV4TT.ini")
                util.asset2file(applicationContext, "PRTLV4TG.ini", systemPath, "PRTLV4TG.ini")
                util.asset2file(applicationContext, "PRTFP2DG.ini", systemPath, "PRTFP2DG.ini")
                util.asset2file(applicationContext, "PRTFP3DG.ini", systemPath, "PRTFP3DG.ini")
                util.asset2file(applicationContext, "PRTBA400TG.ini", systemPath, "PRTBA400TG.ini")
                util.asset2file(applicationContext, "PRTBA400TT.ini", systemPath, "PRTBA400TT.ini")
                util.asset2file(applicationContext, "PRTBV400G.ini", systemPath, "PRTBV400G.ini")
                util.asset2file(applicationContext, "PRTBV400T.ini", systemPath, "PRTBV400T.ini")

                util.asset2file(applicationContext, "ErrMsg0.ini", systemPath, "ErrMsg0.ini")
                util.asset2file(applicationContext, "ErrMsg1.ini", systemPath, "ErrMsg1.ini")
                util.asset2file(applicationContext, "resource.xml", systemPath, "resource.xml")

                util.asset2file(applicationContext, "EP2G_scanToPrint.lfm", systemPath, "tempLabel.lfm")
                //util.asset2file(applicationContext, "B_LP2D_label.lfm", systemPath, "tempLabel.lfm")
            } catch (e: Exception) {
                util.showAlertDialog(this,
                    "Failed to copy ini and lfm files.")
                e.printStackTrace()
                continueFlag = false
                //return
            }

            if (continueFlag) {
                // 使用するプリンタの設定   B-LP2Dは「27」
                mBcpControl!!.usePrinter = 27
                //mBcpControl!!.usePrinter = 99
                Log.v("DEBUG", mBcpControl!!.usePrinter.toString())

                // 通信パラメータの設定
                mConnectionData!!.issueMode = Consts.AsynchronousMode   // 1:送信完了復帰  2:発行完了復帰
                mConnectionData!!.portSetting = "Bluetooth:$printerBdAddress"

                // 通信ポートのオープン　（非同期処理）
                var mOpenPortTask = OpenPortTask(this, mBcpControl, mConnectionData)

                binding.progressAction.visibility = View.VISIBLE
                binding.progressMessage.text = getString(R.string.msg_connectingPrinter)

                if (!mConnectionData!!.isOpen.get()) {
                    CoroutineScope(Dispatchers.Main).launch {
                        var resultMessage = mOpenPortTask.openBluetoothPort()
                        binding.progressAction.visibility = View.GONE
                        if (resultMessage.equals(getString(R.string.msg_success))) {
                            Log.i("openPort", "isOpen = " + mConnectionData!!.isOpen.toString())
                            mOptionsMenu.findItem(R.id.printer).setIcon(R.drawable.baseline_print_24_white)
                            binding.fabPrint.setImageResource(R.drawable.baseline_print_24_white)
                        } else {
                            mOptionsMenu.findItem(R.id.printer).setIcon(R.drawable.baseline_print_24)
                            util.showAlertDialog(context, resultMessage)
                        }
                    }
                } else {
                    Log.v("openPort", "Already opened - skip")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mActivity = this

        scannerBtSPP.setupService()
        scannerBtSPP.startService(BluetoothState.DEVICE_OTHER)

        if(!scannerBtSPP.isBluetoothAvailable) {
            Toast.makeText(this,getString(R.string.msg_bt_not_available), Toast.LENGTH_LONG).show()
            finish()
        }

        scanAdapter = ScanAdapter(this)

        binding.rvScan.layoutManager = LinearLayoutManager(this)
        binding.rvScan.adapter = scanAdapter
        val dividerItemDecoration = DividerItemDecoration(this, LinearLayoutManager(this).orientation)
        binding.rvScan.addItemDecoration(dividerItemDecoration)

        binding.fabPrint.setOnClickListener { fabClicked() }

        scannerBtSPP.setOnDataReceivedListener(this)
        scannerBtSPP.setBluetoothConnectionListener(this)
        scannerBtSPP.setBluetoothStateListener(this)

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        mOptionsMenu = menu!!
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.scanner -> {
                if(isScannerConnected){
                    scannerBtSPP.disconnect()
                    item.setIcon(R.drawable.baseline_qr_code_scanner_24)
                } else {
                    val intent = Intent(this, DeviceList::class.java)
                    scannerSelectResultLauncher.launch(intent)
                }
            }
            R.id.printer -> {
                if(isPrinterConnected){
                    //bt.disconnect()
                    item.setIcon(R.drawable.baseline_print_24)
                } else {
                    val intent = Intent(applicationContext, SelectPrinterActivity::class.java)
                    printerSelectResultLauncher.launch(intent)
                }

            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun fabClicked() {

        val hinban = scannedCodes[scannedCodes.size - 1].codeId


        mPrintData = PrintData()
        val result = LongRef(0)
        mPrintData!!.currentIssueMode = mConnectionData!!.issueMode
        mPrintData!!.printCount = 1  // 決め打ちで１枚とする

        val printItemList = HashMap<String?, String?>()

        // 品番データ（8桁）
        if (hinban.length > 8) {
            printItemList[getString(R.string.hinbanData)] = hinban.substring(0,7)
        } else {
            printItemList[getString(R.string.hinbanData)] = hinban
        }

        // 品名データ（14桁）
        val hinmei = "テスト品名"
        if (hinmei.length > 14) {
            printItemList[getString(R.string.hinmeiData)] = hinmei.substring(0,13)
        } else {
            printItemList[getString(R.string.hinmeiData)] = hinmei
        }

        // 仕入先データ（14桁）
        val siiresaki = "テスト仕入先"
        if (siiresaki.length > 14) {
            printItemList[getString(R.string.siiresakiData)] = siiresaki.substring(0,13)
        } else {
            printItemList[getString(R.string.siiresakiData)] = siiresaki
        }

        // QRCODE
        val qrcode = hinban
        printItemList[getString(R.string.qrcodeData)] = qrcode

        // 印刷データをセット
        mPrintData!!.objectDataList = printItemList

        // lfmファイルをセット
        val filePathName =
            systemPath + "/tempLabel.lfm"
        mPrintData!!.lfmFileFullPath = filePathName

        // 印刷実行スレッドの起動
        var mPrintExecuteTask = PrintExecuteTask(this,mBcpControl, mPrintData)
        binding.progressAction.visibility = View.VISIBLE
        binding.progressMessage.text = getString(R.string.msg_executingPrint)

        CoroutineScope(Dispatchers.Main).launch {
            var resultMessage = mPrintExecuteTask.print()
            binding.progressAction.visibility = View.INVISIBLE
            when (resultMessage) {
                getString(R.string.msg_success) -> {
                    // mActivity.showDialog(PrintDialogDelegate.Companion.PRINT_COMPLETEMESSAGE_DIALOG)
                }
                getString(R.string.msg_RetryError) -> {
                    mActivity.showDialog(PrintDialogDelegate.Companion.RETRYERRORMESSAGE_DIALOG)
                }
                else -> {
                    mActivity.showDialog(PrintDialogDelegate.Companion.ERRORMESSAGE_DIALOG)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if(!scannerBtSPP.isBluetoothEnabled) scannerBtSPP.enable()
    }

    override fun onStop() {
        super.onStop()
        //scannerBtSPP.stopService()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDataReceived(data: ByteArray, message: String) {
        scannedCodes.add(Code(message, DateTimeFormatter.ISO_INSTANT.format(Instant.now())))
        scanAdapter.setData(scannedCodes)
        binding.rvScan.scrollToPosition(scannedCodes.size - 1)
    }

    override fun onDeviceConnected(name: String?, address: String?) {
        Toast.makeText(this, getText(R.string.msg_scanner_connected), Toast.LENGTH_SHORT).show()
        binding.progressMessage.text = getText(R.string.msg_connecting)
        binding.progress.visibility = View.VISIBLE
        binding.progressAction.visibility = View.GONE
        mOptionsMenu.findItem(R.id.scanner).setIcon(R.drawable.baseline_qr_code_scanner_24_white)
    }

    override fun onDeviceDisconnected() {
        Toast.makeText(this, getText(R.string.msg_scanner_disconnected), Toast.LENGTH_SHORT).show()
        binding.progress.visibility = View.GONE
        binding.progressAction.visibility = View.GONE
        mOptionsMenu.findItem(R.id.scanner).setIcon(R.drawable.baseline_qr_code_scanner_24)
    }
    override fun onDeviceConnectionFailed() {
        Toast.makeText(this, getText(R.string.msg_scanner_connection_failed), Toast.LENGTH_SHORT).show()
        binding.fabPrint.setImageResource(R.drawable.baseline_stop_24)
        binding.progress.visibility = View.GONE
        binding.progressAction.visibility = View.GONE
        mOptionsMenu.findItem(R.id.scanner).setIcon(R.drawable.baseline_qr_code_scanner_24)
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // プリンターからのメッセージ受信
    ////////////////////////////////////////////////////////////////////////////////////////////////
    override fun BcpControl_OnStatus(PrinterStatus: String?, Result: Long) {
        var strMessage = ""
        val message = StringRef("")
        strMessage = if (! mBcpControl!!.GetMessage(Result, message)) {
            String.format(getString(R.string.statusReception) + " %s : %s ", PrinterStatus, "failed to error message")
        } else {
            String.format(getString(R.string.statusReception) + " %s : %s ", PrinterStatus, message.getStringValue())
        }
        Log.i("onStatus", strMessage)
    }


    override fun onServiceStateChanged(state: Int) {
        if(state == BluetoothState.STATE_CONNECTING) {
            binding.progressMessage.text = getText(R.string.msg_connecting)
            binding.progressAction.visibility = View.VISIBLE
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // プリンタのBluetoothポートをクローズするメソッド
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private fun closePrinterBluetoothPort() {
        Log.i("closePort","close port start")
        if (mConnectionData!!.isOpen.get()) {
            Log.i("closePort","close port start2")
            val Result = LongRef(0)
            if (! mBcpControl!!.ClosePort(Result)) {
                val Message = StringRef("")
                if (! mBcpControl!!.GetMessage(Result.longValue, Message)) {
                    Log.e("closePort",String.format(R.string.msg_PortCloseErrorcode.toString() + "= %08x", Result.longValue))
                    util.showAlertDialog(
                        this,
                        String.format(R.string.msg_PortCloseErrorcode.toString() + "= %08x", Result.longValue)
                    )
                } else {
                    Log.e("closePort",Message.getStringValue())
                    util.showAlertDialog(this, Message.getStringValue())
                }
            } else {
                Log.i("closePort",this.getString(R.string.msg_PortCloseSuccess))
                //util.showAlertDialog(this, this.getString(R.string.msg_PortCloseSuccess))
                mConnectionData!!.isOpen = AtomicBoolean(false)
                mOptionsMenu.findItem(R.id.printer).setIcon(R.drawable.baseline_print_24)
                binding.fabPrint.setImageResource(R.drawable.baseline_stop_24)
            }
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // 「前画面に戻る」ボタン押下時の処理
    ////////////////////////////////////////////////////////////////////////////////////////////////
    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (event!!.action == KeyEvent.ACTION_DOWN) {
            if (event!!.keyCode == KeyEvent.KEYCODE_BACK) {
                confirmationEndDialog(this)
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }
    private fun confirmationEndDialog(activity: Activity) {
        val alertBuilder = AlertDialog.Builder(this)
        alertBuilder.setMessage(R.string.confirmBack)
        alertBuilder.setCancelable(false)
        alertBuilder.setPositiveButton(R.string.msg_Ok) { _, _ ->
            this.closePrinterBluetoothPort()
            mBcpControl = null
            mConnectionData = null
            mPrintData = null
            mPrintDialogDelegate = null
            finish()
        }
        alertBuilder.setNegativeButton(R.string.msg_No) { _, _ ->
            // 何もしない
        }
        val alertDialog = alertBuilder.create()
        alertDialog.show()
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Dialog作成処理
    ////////////////////////////////////////////////////////////////////////////////////////////////
    override fun onPrepareDialog(id: Int, dialog: Dialog) {
        if (false == mPrintDialogDelegate!!.prepareDialog(id, dialog)) {
            super.onPrepareDialog(id, dialog)
        }
    }
    override fun onCreateDialog(id: Int): Dialog {
        var dialog: Dialog? = null
        dialog = mPrintDialogDelegate!!.createDialog(id)
        if (null == dialog) {
            dialog = super.onCreateDialog(id)
        }
        return dialog!!
    }


}