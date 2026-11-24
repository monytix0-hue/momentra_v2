package com.example.momentra.ui.widget

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.momentra.MainActivity

class PersonalWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContainer(
                    title = "Personal Shell",
                    subtitle = "Pulse, habits & daily moments",
                    accentColor = ColorProvider(Color(0xFFC9BFFF)),
                    context = context,
                )
            }
        }
    }
}

class GroupWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContainer(
                    title = "Group Shell",
                    subtitle = "Collab, expenses & shared trips",
                    accentColor = ColorProvider(Color(0xFFFF7A3D)),
                    context = context,
                )
            }
        }
    }
}

class BusinessWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContainer(
                    title = "Business Shell",
                    subtitle = "Orders, inventory & revenue ops",
                    accentColor = ColorProvider(Color(0xFF818CF8)),
                    context = context,
                )
            }
        }
    }
}

@Composable
private fun WidgetContainer(
    title: String,
    subtitle: String,
    accentColor: ColorProvider,
    context: Context,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFF14121B)))
            .padding(16.dp)
            .clickable(actionStartActivity(ComponentName(context, MainActivity::class.java))),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.Start,
    ) {
        Text(
            text = title,
            style = TextStyle(
                color = accentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            ),
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = subtitle,
            style = TextStyle(
                color = ColorProvider(Color(0xFFC9C4D8)),
                fontSize = 12.sp,
            ),
        )
    }
}
