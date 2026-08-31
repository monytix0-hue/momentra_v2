package com.example.momentra.ui.shell.empty.group

import android.content.Context
import android.provider.ContactsContract
import com.example.momentra.R

internal data class DeviceContact(
    val id: String,
    val name: String,
    val subtitle: String,
    val photoUri: String?,
    val phone: String? = null,
    val email: String? = null,
    val avatarRes: Int? = null,
)

/** Figma 579:12741 demo rows when device contacts are unavailable. */
internal fun figmaDemoContacts(): List<DeviceContact> = listOf(
    DeviceContact(
        id = "demo-rahul",
        name = "Rahul Mehta",
        subtitle = "+91 98765 43210",
        photoUri = null,
        phone = "+91 98765 43210",
        avatarRes = R.drawable.gap_demo_rahul,
    ),
    DeviceContact(
        id = "demo-priya",
        name = "Priya Singh",
        subtitle = "priya.singh@gmail.com",
        photoUri = null,
        email = "priya.singh@gmail.com",
        avatarRes = R.drawable.gap_demo_priya,
    ),
    DeviceContact(
        id = "demo-kavita",
        name = "Kavita Joshi",
        subtitle = "kavita.j@yahoo.com",
        photoUri = null,
        email = "kavita.j@yahoo.com",
        avatarRes = R.drawable.gap_demo_kavita,
    ),
)

internal fun loadDeviceContacts(context: Context): List<DeviceContact> {
    val byId = linkedMapOf<String, DeviceContact>()
    val resolver = context.contentResolver

    resolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
        ),
        null,
        null,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE LOCALIZED ASC",
    )?.use { cursor ->
        val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
        val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val photoIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
        while (cursor.moveToNext()) {
            val id = cursor.getString(idIdx) ?: continue
            if (byId.containsKey(id)) continue
            val name = cursor.getString(nameIdx)?.trim().orEmpty()
            if (name.isEmpty()) continue
            byId[id] = DeviceContact(
                id = id,
                name = name,
                subtitle = cursor.getString(numIdx)?.trim().orEmpty(),
                photoUri = cursor.getString(photoIdx),
                phone = cursor.getString(numIdx)?.trim(),
            )
        }
    }

    resolver.query(
        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Email.CONTACT_ID,
            ContactsContract.CommonDataKinds.Email.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Email.ADDRESS,
            ContactsContract.CommonDataKinds.Email.PHOTO_URI,
        ),
        null,
        null,
        ContactsContract.CommonDataKinds.Email.DISPLAY_NAME + " COLLATE LOCALIZED ASC",
    )?.use { cursor ->
        val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
        val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME)
        val emailIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)
        val photoIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.PHOTO_URI)
        while (cursor.moveToNext()) {
            val id = cursor.getString(idIdx) ?: continue
            val name = cursor.getString(nameIdx)?.trim().orEmpty()
            val email = cursor.getString(emailIdx)?.trim().orEmpty()
            val existing = byId[id]
            when {
                existing != null && existing.subtitle.isBlank() && email.isNotBlank() -> {
                    byId[id] = existing.copy(subtitle = email, email = email)
                }
                existing != null && email.isNotBlank() && existing.email.isNullOrBlank() -> {
                    byId[id] = existing.copy(email = email)
                }
                existing == null && name.isNotBlank() -> {
                    byId[id] = DeviceContact(
                        id = id,
                        name = name,
                        subtitle = email,
                        photoUri = cursor.getString(photoIdx),
                        email = email,
                    )
                }
            }
        }
    }

    return byId.values.sortedBy { it.name.lowercase() }
}
