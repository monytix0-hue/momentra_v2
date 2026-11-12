package com.example.momentra.ui.shell.business.retail

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.momentra.data.api.OperatingActivityDto
import com.example.momentra.data.api.OperatingDocumentDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.theme.RetailRose
import java.io.File
import java.math.BigDecimal
import kotlin.math.roundToInt

/**
 * Harbor Mart thermal receipt — on-screen paper preview + WhatsApp PDF/JPG share.
 *
 * Style: narrow cream slip, charcoal tabular type, dashed rules, rose only on the
 * duplicate banner. Refs come from real SALE fields (`clientKey` / event id) — never
 * invent fiscal `HM-B-*` numbers. Client re-render until a receipt.pdf API exists.
 *
 * Logo is device-local branding keyed by company/moment until a store-branding API exists.
 * Primary upload lives in Retail Locations masters (Store branding); reprint / post&print
 * keep a lighter Replace that also enforces JPEG-only.
 */

internal const val RETAIL_LOGO_JPEG_ONLY_MESSAGE =
    "JPEG only — PNG, HEIC and WebP are not allowed"

internal sealed class RetailLogoPickResult {
    data object Saved : RetailLogoPickResult()
    data class Rejected(val message: String) : RetailLogoPickResult()
}

internal const val HARBOR_MART_DEMO_GSTIN = "36ADUFS9403L1Z9"

internal enum class RetailReceiptPaperSize(
    val label: String,
    val shortLabel: String,
    /** Typical printable width in millimetres. */
    val widthMm: Float,
) {
    THERMAL_2("2 inch · 58mm", "2\"", 58f),
    THERMAL_3("3 inch · 80mm", "3\"", 80f),
    THERMAL_4("4 inch · 112mm", "4\"", 112f),
    THERMAL_6("6 inch · 152mm", "6\"", 152f),
    A4("A4 · 210mm", "A4", 210f);

    companion object {
        val Default = THERMAL_3
    }

    /** On-screen preview column width (dp). A4 centres a thermal-like slip. */
    val previewContentWidthDp: Dp
        get() = when (this) {
            THERMAL_2 -> 200.dp
            THERMAL_3 -> 280.dp
            THERMAL_4 -> 340.dp
            THERMAL_6 -> 400.dp
            A4 -> 320.dp
        }

    val previewFrameWidthDp: Dp
        get() = when (this) {
            A4 -> 460.dp
            else -> previewContentWidthDp
        }

    fun pageWidthPt(dpi: Int = 72): Int =
        ((widthMm / 25.4f) * dpi).roundToInt().coerceAtLeast(164)

    fun pageHeightPt(contentHeightPx: Int, contentWidthPx: Int, dpi: Int = 72): Int {
        if (this == A4) return ((297f / 25.4f) * dpi).roundToInt()
        val scale = pageWidthPt(dpi).toFloat() / contentWidthPx.coerceAtLeast(1)
        return (contentHeightPx * scale).roundToInt().coerceAtLeast(pageWidthPt(dpi))
    }
}

internal enum class RetailReceiptShareFormat(val label: String, val mime: String) {
    JPG("JPG", "image/jpeg"),
    PDF("PDF", "application/pdf"),
}

internal data class RetailReceiptLine(
    val name: String,
    val qty: String? = null,
    val rate: String? = null,
    val amount: String? = null,
    val meta: String? = null,
)

internal data class RetailReceiptModel(
    val storeName: String,
    /** Deprecated marketing line — kept blank; header uses location + GSTIN + cashier. */
    val tagline: String = "",
    val locationLine: String?,
    /** Store GSTIN when known; omit line when null. */
    val gstin: String?,
    /** Person who posted / entered the invoice. */
    val enteredBy: String? = null,
    val occurredAt: String?,
    /** Bill / client key. Blank when not yet assigned — Ref row omitted. */
    val refLabel: String,
    val duplicate: Boolean,
    val lines: List<RetailReceiptLine>,
    /** Quick amount / activity-only sales — amount without inventing SKU rows. */
    val amountOnly: Boolean,
    val amountOnlyTitle: String?,
    val subtotal: String?,
    val gstEstimate: String?,
    val tender: String?,
    val total: String?,
    val currency: String,
    val reason: String? = null,
    val footerHonesty: String =
        "Client duplicate · receipt.pdf API not allocated yet",
)

internal object RetailReceiptPaper {
    val cream = Color(0xFFFFFBF5)
    val ink = Color(0xFF1C1917)
    val muted = Color(0xFF57534E)
    val rule = Color(0xFFA8A29E)
    val roseBanner = RetailRose.rose
}

/** Device-local JPEG logo keyed by companyId/momentId — not a server branding API. */
internal object RetailReceiptLogoStore {
    private const val PREFS = "retail_receipt_logo"
    private const val DIR = "receipt_logos"
    private val JPEG_MIME = setOf("image/jpeg", "image/jpg")
    private val JPEG_EXT = setOf("jpg", "jpeg")

    private fun scopeKey(companyId: String?, momentId: String?): String {
        val company = companyId?.takeIf { it.isNotBlank() } ?: "_"
        val moment = momentId?.takeIf { it.isNotBlank() } ?: "_session"
        return "${company}__$moment"
    }

