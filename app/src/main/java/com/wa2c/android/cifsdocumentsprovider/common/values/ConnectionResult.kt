package com.wa2c.android.cifsdocumentsprovider.common.values

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.wa2c.android.cifsdocumentsprovider.R

/**
 * Server connection result
 */
sealed class ConnectionResult {

    /** Icon resource ID */
    abstract val iconRes: Int
    /** Icon color resource ID */
    abstract val colorRes: Int
    /** Message string resource ID */
    abstract val messageRes: Int

    /**
     * Get message
     */
    abstract fun getMessage(context: Context): Spannable

    object Success: ConnectionResult() {

        override val iconRes: Int
            @DrawableRes
            get() = R.drawable.ic_check_ok

        override val colorRes: Int
            @ColorRes
            get() = R.color.ic_check_ok

        override val messageRes: Int
            @StringRes
            get() = R.string.edit_check_connection_ok_message

        override fun getMessage(context: Context): Spannable {
            val builder = SpannableStringBuilder()
            val message = context.getString(messageRes)
            builder.append(message)
            builder.setSpan(StyleSpan(Typeface.BOLD), 0, message.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            return builder
        }
    }

    data class Warning(
        val cause: Throwable = RuntimeException()
    ): ConnectionResult() {

        override val iconRes: Int
            @DrawableRes
            get() = R.drawable.ic_check_wn

        override val colorRes: Int
            @ColorRes
            get() = R.color.ic_check_wn

        override val messageRes: Int
            @StringRes
            get() = R.string.edit_check_connection_wn_message

        override fun getMessage(context: Context): Spannable {
            val builder = SpannableStringBuilder()
            val message = context.getString(messageRes)
            builder.append(message)
            builder.setSpan(StyleSpan(Typeface.BOLD), 0, message.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            val cause = cause.localizedMessage?.substringAfter(": ")
            if (!cause.isNullOrEmpty()) { builder.append("\n[$cause]") }
            return builder
        }
    }

    data class Failure(
        val cause: Throwable = RuntimeException()
    ): ConnectionResult() {

        override val iconRes: Int
            @DrawableRes
            get() = R.drawable.ic_check_ng

        override val colorRes: Int
            @ColorRes
            get() = R.color.ic_check_ng

        override val messageRes: Int
            @StringRes
            get() = R.string.edit_check_connection_ng_message

        override fun getMessage(context: Context): Spannable {
            val builder = SpannableStringBuilder()
            val message = context.getString(messageRes)
            builder.append(message)
            builder.setSpan(StyleSpan(Typeface.BOLD), 0, message.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            val cause = cause.localizedMessage?.substringAfter(": ")
            if (!cause.isNullOrEmpty()) { builder.append("\n[$cause]") }
            return builder
        }
    }

}