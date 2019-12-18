package com.frolo.muse.ui.main

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.Window
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProviders
import com.frolo.muse.R
import com.frolo.muse.arch.observe
import com.frolo.muse.arch.observeNonNull
import com.frolo.muse.engine.Player
import com.frolo.muse.ui.base.BackPressHandler
import com.frolo.muse.ui.base.BaseActivity
import com.frolo.muse.ui.base.FragmentNavigator
import com.frolo.muse.ui.main.audiofx.AudioFxFragment
import com.frolo.muse.ui.main.library.LibraryFragment
import com.frolo.muse.ui.main.library.search.SearchFragment
import com.frolo.muse.ui.main.player.PlayerFragment
import com.frolo.muse.ui.main.settings.AppBarSettingsFragment
import com.ncapdevi.fragnav.FragNavController
import com.ncapdevi.fragnav.FragNavTransactionOptions
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : BaseActivity(),
        FragmentNavigator,
        PlayerHolderFragment.PlayerConnection {

    companion object {
        private const val RC_READ_STORAGE = 1043

        private const val EXTRA_TAB_INDEX = "last_tab_index"

        const val INDEX_LIBRARY = FragNavController.TAB1
        const val INDEX_PLAYER = FragNavController.TAB2
        const val INDEX_EQUALIZER = FragNavController.TAB3
        const val INDEX_SEARCH = FragNavController.TAB4
        const val INDEX_SETTINGS = FragNavController.TAB5

        fun newIntent(
                context: Context,
                tabIndex: Int = INDEX_PLAYER): Intent {

            return Intent(context, MainActivity::class.java)
                    .putExtra(EXTRA_TAB_INDEX, tabIndex)
        }
    }

    /*presentation*/
    private lateinit var viewModel: MainViewModel

    // Fragment controller
    private var pendingFragControllerInitialization = false
    private val isFragmentManagerStateSaved: Boolean get() = supportFragmentManager.isStateSaved
    private var fragNavController: FragNavController? = null
    private var currTabIndex = 1

    // hold this value for fragment controller because it's initialization is asynchronous relatively to the activity creation
    private var lastSavedInstanceState: Bundle? = null

    // Rate Dialog
    private var rateDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        supportRequestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY) // it's important to call window feature before onCreate

        lastSavedInstanceState = savedInstanceState

        super.onCreate(savedInstanceState)

        val vmFactory = requireApp()
                .appComponent
                .provideVMFactory()
        viewModel = ViewModelProviders.of(this, vmFactory)[MainViewModel::class.java]

        setContentView(R.layout.activity_main)

        // we need to determine the index
        currTabIndex = if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_TAB_INDEX)) {
            savedInstanceState.getInt(EXTRA_TAB_INDEX, 1)
        } else {
            intent?.getIntExtra(EXTRA_TAB_INDEX, 1) ?: 1
        }

        checkForPlayerHolder()

        requireApp().onFragmentNavigatorCreated(this)

        observerViewModel(this)
    }

    override fun onRestart() {
        super.onRestart()
        if (pendingFragControllerInitialization) {
            pendingFragControllerInitialization = false
            initializeFragNavController(lastSavedInstanceState)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            fragNavController?.let { controller ->
                if (!controller.isRootFragment && controller.popFragment().not())
                    finish()
            }
            true
        } else super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray) {
        if (requestCode == RC_READ_STORAGE) {
            val index = permissions.indexOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (index >= 0) {
                if (grantResults[index] == PackageManager.PERMISSION_GRANTED) {
                    viewModel.onReadStoragePermissionGranted()
                } else {
                    viewModel.onReadStoragePermissionDenied()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNewIntent(intent)
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(EXTRA_TAB_INDEX, currTabIndex)
        fragNavController?.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        fragNavController.let { controller ->
            if (isFragmentManagerStateSaved) {
                super.onBackPressed()
            }

            if (controller == null) { // not even initialized yet
                finish()
                return
            }

            val current = controller.currentFrag
            if (current is BackPressHandler
                    /*
                    Also need to check if the fragment has a view created,
                    so it is able to handle the back press
                    */
                    && current.view != null) {
                if (current.onBackPress()) { // fragment successfully handled it itself
                    return
                }
            }

            if (controller.isRootFragment) {
                // Just call finish. Calling onBackPressed() causes popping bac stack from the fragment manager.
                // This will simply removes PlayerHolderFragment and not finish the activity.
                // This is not what we want.
                //super.onBackPressed()
                finish()
            } else if (controller.popFragment().not()) { // no fragments left in the stack
                // Just call finish. Calling onBackPressed() causes popping bac stack from the fragment manager.
                // This will simply removes PlayerHolderFragment and not finish the activity.
                // This is not what we want.
                //super.onBackPressed()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        requireApp().onFragmentNavigatorDestroyed()
    }

    override fun pushFragment(newFragment: Fragment) {
        if (isFragmentManagerStateSaved)
            return
        fragNavController?.pushFragment(newFragment)
    }

    override fun pop() {
        if (isFragmentManagerStateSaved)
            return
        fragNavController?.let { controller ->
            val currDialogFrag = controller.currentDialogFrag
            if (currDialogFrag != null
                    && currDialogFrag.isAdded) {
                // There's a dialog fragment opened.
                // Clear it first.
                controller.clearDialogFragment()
            } else {
                val stack = controller.currentStack
                if (stack != null && stack.size > 1) {
                    // pop stack only if it's not null and its size is more than 1.
                    controller.popFragments(1)
                } else {
                    // Stack is empty or has only root fragment.
                    // Let's finish the activity.
                    finish()
                }
            }
        }
    }

    override fun pushDialog(newDialog: DialogFragment) {
        if (isFragmentManagerStateSaved)
            return

        fragNavController?.showDialogFragment(newDialog)
    }

    override fun onPlayerConnected(player: Player) {
        requireApp().onPlayerConnected(player)
        viewModel.onPlayerConnected(player)
        initializeFragNavController(lastSavedInstanceState)
    }

    override fun onPlayerDisconnected() {
        viewModel.onPlayerDisconnected()
        requireApp().onPlayerDisconnected()
        finish()
    }

    // return true if created successfully
    private fun initializeFragNavController(savedInstanceState: Bundle?): Boolean {
        val fragmentManager = supportFragmentManager
        if (fragmentManager.isStateSaved) {
            pendingFragControllerInitialization = true
            return false
        }

        fragNavController = FragNavController(fragmentManager, R.id.container).apply {
            //fragmentHideStrategy = FragNavController.REMOVE
            defaultTransactionOptions = FragNavTransactionOptions
                    .newBuilder()
                    .customAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                    .build()
            rootFragmentListener = object : FragNavController.RootFragmentListener {
                override val numberOfRootFragments = 5
                override fun getRootFragment(index: Int): Fragment {
                    when (index) {
                        INDEX_LIBRARY -> return LibraryFragment.newInstance()
                        INDEX_PLAYER -> return PlayerFragment.newInstance()
                        INDEX_EQUALIZER-> return AudioFxFragment.newInstance()
                        INDEX_SEARCH -> return SearchFragment.newInstance()
                        INDEX_SETTINGS-> return AppBarSettingsFragment.newInstance()
                    }
                    throw IllegalStateException("Unknown index: $index")
                }

            }
            initialize(currTabIndex, savedInstanceState)
        }

        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            if (isFragmentManagerStateSaved)
                return@setOnNavigationItemSelectedListener true

            fragNavController?.also { controller ->
                when (menuItem.itemId) {
                    R.id.nav_library -> {
                        controller.switchTab(INDEX_LIBRARY)
                        currTabIndex = 0
                    }
                    R.id.nav_player -> {
                        controller.switchTab(INDEX_PLAYER)
                        currTabIndex = 1
                    }
                    R.id.nav_equalizer -> {
                        controller.switchTab(INDEX_EQUALIZER)
                        currTabIndex = 2
                    }
                    R.id.nav_search -> {
                        controller.switchTab(INDEX_SEARCH)
                        currTabIndex = 3
                    }
                    R.id.nav_settings -> {
                        controller.switchTab(INDEX_SETTINGS)
                        currTabIndex = 4
                    }
                }
            }
            return@setOnNavigationItemSelectedListener true
        }
        bottomNavigationView.setOnNavigationItemReselectedListener { fragNavController?.clearStack() }
        bottomNavigationView.selectedItemId = when(currTabIndex) {
            INDEX_LIBRARY -> R.id.nav_library
            INDEX_PLAYER -> R.id.nav_player
            INDEX_EQUALIZER -> R.id.nav_equalizer
            INDEX_SEARCH -> R.id.nav_search
            INDEX_SETTINGS -> R.id.nav_settings
            else -> R.id.nav_library
        }
        return true
    }

    private fun showRateDialog() {
        rateDialog?.dismiss()

        val dialog = RateDialog(this) { dialog, what ->
            dialog.dismiss()
            when (what) {
                RateDialog.Button.NO -> viewModel.onDismissRate()
                RateDialog.Button.REMIND_LATER -> viewModel.onWishingAskingLater()
                RateDialog.Button.RATE -> viewModel.onApproveToRate()
            }
        }

        rateDialog = dialog.apply {
            setOnCancelListener { viewModel.onCancelledRateDialog() }
            show()
        }
    }

    private fun handleNewIntent(intent: Intent) {
        val extraTabIndex = intent.getIntExtra(EXTRA_TAB_INDEX, currTabIndex)
        if (extraTabIndex != currTabIndex) {
            bottomNavigationView.selectedItemId = when(extraTabIndex) {
                INDEX_LIBRARY -> R.id.nav_library
                INDEX_PLAYER -> R.id.nav_player
                INDEX_EQUALIZER -> R.id.nav_equalizer
                INDEX_SEARCH -> R.id.nav_search
                INDEX_SETTINGS -> R.id.nav_settings
                else -> R.id.nav_library
            }
        }
    }

    private fun checkForPlayerHolder() {
        // Need to find PlayerHolderFragment
        val playerHolderTag = "PlayerHolder"
        val playerHolderFragment = supportFragmentManager.findFragmentByTag(playerHolderTag) as? PlayerHolderFragment
        val player = playerHolderFragment?.player
        if (playerHolderFragment == null || player == null) {
            // PlayerHolderFragment or its player instance is null. Need to start from the beginning.

            // We assume that the saved instance state is zero
            lastSavedInstanceState = null

            // Now we remove all back stack entries from the fragment manager.
            for (i in 0 until supportFragmentManager.backStackEntryCount) {
                supportFragmentManager.popBackStackImmediate()
            }
            // Then we remove all fragments from the fragment manager.
            val removeAllFragmentsTransaction = supportFragmentManager.beginTransaction()
            for (fragment in supportFragmentManager.fragments) {
                if (fragment != null) {
                    removeAllFragmentsTransaction.remove(fragment)
                }
            }
            removeAllFragmentsTransaction.commitNow()

            // Finally we add a new PlayerHolderFragment.
            // NOTE: it must be added to the back stack as well.
            supportFragmentManager
                    .beginTransaction()
                    .add(PlayerHolderFragment(), playerHolderTag)
                    .addToBackStack(null) // important to add it to back stack, otherwise - fragment will be removed on activity's destroy
                    .commit()
        } else {
            // OK there is a PlayerHolderFragment with non-null player instance. We can assume it's connected.
            onPlayerConnected(player)
        }
    }

    private fun checkReadStoragePermission(requestIfNeeded: Boolean) {
        val permission = Manifest.permission.READ_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            viewModel.onReadStoragePermissionGranted()
        } else if (requestIfNeeded) {
            requestPermissions(arrayOf(permission), RC_READ_STORAGE)
        }
    }

    private fun requestReadStoragePermission() {
        checkReadStoragePermission(true)
    }

    private fun observerViewModel(owner: LifecycleOwner) {
        viewModel.apply {
            askToRateEvent.observe(owner) {
                showRateDialog()
            }

            askReadStoragePermissionsEvent.observe(owner) {
                requestReadStoragePermission()
            }
        }
    }
}