    private fun file(context: Context, companyId: String?, momentId: String?): File {
        val dir = File(context.filesDir, DIR).also { it.mkdirs() }
        return File(dir, "${scopeKey(companyId, momentId)}.jpg")
    }

    fun loadBitmap(context: Context, companyId: String?, momentId: String?): Bitmap? {
        val f = file(context, companyId, momentId)
        if (!f.exists()) return null
        return runCatching { BitmapFactory.decodeFile(f.absolutePath) }.getOrNull()
    }

    fun isJpegUri(context: Context, uri: Uri): Boolean {
        val mime = context.contentResolver.getType(uri)?.lowercase()?.substringBefore(';')
        if (mime != null && mime !in JPEG_MIME) return false
        val name = uri.lastPathSegment?.lowercase().orEmpty()
        val ext = name.substringAfterLast('.', missingDelimiterValue = "")
        if (ext.isNotEmpty() && ext !in JPEG_EXT) return false
        val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
            val header = ByteArray(3)
            val read = stream.read(header)
            if (read < 3) null else header
        } ?: return false
        return isJpegMagic(bytes)
    }

    fun isJpegMagic(bytes: ByteArray): Boolean =
        bytes.size >= 3 &&
            bytes[0] == 0xFF.toByte() &&
            bytes[1] == 0xD8.toByte() &&
            bytes[2] == 0xFF.toByte()

    fun saveFromUri(
        context: Context,
        companyId: String?,
        momentId: String?,
        uri: Uri,
    ): RetailLogoPickResult {
        if (!isJpegUri(context, uri)) {
            return RetailLogoPickResult.Rejected(RETAIL_LOGO_JPEG_ONLY_MESSAGE)
        }
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return RetailLogoPickResult.Rejected("Could not open that photo")
        if (!isJpegMagic(bytes)) {
            return RetailLogoPickResult.Rejected(RETAIL_LOGO_JPEG_ONLY_MESSAGE)
        }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: return RetailLogoPickResult.Rejected("Could not open that photo")
        val sized = scaleLogo(decoded, 512)
        val out = file(context, companyId, momentId)
        out.outputStream().use { stream ->
            sized.compress(Bitmap.CompressFormat.JPEG, 88, stream)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(scopeKey(companyId, momentId), out.absolutePath)
            .apply()
        return RetailLogoPickResult.Saved
    }

    fun clear(context: Context, companyId: String?, momentId: String?) {
        file(context, companyId, momentId).delete()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(scopeKey(companyId, momentId))
            .apply()
    }

    private fun scaleLogo(src: Bitmap, maxEdge: Int): Bitmap {
        val w = src.width.coerceAtLeast(1)
        val h = src.height.coerceAtLeast(1)
        val scale = maxEdge.toFloat() / maxOf(w, h).toFloat()
        if (scale >= 1f) return src
        return Bitmap.createScaledBitmap(
            src,
            (w * scale).roundToInt().coerceAtLeast(1),
            (h * scale).roundToInt().coerceAtLeast(1),
            true,
        )
    }
}

internal fun retailStoreDisplayName(model: OperatingProjectionDto): String =
    model.title?.takeIf { it.isNotBlank() } ?: "Store"

/** Setup location only — no Harbor demo fallback. */
internal fun retailStoreLocationLine(model: OperatingProjectionDto): String? =
    model.location?.takeIf { it.isNotBlank() }

/**
 * Prefer setup [OperatingProjectionDto.gstin], then a master GSTIN.
 * Harbor demo GSTIN only when the store title is still Harbor Mart.
 */
internal fun retailReceiptGstin(model: OperatingProjectionDto): String? {
    val fromSetup = model.gstin?.trim()?.uppercase()?.takeIf { looksLikeGstin(it) }
    if (fromSetup != null) return fromSetup
    val fromReg = model.gstRegistration?.trim()?.uppercase()?.takeIf { looksLikeGstin(it) }
    if (fromReg != null) return fromReg
    val fromMaster = model.masters.orEmpty().asSequence()
        .mapNotNull { it.gstin?.trim()?.uppercase()?.takeIf { g -> looksLikeGstin(g) } }
        .firstOrNull()
    if (fromMaster != null) return fromMaster
    val display = retailStoreDisplayName(model)
    return if (display.contains("Harbor Mart", ignoreCase = true)) HARBOR_MART_DEMO_GSTIN else null
}

/** Cashier / invoice entry person from Firebase or cached Momentra identity. */
internal fun retailReceiptEnteredBy(context: android.content.Context?): String? {
    val user = runCatching { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }.getOrNull()
        ?: return null
    user.displayName?.takeIf { it.isNotBlank() }?.let { return it }
    val uid = user.uid
    if (context != null) {
        val cached = runCatching {
            com.example.momentra.data.local.AppPreferences(context).getCachedIdentity(uid)?.second
        }.getOrNull()?.takeIf { it.isNotBlank() }
        if (cached != null) return cached
    }
    return user.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
}

private fun looksLikeGstin(value: String): Boolean =
    value.length == 15 && value.all { it.isLetterOrDigit() } &&
        value.take(2).all { it.isDigit() }

