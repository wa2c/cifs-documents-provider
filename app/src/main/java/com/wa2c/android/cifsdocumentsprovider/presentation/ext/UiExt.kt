package com.wa2c.android.cifsdocumentsprovider.presentation.ext

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.format.Formatter
import android.text.style.StyleSpan
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
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
import com.wa2c.android.cifsdocumentsprovider.common.values.SendDataState
import com.wa2c.android.cifsdocumentsprovider.domain.model.SendData
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.labelRes

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
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
 * Main Coroutine Scope
 */
class MainCoroutineScope: CoroutineScope {
    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
}


/**
 * Collect flow
 */
fun <T> Flow<T>.collectIn(
    lifecycleOwner: LifecycleOwner,
    state: Lifecycle.State = Lifecycle.State.STARTED,
    observer: (T) -> Unit = { },
) {
    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(state) {
            this@collectIn.collect { observer(it) }
        }
    }
}

/**
 * StateFlow map
 */
fun <T, K> StateFlow<T>.mapState(
    viewModelScope: CoroutineScope,
    transform: (data: T) -> K
): StateFlow<K> {
    return map {
        transform(it)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, transform(this.value))
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

/**
 * Summary Text
 * ex. 10% [10MB/100MB] 1MB/s (1:00)
 */
fun SendData.getSummaryText(context: Context): String {
    return when (state) {
        SendDataState.PROGRESS -> {
            val sendSize = " (${Formatter.formatShortFileSize(context, progressSize)}/${Formatter.formatShortFileSize(context, size)})"
            val sendSpeed = "${Formatter.formatShortFileSize(context, bps)}/s (${DateUtils.formatElapsedTime(elapsedTime / 1000)})"
            "$progress% $sendSize $sendSpeed"
        }
        else -> {
            context.getString(state.labelRes)
        }
    }
}