package org.fossify.messages.helpers

import android.content.Context
import org.fossify.commons.extensions.getMyContactsCursor
import org.fossify.commons.helpers.ContactLookupResult
import org.fossify.commons.helpers.SimpleContactsHelper
import org.fossify.messages.extensions.config

object ReceiverUtils {

    fun isMessageFilteredOut(context: Context, body: String, address: String? = null): Boolean {
        // Contacts are always exempt from keyword filtering - bypass if sender is a contact
        if (address != null) {
            val privateCursor = context.getMyContactsCursor(favoritesOnly = false, withPhoneNumbersOnly = true)
            val result = SimpleContactsHelper(context).existsSync(address, privateCursor)
            if (result != ContactLookupResult.NotFound) {
                return false
            }
        }

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
