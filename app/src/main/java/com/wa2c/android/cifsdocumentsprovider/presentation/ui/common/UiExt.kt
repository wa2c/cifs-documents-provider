package com.wa2c.android.cifsdocumentsprovider.presentation.ui.common

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.wa2c.android.cifsdocumentsprovider.R
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

fun Fragment.navigateBack() {
    findNavController().popBackStack()
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

