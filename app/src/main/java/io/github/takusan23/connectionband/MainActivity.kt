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
            if (cellInfo is CellInfoLte) {
                // 4G / LTE
                val band = getBand(cellInfo.cellIdentity.earfcn)
                viewBinding.activityMainTextView.text = "接続中バンド：${band}"
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                // 5G NR
                if (cellInfo.cellIdentity is CellIdentityNr) {
                    val band = getNRBand((cellInfo.cellIdentity as CellIdentityNr).nrarfcn)
                    viewBinding.activityMainTextView.text = "接続中バンド：n${band}"
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
                BandData(14, 5280, 5379),
                BandData(17, 5730, 5849),
                BandData(18, 5850, 5999),
                BandData(19, 6000, 6149),
                BandData(20, 6150, 6449),
                BandData(21, 6450, 6599),
                BandData(24, 7700, 8039),
                BandData(33, 36000, 36199),
                BandData(34, 36200, 36349),
                BandData(35, 36350, 36949),
                BandData(36, 36950, 37549),
                BandData(37, 37550, 37749),
                BandData(38, 37750, 38249),
                BandData(39, 38250, 38649),
                BandData(40, 38650, 39649),
                BandData(41, 39650, 41589),
                BandData(42, 41590, 43589),
                BandData(43, 43590, 45589),
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