internal fun retailReceiptRef(
    clientKey: String?,
    eventId: String?,
): String {
    clientKey?.takeIf { it.isNotBlank() }?.let { return it }
    eventId?.takeIf { it.isNotBlank() }?.let { return "Sale · $it" }
    return "Posted sale · no bill no."
}

internal fun buildRetailReceiptFromSale(
    model: OperatingProjectionDto,
    sale: OperatingActivityDto,
    document: OperatingDocumentDto? = null,
    duplicate: Boolean = true,
    reason: String? = null,
    tenderOverride: String? = null,
    enteredBy: String? = null,
): RetailReceiptModel {
    val currency = model.currency?.takeIf { it.isNotBlank() } ?: "INR"
    val docLines = document?.lines.orEmpty()
    val lines = if (docLines.isNotEmpty()) {
        docLines.map { line ->
            val qty = line.quantity?.takeIf { it.isNotBlank() }
            val rate = line.unitPrice?.takeIf { it.isNotBlank() }
            val qtyBd = qty?.toBigDecimalOrNull()
            val rateBd = rate?.toBigDecimalOrNull()
            val amount = when {
                qtyBd != null && rateBd != null -> qtyBd.multiply(rateBd).stripTrailingZeros().toPlainString()
                else -> rate
            }
            RetailReceiptLine(
                name = line.itemName.ifBlank { line.masterName ?: "Line" },
                qty = qty,
                rate = rate?.let { RetailFormat.exact(currency, it) },
                amount = amount?.let { RetailFormat.exact(currency, it) },
                meta = listOfNotNull(
                    line.unit?.takeIf { it.isNotBlank() },
                    line.gstRate?.takeIf { it.isNotBlank() }?.let { "GST $it%" },
                ).joinToString(" · ").ifBlank { null },
            )
        }
    } else {
        emptyList()
    }
    val amountOnly = lines.isEmpty()
    val totalRaw = document?.grandTotal?.takeIf { it.isNotBlank() }
        ?: sale.amount?.takeIf { it.isNotBlank() }
    val subtotalRaw = document?.subTotal?.takeIf { it.isNotBlank() }
        ?: totalRaw.takeIf { !amountOnly }
    val gstRaw = document?.tax?.let { tax ->
        tax.total?.takeIf { it.isNotBlank() }
            ?: tax.cgst?.toBigDecimalOrNull()?.let { cgst ->
                val sgst = tax.sgst?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                cgst.add(sgst).stripTrailingZeros().toPlainString()
            }
    }
    val tender = tenderOverride
        ?: sale.paymentStatus?.takeIf { it.isNotBlank() }
        ?: sale.note?.let { note ->
            Regex("""Tender:\s*([^·]+)""").find(note)?.groupValues?.getOrNull(1)?.trim()
        }
    return RetailReceiptModel(
        storeName = retailStoreDisplayName(model),
        tagline = "",
        locationLine = retailStoreLocationLine(model),
        gstin = retailReceiptGstin(model),
        enteredBy = enteredBy,
        occurredAt = sale.occurredAt.takeIf { it.isNotBlank() },
        refLabel = retailReceiptRef(sale.clientKey, sale.id),
        duplicate = duplicate,
        lines = lines,
        amountOnly = amountOnly,
        amountOnlyTitle = if (amountOnly) {
            sale.title.ifBlank { "Quick amount sale" }
        } else {
            null
        },
        subtotal = subtotalRaw?.let { RetailFormat.exact(currency, it) },
        gstEstimate = gstRaw?.let { RetailFormat.exact(currency, it) }?.let { "GST est. $it" },
        tender = tender,
        total = totalRaw?.let { RetailFormat.exact(currency, it) },
        currency = currency,
        reason = reason,
    )
}

internal fun buildRetailReceiptFromPosCart(
    model: OperatingProjectionDto,
    title: String,
    lines: List<RetailPosLine>,
    totalPlain: String,
    tender: String,
    customer: String?,
    quickAmountOnly: Boolean,
    gstEstimatePlain: String? = null,
    enteredBy: String? = null,
): RetailReceiptModel {
    val currency = model.currency?.takeIf { it.isNotBlank() } ?: "INR"
    val receiptLines = if (quickAmountOnly || lines.isEmpty()) {
        emptyList()
    } else {
        lines.map { line ->
            RetailReceiptLine(
                name = line.name,
                qty = line.quantity.toString(),
                rate = RetailFormat.exact(currency, line.unitPrice),
                amount = RetailFormat.exact(
                    currency,
                    line.amount().stripTrailingZeros().toPlainString(),
                ),
                meta = line.gstRate?.takeIf { it.isNotBlank() }?.let { "GST $it" },
            )
        }
    }
    return RetailReceiptModel(
        storeName = retailStoreDisplayName(model),
        tagline = "",
        locationLine = retailStoreLocationLine(model),
        gstin = retailReceiptGstin(model),
        enteredBy = enteredBy,
        occurredAt = null,
        // No placeholder bill key — Ref row omitted until clientKey exists.
        refLabel = "",
        duplicate = false,
        lines = receiptLines,
        amountOnly = receiptLines.isEmpty(),
        amountOnlyTitle = if (receiptLines.isEmpty()) title else null,
        subtotal = if (receiptLines.isEmpty()) null else RetailFormat.exact(currency, totalPlain),
        gstEstimate = gstEstimatePlain?.let { "GST est. ${RetailFormat.exact(currency, it)}" },
        tender = tender.takeIf { it.isNotBlank() },
        total = RetailFormat.exact(currency, totalPlain),
        currency = currency,
        footerHonesty = listOfNotNull(
            customer?.takeIf { it.isNotBlank() }?.let { "Customer · $it" },
            "Client copy · receipt.pdf API not allocated yet",
        ).joinToString(" · "),
    )
}

