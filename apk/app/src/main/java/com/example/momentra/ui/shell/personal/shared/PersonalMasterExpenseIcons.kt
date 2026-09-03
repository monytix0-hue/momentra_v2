package com.example.momentra.ui.shell.personal.shared

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size

/** Native Material Outlined icons for Master Expense UI chrome (categories + structural UI). */
object PersonalMasterExpenseIcons {
    fun categoryIcon(code: String): ImageVector = when (code.uppercase()) {
        "FOOD" -> Icons.Outlined.Restaurant
        "TRANSPORT" -> Icons.Outlined.DirectionsCar
        "SHOPPING" -> Icons.Outlined.ShoppingBag
        "CAFE" -> Icons.Outlined.LocalCafe
        "HEALTH" -> Icons.Outlined.MedicalServices
        "ENTERTAINMENT" -> Icons.Outlined.Movie
        "BILLS" -> Icons.Outlined.ReceiptLong
        else -> Icons.Outlined.Inventory2
    }

    enum class Chrome(val vector: ImageVector) {
        Header(Icons.Outlined.CreditCard),
        Info(Icons.Outlined.Info),
        Edit(Icons.Outlined.Edit),
        Amount(Icons.Outlined.CurrencyRupee),
        Calendar(Icons.Outlined.CalendarToday),
        Folder(Icons.Outlined.Folder),
        Back(Icons.AutoMirrored.Outlined.ArrowBack),
        ExpandUp(Icons.Outlined.KeyboardArrowUp),
        ExpandDown(Icons.Outlined.KeyboardArrowDown),
    }
}

@Composable
fun MeIcon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    size: Dp = 20.dp,
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint,
    )
}
