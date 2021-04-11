package io.github.takusan23.connectionband

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.github.takusan23.connectionband.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val viewBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private val permissionCallBack = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            // 権限もらえた
            setBandToTextView()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        start()

        viewBinding.activityMainButton.setOnClickListener {
            start()
        }

    }

    /** 権限があれば計測、なければ権限をもらう */
    private fun start() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // 権限あり
            setBandToTextView()
        } else {
            // 権限なし。貰いに行く
            permissionCallBack.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    /** 接続しているバンドをTextViewに入れる */
    private fun setBandToTextView() {
        getEarfcn { cellInfo ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                when (cellInfo) {
                    is CellInfoLte -> {
                        // 4G / LTE
                        val band = getBand(cellInfo.cellIdentity.earfcn)
                        viewBinding.activityMainTextView.text = "接続中バンド：${band}"
                    }
                    is CellInfoNr -> {
                        // 5G NR
                        val band = getNRBand((cellInfo.cellIdentity as CellIdentityNr).nrarfcn)
                        viewBinding.activityMainTextView.text = "接続中バンド：n${band}"
                    }
                }
            } else {
                when (cellInfo) {
                    is CellInfoLte -> {
                        // 4G / LTE
                        val band = getBand(cellInfo.cellIdentity.earfcn)
                        viewBinding.activityMainTextView.text = "接続中バンド：${band}"
                    }
                }
            }
        }
    }

    /** TextViewにバンドを表示させる。コールバック形式になります */
    private fun getEarfcn(result: (CellInfo) -> Unit) {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // Android 10以降
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                telephonyManager.requestCellInfoUpdate(mainExecutor, object : TelephonyManager.CellInfoCallback() {
                    override fun onCellInfo(cellInfo: MutableList<CellInfo>) {
                        result.invoke(cellInfo[0])
                    }
                })
            }
        } else {
            // Android 9以前
            result.invoke(telephonyManager.allCellInfo[0])
        }
    }

    /** EARFCNからバンドを割り出す */
    private fun getBand(earfcn: Int): Int {
        println(earfcn)
        /**
         * https://www.arib.or.jp/english/html/overview/doc/STD-T104v2_10/5_Appendix/Rel11/36/36101-b50.pdf
         *
         * 33ページ目、「Table 5.7.3-1: E-UTRA channel numbers」参照
         * */
        val bandList = listOf(
                BandData(1, 0, 599),
                BandData(2, 600, 1199),
                BandData(3, 1200, 1949),
                BandData(4, 1950, 2399),
                BandData(5, 2400, 2649),
                BandData(6, 2650, 2749),
                BandData(7, 2750, 3449),
                BandData(8, 3450, 3799),
                BandData(9, 3800, 4149),
                BandData(10, 4150, 4749),
                BandData(11, 4750, 4949),
                BandData(12, 5010, 5179),
                BandData(13, 5180, 5279),
                BandData(14, 5280, 5279),
                BandData(17, 5730, 5849),
                BandData(18, 5850, 5999),
                BandData(19, 6000, 6149),
                BandData(20, 6150, 6449),
                BandData(21, 6450, 6599),
                BandData(22, 6600, 7399),
                BandData(23, 7500, 7699),
                BandData(24, 7700, 8039),
                BandData(25, 8040, 8689),
                BandData(26, 8690, 9039),
                BandData(27, 9040, 9209),
                BandData(28, 9210, 9659),
                BandData(29, 9660, 9769),
                BandData(30, 9770, 9869),
                BandData(31, 9870, 9919),
                BandData(32, 9920, 10359),
                BandData(65, 65536, 66435),
                BandData(66, 66436, 67335),
                BandData(67, 67336, 67535),
                BandData(68, 67536, 67835),
                BandData(69, 67836, 68335),
                BandData(70, 68336, 68585),
                BandData(71, 68586, 68935),
        )
        // 探す
        return bandList.find { bandData -> earfcn in bandData.nDlMin..bandData.nDlMax }?.bandNumber ?: 0
    }

    /** バンドを取得する。5G対応版 */
    private fun getNRBand(arfcn: Int): Int {
        val bandList = listOf(
                BandData(1, 422000, 434000),
                BandData(2, 386000, 398000),
                BandData(3, 361000, 376000),
                BandData(5, 173800, 178800),
                BandData(7, 524000, 538000),
                BandData(8, 185000, 192000),
                BandData(20, 158200, 164200),
                BandData(28, 151600, 160600),
                BandData(38, 514000, 524000),
                BandData(41, 499200, 537999),
                BandData(50, 286400, 303400),
                BandData(51, 285400, 286400),
                BandData(66, 422000, 440000),
                BandData(70, 399000, 404000),
                BandData(71, 123400, 130400),
                BandData(74, 295000, 303600),
                BandData(75, 286400, 303400),
                BandData(76, 285400, 286400),
                BandData(77, 620000, 680000),
                BandData(78, 620000, 653333),
                BandData(79, 693334, 733333),
                // 5G ミリ波
                BandData(257, 2054167, 2104166),
                BandData(258, 2016667, 2070833),
                BandData(260, 2229167, 2279166),
        )
        // 探す
        return bandList.find { bandData -> arfcn in bandData.nDlMin..bandData.nDlMax }?.bandNumber ?: 0
    }

    /**
     * バンドの表データ
     * @param bandNumber バンドの番号
     * @param nDlMin Earfcnここから
     * @param nDlMax Earfcnここまで
     * */
    data class BandData(val bandNumber: Int, val nDlMin: Int, val nDlMax: Int)

}