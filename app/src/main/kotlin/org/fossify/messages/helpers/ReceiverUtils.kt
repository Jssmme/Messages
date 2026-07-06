package org.fossify.messages.helpers

import android.content.Context
import org.fossify.messages.extensions.config

object ReceiverUtils {

    fun isMessageFilteredOut(context: Context, body: String): Boolean {
        // Check whitelist keywords first - if matched, bypass blocked keywords
        for (whitelistKeyword in context.config.whitelistKeywords) {
            if (body.contains(whitelistKeyword, ignoreCase = true)) {
                return false
            }
        }

        for (blockedKeyword in context.config.blockedKeywords) {
            if (body.contains(blockedKeyword, ignoreCase = true)) {
                return true
            }
        }

        return false
    }
}