internal fun retailReceiptShareText(model: RetailReceiptModel): String = buildString {
    val rule = "-".repeat(32)
    appendLine(model.storeName.uppercase())
    model.locationLine?.let { appendLine(it) }
    model.gstin?.let { appendLine("GSTIN : $it") }
    model.enteredBy?.takeIf { it.isNotBlank() }?.let { appendLine("Entered by : $it") }
    appendLine(rule)
    if (model.duplicate) {
        appendLine("DUPLICATE COPY")
    } else {
        appendLine("CUSTOMER COPY")
    }
    appendLine(rule)
    if (model.refLabel.isNotBlank()) appendLine("Ref ${model.refLabel}")
    model.occurredAt?.let { appendLine("When $it") }
    appendLine(rule)
    if (model.amountOnly) {
        appendLine(model.amountOnlyTitle ?: "Amount sale")
        appendLine("(No SKU lines · amount only)")
        model.total?.let { appendLine("TOTAL".padEnd(20) + it) }
    } else {
        appendLine(padCols("ITEM", "QTY", "AMT"))
        appendLine(rule)
        model.lines.forEach { line ->
            appendLine(padCols(line.name.take(18), line.qty ?: "—", line.amount ?: "—"))
            line.meta?.let { appendLine("  $it") }
        }
        appendLine(rule)
        model.subtotal?.let { appendLine(padCols("Subtotal", "", it)) }
        model.gstEstimate?.let { appendLine(" $it") }
        model.tender?.let { appendLine("Tender $it") }
        model.total?.let { appendLine(padCols("TOTAL", "", it)) }
    }
    model.reason?.takeIf { it.isNotBlank() }?.let {
        appendLine(rule)
        appendLine("Reprint $it")
    }
    appendLine(rule)
    appendLine("Thank you for shopping with ${model.storeName}")
    appendLine(model.footerHonesty)
}

private fun padCols(a: String, b: String, c: String): String {
    val left = a.padEnd(18).take(18)
    val mid = b.padStart(4).take(4)
    val right = c.padStart(10).take(10)
    return "$left $mid $right"
}

internal fun shareRetailReceipt(
    context: Context,
    model: RetailReceiptModel,
) {
    val body = retailReceiptShareText(model)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(
            Intent.EXTRA_SUBJECT,
            "${if (model.duplicate) "Duplicate" else "Customer"} receipt · ${model.refLabel}",
        )
        putExtra(Intent.EXTRA_TEXT, body)
    }
    context.startActivity(Intent.createChooser(intent, "Share receipt"))
}

internal fun shareRetailReceiptFile(
    context: Context,
    model: RetailReceiptModel,
    paper: RetailReceiptPaperSize,
    format: RetailReceiptShareFormat,
    logo: Bitmap?,
    toWhatsApp: Boolean,
) {
    val file = when (format) {
        RetailReceiptShareFormat.JPG -> RetailReceiptExporter.writeJpeg(context, model, paper, logo)
        RetailReceiptShareFormat.PDF -> RetailReceiptExporter.writePdf(context, model, paper, logo)
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val caption = retailReceiptShareText(model)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = format.mime
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, caption)
        putExtra(
            Intent.EXTRA_SUBJECT,
            "${if (model.duplicate) "Duplicate" else "Customer"} receipt · ${model.refLabel}",
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = android.content.ClipData.newUri(context.contentResolver, "receipt", uri)
    }
    if (toWhatsApp) {
        val pkgs = listOf("com.whatsapp", "com.whatsapp.w4b")
        for (pkg in pkgs) {
            intent.setPackage(pkg)
            if (intent.resolveActivity(context.packageManager) != null) {
                runCatching {
                    context.startActivity(intent)
                    return
                }
            }
        }
        intent.setPackage(null)
    }
    context.startActivity(Intent.createChooser(intent, "Share receipt"))
}

internal object RetailReceiptExporter {
    private const val EXPORT_DPI = 203 // typical thermal density

