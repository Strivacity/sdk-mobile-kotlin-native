package com.strivacity.android.app

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.strivacity.android.native_sdk.render.LoginController
import com.strivacity.android.native_sdk.render.models.HorizontalLayout
import com.strivacity.android.native_sdk.render.models.Layout
import com.strivacity.android.native_sdk.render.models.Screen
import com.strivacity.android.native_sdk.render.models.VerticalLayout
import com.strivacity.android.native_sdk.render.models.WidgetLayout

@Composable
fun Layout(loginController: LoginController, screen: Screen, layout: Layout) {
  when (layout) {
    is HorizontalLayout -> HorizontalLayout(loginController, screen, layout)
    is VerticalLayout -> VerticalLayout(loginController, screen, layout)
    is WidgetLayout -> WidgetLayout(loginController, screen, layout)
  }
}

@Composable
fun HorizontalLayout(
    loginController: LoginController,
    screen: Screen,
    horizontalLayout: HorizontalLayout
) {
  LazyRow(verticalAlignment = Alignment.CenterVertically) {
    items(horizontalLayout.items) { item -> Layout(loginController, screen, item) }
  }
}

@Composable
fun VerticalLayout(
    loginController: LoginController,
    screen: Screen,
    verticalLayout: VerticalLayout
) {
  LazyColumn(horizontalAlignment = Alignment.CenterHorizontally) {
    items(verticalLayout.items) { item -> Layout(loginController, screen, item) }
  }
}

@Composable
fun WidgetLayout(loginController: LoginController, screen: Screen, widgetLayout: WidgetLayout) {
  val widget =
      screen.forms
          ?.find { it.id == widgetLayout.formId }
          ?.widgets
          ?.find { it.id == widgetLayout.widgetId }

  if (widget == null) {
    loginController.triggerFallback()
    return
  }

  Widget(loginController, screen, widget, widgetLayout.formId, widgetLayout.widgetId)
}
