package com.wa2c.android.cifsdocumentsprovider.presentation.ui.common

import android.content.Context
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.wa2c.android.cifsdocumentsprovider.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

// ref: https://satoshun.github.io/2020/01/fragment-view-memory-leak/
//      https://satoshun.github.io/2019/04/jetpack-coroutine-support/

fun <T : ViewDataBinding> AppCompatActivity.viewBinding(): ReadOnlyProperty<AppCompatActivity, T?> {
    return ReadOnlyProperty<AppCompatActivity, T?> { thisRef, _ ->
        val view = thisRef.findViewById<ViewGroup>(android.R.id.content)[0]
        DataBindingUtil.bind<T>(view)?.also {
            it.lifecycleOwner = thisRef
        }
    }
}

fun <T : ViewDataBinding> Fragment.viewBinding(): ReadOnlyProperty<Fragment, T?> {
    return object : ReadOnlyProperty<Fragment, T?> {
        override fun getValue(thisRef: Fragment, property: KProperty<*>): T? {
            val view = thisRef.view ?: return null
            return DataBindingUtil.bind<T>(view)?.also {
                it.lifecycleOwner = thisRef.viewLifecycleOwner
            }
        }
    }
}

/**
 * Collect flow
 */
fun <T> Flow<T>.collectIn(
    lifecycleOwner: LifecycleOwner,
    observer: (T) -> Unit,
) {
    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            this@collectIn.collect { observer(it) }
        }
    }
}

/**
 * Navigate
 */
fun Fragment.navigateSafe(directions: NavDirections) {
    val navController = findNavController()
    val actionId = directions.actionId
    val destinationId = navController.currentDestination?.getAction(actionId)?.destinationId
        ?: navController.graph.getAction(actionId)?.destinationId
    if (destinationId != null && navController.currentDestination?.id != destinationId) {
        navController.navigate(directions)
    }
}

fun Fragment.navigateBack(@IdRes destinationId: Int? = null, inclusive: Boolean = false) {
    if (destinationId == null) {
        findNavController().popBackStack()
    } else {
        findNavController().popBackStack(destinationId, inclusive)
    }
}

@BindingAdapter("visible")
fun View.isVisible(visible: Boolean) {
    this.isVisible = visible
}


/**
 * Show toast
 */
fun Context.toast(@StringRes textId: Int) {
    Toast.makeText(this, textId, Toast.LENGTH_SHORT).show()
}

/**
 * Show toast
 */
fun Context.toast(text: CharSequence) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}

/**
 * Show toast
 */
fun Fragment.toast(@StringRes textId: Int) {
    Toast.makeText(requireContext(), textId, Toast.LENGTH_SHORT).show()
}

/**
 * Show toast
 */
fun Fragment.toast(text: CharSequence) {
    Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
}


/**
 * Start loading animation
 */
fun MenuItem.startLoadingAnimation() {
    stopLoadingAnimation()
    setActionView(R.layout.layout_menu_item_reload)
    actionView?.animation = RotateAnimation(
        0.0f, 360.0f,
        Animation.RELATIVE_TO_SELF, 0.5f,
        Animation.RELATIVE_TO_SELF, 0.5f
    ).apply {
        duration = 1000
        repeatCount = Animation.INFINITE
        interpolator = LinearInterpolator()
        start()
    }
}

/**
 * Stop loading animation
 */
fun MenuItem.stopLoadingAnimation() {
    actionView?.animation?.cancel()
    actionView = null
}