    fun writeJpeg(
        context: Context,
        model: RetailReceiptModel,
        paper: RetailReceiptPaperSize,
        logo: Bitmap?,
    ): File {
        val bitmap = renderBitmap(model, paper, logo)
        val file = File(context.cacheDir, "harbor-mart-receipt-${System.currentTimeMillis()}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        return file
    }

    fun writePdf(
        context: Context,
        model: RetailReceiptModel,
        paper: RetailReceiptPaperSize,
        logo: Bitmap?,
    ): File {
        val bitmap = renderBitmap(model, paper, logo)
        val pageW = paper.pageWidthPt(72)
        val pageH = if (paper == RetailReceiptPaperSize.A4) {
            ((297f / 25.4f) * 72f).roundToInt()
        } else {
            val scale = pageW.toFloat() / bitmap.width.coerceAtLeast(1)
            (bitmap.height * scale).roundToInt().coerceAtLeast(pageW)
        }
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas
        canvas.drawColor(AndroidColor.rgb(255, 251, 245))
        val destW: Float
        val destH: Float
        val left: Float
        val top: Float
        if (paper == RetailReceiptPaperSize.A4) {
            destW = pageW * 0.62f
            destH = bitmap.height * (destW / bitmap.width)
            left = (pageW - destW) / 2f
            top = 36f
        } else {
            destW = pageW.toFloat()
            destH = pageH.toFloat()
            left = 0f
            top = 0f
        }
        canvas.drawBitmap(
            bitmap,
            null,
            android.graphics.RectF(left, top, left + destW, top + destH),
            null,
        )
        doc.finishPage(page)
        val file = File(context.cacheDir, "harbor-mart-receipt-${System.currentTimeMillis()}.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun renderBitmap(
        model: RetailReceiptModel,
        paper: RetailReceiptPaperSize,
        logo: Bitmap?,
    ): Bitmap {
        val widthPx = ((paper.widthMm / 25.4f) * EXPORT_DPI).roundToInt()
            .coerceIn(320, 1600)
        val contentWidth = if (paper == RetailReceiptPaperSize.A4) {
            ((80f / 25.4f) * EXPORT_DPI).roundToInt()
        } else {
            widthPx
        }
        val pad = (contentWidth * 0.06f).roundToInt()
        val ink = AndroidColor.parseColor("#1C1917")
        val muted = AndroidColor.parseColor("#57534E")
        val rose = AndroidColor.parseColor("#F43F5E")
        val cream = AndroidColor.parseColor("#FFFBF5")
        val rule = AndroidColor.parseColor("#A8A29E")

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            textSize = contentWidth * 0.055f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            textSize = contentWidth * 0.038f
            typeface = Typeface.MONOSPACE
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = muted
            textSize = contentWidth * 0.034f
            typeface = Typeface.MONOSPACE
        }
        val rosePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = rose
            textSize = contentWidth * 0.034f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            textSize = contentWidth * 0.042f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val lines = buildExportLines(model)
        val lineH = bodyPaint.textSize * 1.45f
        val logoH = if (logo != null) contentWidth * 0.18f else 0f
        val heightEstimate = (
            pad * 2 + logoH + titlePaint.textSize * 2.2f +
                lines.size * lineH + contentWidth * 0.12f
            ).roundToInt().coerceAtLeast(contentWidth)

        val bitmap = Bitmap.createBitmap(widthPx, heightEstimate, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(cream)
        val left = ((widthPx - contentWidth) / 2f) + pad
        val right = ((widthPx + contentWidth) / 2f) - pad
        val centerX = widthPx / 2f
        var y = pad.toFloat()

        logo?.let { bmp ->
            val maxW = contentWidth * 0.34f
            val maxH = contentWidth * 0.16f
            val scale = minOf(maxW / bmp.width, maxH / bmp.height)
            val dw = bmp.width * scale
            val dh = bmp.height * scale
            val dst = android.graphics.RectF(
                centerX - dw / 2f,
                y,
                centerX + dw / 2f,
                y + dh,
            )
            canvas.drawBitmap(bmp, null, dst, null)
            y += dh + lineH * 0.4f
        }

        canvas.drawText(model.storeName.uppercase(), centerX, y + titlePaint.textSize, titlePaint)
        y += titlePaint.textSize * 1.35f
        model.locationLine?.let {
            mutedPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(it, centerX, y + mutedPaint.textSize, mutedPaint)
            mutedPaint.textAlign = Paint.Align.LEFT
            y += mutedPaint.textSize * 1.3f
        }
        model.gstin?.let {
            mutedPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("GSTIN : $it", centerX, y + mutedPaint.textSize, mutedPaint)
            mutedPaint.textAlign = Paint.Align.LEFT
            y += mutedPaint.textSize * 1.35f
        }
        model.enteredBy?.takeIf { it.isNotBlank() }?.let {
            mutedPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("Entered by : $it", centerX, y + mutedPaint.textSize, mutedPaint)
            mutedPaint.textAlign = Paint.Align.LEFT
            y += mutedPaint.textSize * 1.35f
        }

        y = drawDash(canvas, left, right, y, rule)
        // banner
        val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (model.duplicate) AndroidColor.argb(36, 244, 63, 94)
            else AndroidColor.argb(46, 251, 191, 36)
        }
        val bannerH = boldPaint.textSize * 1.6f
        canvas.drawRoundRect(left, y, right, y + bannerH, 6f, 6f, bannerPaint)
        val bannerText = if (model.duplicate) "DUPLICATE COPY" else "CUSTOMER COPY"
        val bannerColor = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (model.duplicate) rose else ink
            textSize = boldPaint.textSize
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(bannerText, centerX, y + bannerH * 0.72f, bannerColor)
        y += bannerH + lineH * 0.35f
        y = drawDash(canvas, left, right, y, rule)

        fun drawKv(label: String, value: String, emphatic: Boolean = false) {
            val lp = if (emphatic) boldPaint else mutedPaint
            val vp = if (emphatic) boldPaint else bodyPaint
            canvas.drawText(label, left, y + lp.textSize, lp)
            vp.textAlign = Paint.Align.RIGHT
            canvas.drawText(value, right, y + vp.textSize, vp)
            vp.textAlign = Paint.Align.LEFT
            y += lineH
        }

        if (model.refLabel.isNotBlank()) drawKv("Ref", model.refLabel)
        model.occurredAt?.let { drawKv("When", it.take(16)) }
        y = drawDash(canvas, left, right, y, rule)

        if (model.amountOnly) {
            bodyPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                model.amountOnlyTitle ?: "Amount sale",
                centerX,
                y + bodyPaint.textSize,
                bodyPaint,
            )
            bodyPaint.textAlign = Paint.Align.LEFT
            y += lineH
            mutedPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("No SKU lines · amount only", centerX, y + mutedPaint.textSize, mutedPaint)
            mutedPaint.textAlign = Paint.Align.LEFT
            y += lineH
            model.total?.let { drawKv("TOTAL", it, emphatic = true) }
        } else {
            mutedPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("ITEM", left, y + mutedPaint.textSize, mutedPaint)
            mutedPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("AMT", right, y + mutedPaint.textSize, mutedPaint)
            canvas.drawText("QTY", right - contentWidth * 0.22f, y + mutedPaint.textSize, mutedPaint)
            mutedPaint.textAlign = Paint.Align.LEFT
            y += lineH
            y = drawDash(canvas, left, right, y, rule)
            model.lines.forEach { line ->
                canvas.drawText(line.name.take(22), left, y + bodyPaint.textSize, bodyPaint)
                bodyPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText(line.amount ?: "—", right, y + bodyPaint.textSize, bodyPaint)
                canvas.drawText(
                    line.qty ?: "—",
                    right - contentWidth * 0.22f,
                    y + bodyPaint.textSize,
                    bodyPaint,
                )
                bodyPaint.textAlign = Paint.Align.LEFT
                y += lineH
                line.meta?.let {
                    canvas.drawText(it, left, y + mutedPaint.textSize, mutedPaint)
                    y += lineH * 0.9f
                }
            }
            y = drawDash(canvas, left, right, y, rule)
            model.subtotal?.let { drawKv("Subtotal", it) }
            model.gstEstimate?.let {
                mutedPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText(it, right, y + mutedPaint.textSize, mutedPaint)
                mutedPaint.textAlign = Paint.Align.LEFT
                y += lineH
            }
            model.tender?.let { drawKv("Tender", it) }
            model.total?.let { drawKv("TOTAL", it, emphatic = true) }
        }
        model.reason?.takeIf { it.isNotBlank() }?.let {
            y = drawDash(canvas, left, right, y, rule)
            drawKv("Reprint", it)
        }
        y = drawDash(canvas, left, right, y, rule)
        bodyPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            "Thank you for shopping with ${model.storeName}",
            centerX,
            y + bodyPaint.textSize,
            bodyPaint,
        )
        y += lineH
        mutedPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(model.footerHonesty, centerX, y + mutedPaint.textSize, mutedPaint)
        mutedPaint.textAlign = Paint.Align.LEFT
        bodyPaint.textAlign = Paint.Align.LEFT
        return bitmap
    }

    private fun buildExportLines(model: RetailReceiptModel): List<String> {
        val out = mutableListOf<String>()
        out += model.storeName
        model.locationLine?.let { out += it }
        model.gstin?.let { out += it }
        model.enteredBy?.takeIf { it.isNotBlank() }?.let { out += it }
        out += if (model.duplicate) "DUPLICATE" else "CUSTOMER"
        if (model.refLabel.isNotBlank()) out += model.refLabel
        model.occurredAt?.let { out += it }
        if (model.amountOnly) {
            out += model.amountOnlyTitle.orEmpty()
            model.total?.let { out += it }
        } else {
            out += model.lines.map { it.name }
            model.subtotal?.let { out += it }
            model.gstEstimate?.let { out += it }
            model.tender?.let { out += it }
            model.total?.let { out += it }
        }
        model.reason?.let { out += it }
        out += model.footerHonesty
        return out
    }

    private fun drawDash(
        canvas: Canvas,
        left: Float,
        right: Float,
        y: Float,
        color: Int,
    ): Float {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            strokeWidth = 2f
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 8f), 0f)
        }
        val yy = y + 8f
        canvas.drawLine(left, yy, right, yy, paint)
        return yy + 14f
    }
}

