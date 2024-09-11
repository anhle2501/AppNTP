package vn.bvntp.app.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupWithNavController
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import vn.bvntp.app.App
import vn.bvntp.app.R
import vn.bvntp.app.databinding.ActivityHostBinding
import vn.bvntp.app.helper.retrieveAccessDataDecryptedData
import vn.bvntp.app.helper.storeEncryptedData
import vn.bvntp.app.model.ThongTinBenhNhanRequest
import vn.bvntp.app.viewmodel.HoSoBenhAnViewModel
import vn.bvntp.app.viewmodel.LoginViewModel
import vn.bvntp.app.viewmodel.ThongTinBenhNhanViewModel

class HostActivity : AppCompatActivity() {

    private var _binding: ActivityHostBinding? = null
    private val binding get() = _binding!!
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var hsbaViewModel: HoSoBenhAnViewModel
    private lateinit var loginViewModel: LoginViewModel
    private lateinit var thongTinBenhNhan: ThongTinBenhNhanViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityHostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.statusBarColor = ContextCompat.getColor(this, R.color.primaryLight)

//         config to display up button and display which screen you are in
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
//         config to display side navigation bar
        val drawer = binding.drawerLayout

        val builder = AppBarConfiguration.Builder(navController.graph)
        builder.setOpenableLayout(drawer)
        appBarConfiguration = builder.build()

        val toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        toolbar.setupWithNavController(navController, appBarConfiguration)
        // config to display bottom navigation bar
//        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav)
//        bottomNavigationView.setupWithNavController(navController)
        val navView = binding.navView
        // drawer icon color adjust
        navView.itemIconTintList = ContextCompat.getColorStateList(this, R.color.primaryLight)
        NavigationUI.setupWithNavController(navView, navController)
//        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)

        // open menu nhap ma benh nhan
        binding.fab.setOnClickListener{
            binding.scanPatientCode.visibility = if (binding.scanPatientCode.visibility == View.GONE) View.VISIBLE else View.GONE
        }

        // create viewModel for HostActivity
        val context = this
        val appContainer = (applicationContext as App).container
        hsbaViewModel =  ViewModelProvider(this, appContainer.hsbaViewModelFactory).get(HoSoBenhAnViewModel::class.java)
        loginViewModel = ViewModelProvider(this, appContainer.viewModelFactory).get(LoginViewModel::class.java)
        thongTinBenhNhan =  ViewModelProvider(this, appContainer.thongTinBenhNhanFactory).get(ThongTinBenhNhanViewModel::class.java)
        binding.lifecycleOwner = context
        binding.hsbaViewModel = hsbaViewModel

        binding.maBenhNhan.onFocusChangeListener =  View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                // Hide the keyboard when the EditText loses focus
                hideKeyboard(v)
            }
        }

        // close menu nhập bệnh nhân
        binding.closeNhapMaBenhNhan.setOnClickListener{
            if (binding.scanPatientCode.visibility == View.VISIBLE) binding.scanPatientCode.visibility = View.GONE
        }

        binding.maBenhNhan.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                if (s != null && s.length == 8) {
                    hsbaViewModel.updateMaBenhNhan(s.toString())
//                    autoInvokeModelLichSuDieuTri()
                    Toast.makeText(context, "Đang xử lý mã: " + hsbaViewModel.maBenhNhan.value.toString(), Toast.LENGTH_SHORT).show()
                }
            }
        })

        val tenBacSi = retrieveAccessDataDecryptedData(context, "ten_bac_si")
        val userName = retrieveAccessDataDecryptedData(context, "user_name")

        // create option for scan bar
        val barcodeLauncher: ActivityResultLauncher<ScanOptions> = registerForActivityResult(
            ScanContract()
        ) { result ->
            if (result.contents == null) {
                Toast.makeText(this, "Hủy", Toast.LENGTH_LONG).show()
            } else {
                hsbaViewModel.updateMaBenhNhan(result.contents.toString())
                Toast.makeText(this, "Quét: " + result.contents, Toast.LENGTH_LONG)
                    .show()
            }
        }
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.ONE_D_CODE_TYPES)
        options.setPrompt("Quét mã bệnh nhân")
        options.setCameraId(0)  // Use a specific camera of the device
        options.setBeepEnabled(true)
        options.setBarcodeImageEnabled(true)
        val barcodeLauncherWithOption = {  barcodeLauncher.launch(options) }

        binding.quetNhapMaBenhNhan.setOnClickListener{
            // ensure _mabenhnhn is "" when click
            hsbaViewModel.updateMaBenhNhan("")
            barcodeLauncherWithOption.invoke()
        }

//        thongTinBenhNhan.thongTinBenhNhan.observe(this, Observer {
//            thongTinBenhNhan.updateThongTinBenhNhanRequest("")
//        })

        hsbaViewModel.maBenhNhan.observe(this, Observer {
            hsbaViewModel.maBenhNhan.value?.let { it1 ->
                if ( it1.length == 8 ) {
                    Log.d("hsbaViewModel.maBenhNhan.value", hsbaViewModel.maBenhNhan.value.toString())
                    thongTinBenhNhan.getThongTinBenhNhanViewModel(context,
                        ThongTinBenhNhanRequest(MaBN = it1, SoVaoVien = "") , {} )
                }
            }
        })

//        thongTinBenhNhan.thongTinBenhNhan.observe(this, Observer {
//            Log.d("thongTinBenhNhan", "thongTinBenhNhan")
//        })

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return when (item.itemId) {
            R.id.helpFragment -> {
                item.onNavDestinationSelected(navController)
                true
            }
            R.id.logout -> {
                storeEncryptedData(this, "access_token", "")
                val intent = Intent(
                    this, LoginActivity::class.java
                )
                this.finish()
                this.startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
        return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
    }

//    override fun onSupportNavigateUp(): Boolean {
//        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()
//    }
    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    fun autoInvokeModelLichSuDieuTri() {
        hsbaViewModel.modelLichSuVaoVienView(this) { next() }
    }

    fun next(){
        Log.d("next", "next: ")
        Log.d("next", hsbaViewModel.lichSuDieuTri.value.toString())
        hsbaViewModel._isLock.value = false
    }

     fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}