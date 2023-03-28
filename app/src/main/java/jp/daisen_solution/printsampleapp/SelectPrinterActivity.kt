package jp.daisen_solution.printsampleapp

import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.PaintDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import kotlin.system.exitProcess
import jp.daisen_solution.printsampleapp.databinding.ActivitySelectPrinterBinding

class SelectPrinterActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySelectPrinterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectPrinterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val con = this.applicationContext
        try {
            /////////////////////////////////////////////////////////////////////////////////////
            // ペアリング済みのBluetooth機器一覧を作成
            /////////////////////////////////////////////////////////////////////////////////////
            val adapter =
                ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice)
            val bluetoothManager =
                this.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val pairedBluetoothDevices = bluetoothManager.adapter.bondedDevices
            if (pairedBluetoothDevices == null) {
                Toast.makeText(this, R.string.msg_NoPaireddevice, Toast.LENGTH_LONG).show()
                return
            }
            val pairedBluetoothDeviceName =
                this.getSharedPreferences(Consts.bcpSectionName, Context.MODE_PRIVATE)
                    .getString(Consts.pairingNameKey, "")
            var position = 0
            var selectPosition = 0
            for (device in pairedBluetoothDevices) {
                var bluetoothDeviceName: String
                bluetoothDeviceName = device.name + " (" + device.address + ")"
                adapter.add(bluetoothDeviceName)
                if (pairedBluetoothDeviceName != null && pairedBluetoothDeviceName.isNotEmpty()) {
                    if (bluetoothDeviceName.compareTo(pairedBluetoothDeviceName) == 0) {
                        selectPosition = position   // 前回接続したデバイスをデフォルト選択
                    } else {
                        val bdAddress = bluetoothDeviceName.substring(
                            bluetoothDeviceName.indexOf("(") + 1, bluetoothDeviceName.indexOf(")")
                        )
                        if (bdAddress == pairedBluetoothDeviceName) {
                            selectPosition = position
                        }
                    }
                }
                position += 1
            }
            binding.bluetoothListView.choiceMode = ListView.CHOICE_MODE_SINGLE
            binding.bluetoothListView.adapter = adapter
            binding.bluetoothListView.selector = PaintDrawable(Color.BLUE)
            binding.bluetoothListView.setItemChecked(selectPosition, true)  // デフォルト行を選択

            binding.connectButton.setOnClickListener {
                Log.v("connectButton", binding.bluetoothListView.getItemAtPosition(selectPosition).toString())
                val item = binding.bluetoothListView.getItemAtPosition(selectPosition) as String
                //val intent = Intent(this, MainActivity::class.java)
                val intentBack = Intent()
                intentBack.putExtra(Consts.bluetoothDeviceExtra, item )
                setResult(Activity.RESULT_OK, intentBack)
                finish()
            }

        } catch (th: Throwable) {
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // 「前画面に戻る」ボタン押下時の処理
    ////////////////////////////////////////////////////////////////////////////////////////////////
    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (event!!.action == KeyEvent.ACTION_DOWN) {
            if (event!!.keyCode == KeyEvent.KEYCODE_BACK) {

                val alertBuilder = AlertDialog.Builder(this)
                alertBuilder.setMessage(R.string.alert_AppExit)
                alertBuilder.setCancelable(false)
                alertBuilder.setPositiveButton(R.string.msg_Ok) { _, _ ->
                    exitProcess(RESULT_OK) }
                alertBuilder.setNegativeButton(R.string.msg_No) { _, _ ->
                    // 何もしない
                }
                alertBuilder.show()
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }
}