@Composable
internal fun RetailReceiptPreview(
    model: RetailReceiptModel,
    modifier: Modifier = Modifier,
    paper: RetailReceiptPaperSize = RetailReceiptPaperSize.Default,
    logo: Bitmap? = null,
) {
    val frameMod = modifier
        .fillMaxWidth()
        .widthIn(max = paper.previewFrameWidthDp)
        .clip(RoundedCornerShape(12.dp))
        .background(RetailReceiptPaper.cream)
        .border(1.dp, RetailRose.rose.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
        .padding(
            horizontal = if (paper == RetailReceiptPaperSize.A4) 18.dp else 0.dp,
            vertical = if (paper == RetailReceiptPaperSize.A4) 18.dp else 0.dp,
        )
    Column(
        frameMod,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier
                .widthIn(max = paper.previewContentWidthDp)
                .padding(horizontal = 14.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            logo?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Store logo",
                    modifier = Modifier
                        .size(
                            width = when (paper) {
                                RetailReceiptPaperSize.THERMAL_2 -> 56.dp
                                RetailReceiptPaperSize.THERMAL_3 -> 72.dp
                                else -> 88.dp
                            },
                            height = when (paper) {
                                RetailReceiptPaperSize.THERMAL_2 -> 40.dp
                                RetailReceiptPaperSize.THERMAL_3 -> 52.dp
                                else -> 64.dp
                            },
                        )
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Fit,
                )
            }
            Text(
                model.storeName.uppercase(),
                color = RetailReceiptPaper.ink,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = if (paper == RetailReceiptPaperSize.THERMAL_2) 13.sp else 15.sp,
                textAlign = TextAlign.Center,
                letterSpacing = 1.2.sp,
            )
            model.locationLine?.let {
                Text(
                    it,
                    color = RetailReceiptPaper.muted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                )
            }
            model.gstin?.let {
                Text(
                    "GSTIN : $it",
                    color = RetailReceiptPaper.muted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                )
            }
            model.enteredBy?.takeIf { it.isNotBlank() }?.let {
                Text(
                    "Entered by : $it",
                    color = RetailReceiptPaper.muted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                )
            }
            ReceiptDash()
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (model.duplicate) RetailReceiptPaper.roseBanner.copy(alpha = 0.14f)
                        else RetailRose.amber.copy(alpha = 0.18f),
                    )
                    .padding(vertical = 5.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (model.duplicate) "DUPLICATE COPY" else "CUSTOMER COPY",
                    color = if (model.duplicate) RetailRose.rose else RetailReceiptPaper.ink,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.4.sp,
                )
            }
            ReceiptDash()
            if (model.refLabel.isNotBlank()) ReceiptKv("Ref", model.refLabel)
            model.occurredAt?.let { ReceiptKv("When", it.take(16)) }
            ReceiptDash()
            if (model.amountOnly) {
                Text(
                    model.amountOnlyTitle ?: "Amount sale",
                    color = RetailReceiptPaper.ink,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "No SKU lines · amount only",
                    color = RetailReceiptPaper.muted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                model.total?.let {
                    ReceiptKv("TOTAL", it, emphatic = true)
                }
            } else {
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        "ITEM",
                        Modifier.weight(1f),
                        color = RetailReceiptPaper.muted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                    )
                    Text(
                        "QTY",
                        Modifier.padding(horizontal = 4.dp),
                        color = RetailReceiptPaper.muted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                    )
                    Text(
                        "AMT",
                        color = RetailReceiptPaper.muted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        textAlign = TextAlign.End,
                    )
                }
                ReceiptDash()
                model.lines.forEach { line ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                line.name,
                                color = RetailReceiptPaper.ink,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            line.meta?.let { meta ->
                                Text(
                                    meta,
                                    color = RetailReceiptPaper.muted,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                )
                            }
                        }
                        Text(
                            line.qty ?: "—",
                            Modifier.padding(horizontal = 6.dp),
                            color = RetailReceiptPaper.ink,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                        )
                        Text(
                            line.amount ?: "—",
                            color = RetailReceiptPaper.ink,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            textAlign = TextAlign.End,
                        )
                    }
                }
                ReceiptDash()
                model.subtotal?.let { ReceiptKv("Subtotal", it) }
                model.gstEstimate?.let {
                    Text(
                        it,
                        color = RetailReceiptPaper.muted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                    )
                }
                model.tender?.let { ReceiptKv("Tender", it) }
                model.total?.let { ReceiptKv("TOTAL", it, emphatic = true) }
            }
            model.reason?.takeIf { it.isNotBlank() }?.let {
                ReceiptDash()
                ReceiptKv("Reprint", it)
            }
            ReceiptDash()
            Text(
                "Thank you for shopping with ${model.storeName}",
                color = RetailReceiptPaper.ink,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                model.footerHonesty,
                color = RetailReceiptPaper.muted,
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Primary store-logo setup control — used from Retail Locations masters.
 * JPEG/JPG only; preview + Replace / Remove; persists via [RetailReceiptLogoStore].
 */
@Composable
internal fun RetailStoreBrandingCard(
    companyId: String?,
    momentId: String?,
    modifier: Modifier = Modifier,
    onMessage: (String) -> Unit = {},
) {
    val context = LocalContext.current
    var logoTick by remember { mutableIntStateOf(0) }
    val logo = remember(companyId, momentId, logoTick) {
        RetailReceiptLogoStore.loadBitmap(context, companyId, momentId)
    }
    val pickLogo = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        when (val result = RetailReceiptLogoStore.saveFromUri(context, companyId, momentId, uri)) {
            is RetailLogoPickResult.Saved -> {
                logoTick += 1
                onMessage("Receipt logo saved · JPEG")
            }
            is RetailLogoPickResult.Rejected -> onMessage(result.message)
        }
    }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(RetailRose.chip)
            .border(1.dp, RetailRose.roseBorder.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Store branding",
            color = RetailRose.textPrimary,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
        )
        Text(
            "Receipt logo · JPEG only · stays on this device",
            color = RetailRose.textFaint,
            fontFamily = PlusJakartaSans,
            fontSize = 12.sp,
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(RetailRose.cardSlate)
                    .border(1.dp, RetailRose.chipStroke, RoundedCornerShape(10.dp))
                    .clickable {
                        pickLogo.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.SingleMimeType("image/jpeg"),
                            ),
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (logo != null) {
                    Image(
                        bitmap = logo.asImageBitmap(),
                        contentDescription = "Store logo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        "JPEG",
                        color = RetailRose.textFaint,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                    )
                }
            }
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                RoseModeChip(
                    text = if (logo != null) "Replace logo" else "Upload JPEG logo",
                    selected = false,
                    onClick = {
                        pickLogo.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.SingleMimeType("image/jpeg"),
                            ),
                        )
                    },
                )
                if (logo != null) {
                    RoseModeChip(
                        text = "Remove logo",
                        selected = false,
                        onClick = {
                            RetailReceiptLogoStore.clear(context, companyId, momentId)
                            logoTick += 1
                            onMessage("Receipt logo removed")
                        },
                    )
                }
            }
        }
    }
}

