package com.wa2c.android.cifsdocumentsprovider.presentation.ui.common

import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.wa2c.android.cifsdocumentsprovider.R
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * View binding
 */
fun <T: ViewDataBinding> Fragment.viewBinding(): ReadOnlyProperty<Fragment, T> {
    return object: ReadOnlyProperty<Fragment, T> {
        override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
            val view = thisRef.requireView()
            @Suppress("UNCHECKED_CAST")
            return (view.getTag(R.id.tag_binding) as? T) ?: let {
                val b = DataBindingUtil.bind<T>(view)!!
                b.lifecycleOwner = thisRef.viewLifecycleOwner
                view.setTag(R.id.tag_binding, b)
                b
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
