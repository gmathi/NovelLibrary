package io.github.gmathi.novellibrary.activity.settings
//
//import android.os.Bundle
//import android.view.View
//import androidx.activity.viewModels
//import androidx.appcompat.app.AppCompatActivity
//import androidx.databinding.DataBindingUtil
//import com.afollestad.materialdialogs.MaterialDialog
//import io.github.gmathi.novellibrary.R
//import io.github.gmathi.novellibrary.databinding.ActivityGoogleBackupBinding
//import io.github.gmathi.novellibrary.viewmodel.GoogleBackupViewModel
//import kotlinx.android.synthetic.main.activity_google_backup.*
//import kotlinx.android.synthetic.main.content_google_backup.*
//
//class GoogleBackupActivity : AppCompatActivity() {
//
//    private val vm: GoogleBackupViewModel by viewModels()
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        //DataBinding so layout can use the properties directly in XML
//        val binding = DataBindingUtil.setContentView<ActivityGoogleBackupBinding>(this, R.layout.activity_google_backup)
//        binding.lifecycleOwner = this
//        binding.vm = vm
//
//        setSupportActionBar(toolbar)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//
//        vm.init(this, this)
//
//        backupButton.setOnClickListener {
//            backupButton.visibility = View.INVISIBLE
//            progressLayout.visibility = View.VISIBLE
//            //Toggle Visible States
//
//        }
//
//        stopButton.setOnClickListener {
//            progressLayout.visibility = View.INVISIBLE
//            backupButton.visibility = View.VISIBLE
//        }
//    }
//
////    fun showBackupDialog() {
////        MaterialDialog.Builder(this)
////            .title("Select Backup Items")
////            .items(R.array.backup_and_restore_options)
////            .itemsCallbackMultiChoice(arrayOf(0, 1, 2, 3)) { _, which, _ ->
////                if (which.isNotEmpty())
////                    vm.backupData(which.contains(0), which.contains(1), which.contains(2), which.contains(3))
////                true
////            }
////            .positiveText(R.string.okay)
////            .show()
////    }
//
//
//}