/**
 * Size / format / logo / WhatsApp controls for reprint + post&print flows.
 * Lighter Replace remains here; primary upload is [RetailStoreBrandingCard] in Locations setup.
 * Logo is device-local until a store-branding API exists. JPEG only.
 */
@Composable
internal fun RetailReceiptShareControls(
    model: RetailReceiptModel,
    companyId: String?,
    momentId: String?,
    modifier: Modifier = Modifier,
    paper: RetailReceiptPaperSize,
    onPaperChange: (RetailReceiptPaperSize) -> Unit,
    format: RetailReceiptShareFormat,
    onFormatChange: (RetailReceiptShareFormat) -> Unit,
    logo: Bitmap?,
    onLogoChanged: () -> Unit,
) {
    val context = LocalContext.current
    var logoError by remember { mutableStateOf<String?>(null) }
    val pickLogo = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        when (val result = RetailReceiptLogoStore.saveFromUri(context, companyId, momentId, uri)) {
            is RetailLogoPickResult.Saved -> {
                logoError = null
                onLogoChanged()
            }
            is RetailLogoPickResult.Rejected -> logoError = result.message
        }
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Paper size",
            style = RetailRose.rowCaption(RetailRose.textFaint).copy(fontSize = 11.sp),
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            RetailReceiptPaperSize.entries.forEach { size ->
                RoseModeChip(
                    text = size.shortLabel,
                    selected = size == paper,
                    onClick = { onPaperChange(size) },
                )
            }
        }
        Text(
            "Share as",
            style = RetailRose.rowCaption(RetailRose.textFaint).copy(fontSize = 11.sp),
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            RetailReceiptShareFormat.entries.forEach { fmt ->
                RoseModeChip(
                    text = fmt.label,
                    selected = fmt == format,
                    onClick = { onFormatChange(fmt) },
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            RoseModeChip(
                text = if (logo != null) "Replace logo" else "Add store logo",
                selected = false,
                onClick = {
                    logoError = null
                    pickLogo.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.SingleMimeType("image/jpeg"),
                        ),
                    )
                },
            )
            if (logo != null) {
                RoseModeChip(
                    text = "Remove logo",
                    selected = false,
                    onClick = {
                        RetailReceiptLogoStore.clear(context, companyId, momentId)
                        logoError = null
                        onLogoChanged()
                    },
                )
            }
            RoseModeChip(
                text = "Share on WhatsApp",
                selected = false,
                onClick = {
                    shareRetailReceiptFile(
                        context = context,
                        model = model,
                        paper = paper,
                        format = format,
                        logo = logo,
                        toWhatsApp = true,
                    )
                },
            )
            RoseModeChip(
                text = "Share / print",
                selected = false,
                onClick = {
                    shareRetailReceiptFile(
                        context = context,
                        model = model,
                        paper = paper,
                        format = format,
                        logo = logo,
                        toWhatsApp = false,
                    )
                },
            )
        }
        Text(
            "JPEG logo · set in Locations · ${paper.label} · ${format.label}",
            style = RetailRose.rowCaption(RetailRose.textFaint).copy(fontSize = 10.sp),
        )
        logoError?.let { err ->
            Text(
                err,
                color = RetailRose.warning,
                fontFamily = PlusJakartaSans,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ReceiptKv(label: String, value: String, emphatic: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            Modifier.weight(1f),
            color = if (emphatic) RetailReceiptPaper.ink else RetailReceiptPaper.muted,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (emphatic) FontWeight.Bold else FontWeight.Normal,
            fontSize = if (emphatic) 12.sp else 10.sp,
        )
        Text(
            value,
            color = RetailReceiptPaper.ink,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (emphatic) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = if (emphatic) 13.sp else 10.sp,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ReceiptDash() {
    val color = RetailReceiptPaper.rule
    Spacer(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .height(1.dp)
            .drawBehind {
                drawLine(
                    color = color,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
                )
            },
    )
}
