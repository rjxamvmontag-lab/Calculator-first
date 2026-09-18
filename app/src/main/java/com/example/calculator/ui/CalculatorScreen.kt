package com.example.calculator.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.calculator.CalculatorUiState
import com.example.calculator.CalculatorViewModel
import com.example.calculator.HistoryItem
import com.example.ui.theme.ClearRed
import com.example.ui.theme.OperatorOrange
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
  viewModel: CalculatorViewModel,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val clipboardManager = LocalClipboardManager.current
  val haptic = LocalHapticFeedback.current
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  LaunchedEffect(uiState.copyMessage) {
    uiState.copyMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.setCopyMessage(null)
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      CalculatorTopBar(
        onHistoryClick = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          viewModel.toggleHistorySheet(true)
        },
        onCopyClick = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          clipboardManager.setText(AnnotatedString(uiState.displayValue))
          viewModel.setCopyMessage("Copied to clipboard")
        },
        historyCount = uiState.history.size
      )
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .navigationBarsPadding(),
      contentAlignment = Alignment.BottomCenter
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 500.dp)
          .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Bottom
      ) {
        // Display Area
        CalculatorDisplay(
          secondaryExpression = uiState.secondaryExpression,
          displayValue = uiState.displayValue,
          isError = uiState.isError,
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Keypad Grid
        CalculatorKeypad(
          onDigit = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.onDigit(it)
          },
          onOperator = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.onOperator(it)
          },
          onEquals = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.onEquals()
          },
          onClear = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.onAllClear()
          },
          onBackspace = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.onBackspace()
          },
          onPercentage = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.onPercentage()
          },
          onToggleSign = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.onToggleSign()
          },
          onDecimal = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.onDecimal()
          }
        )

        Spacer(modifier = Modifier.height(8.dp))
      }
    }
  }

  // History Bottom Sheet
  if (uiState.showHistorySheet) {
    ModalBottomSheet(
      onDismissRequest = { viewModel.toggleHistorySheet(false) },
      sheetState = sheetState,
      containerColor = MaterialTheme.colorScheme.surface
    ) {
      HistorySheetContent(
        history = uiState.history,
        onSelect = { item ->
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          viewModel.onSelectHistory(item)
        },
        onClearAll = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          viewModel.onClearHistory()
        },
        onClose = {
          scope.launch { sheetState.hide() }.invokeOnCompletion {
            viewModel.toggleHistorySheet(false)
          }
        }
      )
    }
  }
}

@Composable
private fun CalculatorTopBar(
  onHistoryClick: () -> Unit,
  onCopyClick: () -> Unit,
  historyCount: Int,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = stringResource(R.string.app_name),
      style = MaterialTheme.typography.titleLarge.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
      ),
      color = MaterialTheme.colorScheme.onBackground
    )

    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = onCopyClick,
        modifier = Modifier.testTag("button_copy")
      ) {
        Icon(
          imageVector = Icons.Default.ContentCopy,
          contentDescription = "Copy result",
          tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      IconButton(
        onClick = onHistoryClick,
        modifier = Modifier.testTag("button_history")
      ) {
        Box(contentAlignment = Alignment.TopEnd) {
          Icon(
            imageVector = Icons.Default.History,
            contentDescription = "Calculation history",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
          if (historyCount > 0) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(OperatorOrange)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CalculatorDisplay(
  secondaryExpression: String,
  displayValue: String,
  isError: Boolean,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp),
    verticalArrangement = Arrangement.Bottom,
    horizontalAlignment = Alignment.End
  ) {
    // Secondary expression
    Text(
      text = secondaryExpression,
      style = MaterialTheme.typography.bodyLarge.copy(
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium
      ),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.End,
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 6.dp)
        .testTag("display_secondary")
    )

    // Primary calculated display value with dynamic font scaling
    val fontSize = when {
      displayValue.length > 13 -> 30.sp
      displayValue.length > 9 -> 38.sp
      displayValue.length > 6 -> 46.sp
      else -> 54.sp
    }

    Text(
      text = displayValue,
      style = MaterialTheme.typography.headlineLarge.copy(
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        lineHeight = fontSize * 1.15f
      ),
      color = if (isError) ClearRed else MaterialTheme.colorScheme.onBackground,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.End,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("display_result")
    )
  }
}

@Composable
private fun CalculatorKeypad(
  onDigit: (String) -> Unit,
  onOperator: (String) -> Unit,
  onEquals: () -> Unit,
  onClear: () -> Unit,
  onBackspace: () -> Unit,
  onPercentage: () -> Unit,
  onToggleSign: () -> Unit,
  onDecimal: () -> Unit,
  modifier: Modifier = Modifier
) {
  val spacing = 12.dp

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(spacing)
  ) {
    // Row 1: AC, ⌫, %, ÷
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
      CalculatorKey(
        text = "AC",
        textColor = ClearRed,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        tag = "button_ac",
        modifier = Modifier.weight(1f),
        onClick = onClear
      )
      CalculatorKey(
        text = "⌫",
        textColor = MaterialTheme.colorScheme.onSurface,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        tag = "button_backspace",
        modifier = Modifier.weight(1f),
        onClick = onBackspace
      )
      CalculatorKey(
        text = "%",
        textColor = MaterialTheme.colorScheme.onSurface,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        tag = "button_percent",
        modifier = Modifier.weight(1f),
        onClick = onPercentage
      )
      CalculatorKey(
        text = "÷",
        textColor = Color.White,
        containerColor = OperatorOrange,
        tag = "button_divide",
        modifier = Modifier.weight(1f),
        onClick = { onOperator("÷") }
      )
    }

    // Row 2: 7, 8, 9, ×
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
      CalculatorKey(
        text = "7",
        tag = "button_7",
        modifier = Modifier.weight(1f),
        onClick = { onDigit("7") }
      )
      CalculatorKey(
        text = "8",
        tag = "button_8",
        modifier = Modifier.weight(1f),
        onClick = { onDigit("8") }
      )
      CalculatorKey(
        text = "9",
        tag = "button_9",
        modifier = Modifier.weight(1f),
        onClick = { onDigit("9") }
      )
      CalculatorKey(
        text = "×",
        textColor = Color.White,
        containerColor = OperatorOrange,
        tag = "button_multiply",
        modifier = Modifier.weight(1f),
        onClick = { onOperator("×") }
      )
    }

    // Row 3: 4, 5, 6, −
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
      CalculatorKey(
        text = "4",
        tag = "button_4",
        modifier = Modifier.weight(1f),
        onClick = { onDigit("4") }
      )
      CalculatorKey(
        text = "5",
        tag = "button_5",
        modifier = Modifier.weight(1f),
        onClick = { onDigit("5") }
      )
      CalculatorKey(
        text = "6",
        tag = "button_6",
        modifier = Modifier.weight(1f),
        onClick = { onDigit("6") }
      )
      CalculatorKey(
        text = "−",
        textColor = Color.White,
        containerColor = OperatorOrange,
        tag = "button_subtract",
        modifier = Modifier.weight(1f),
        onClick = { onOperator("−") }
      )
    }

    // Row 4: 1, 2, 3, +
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
      CalculatorKey(
        text = "1",
        tag = "button_1",
        modifier = Modifier.weight(1f),
        onClick = { onDigit("1") }
      )
      CalculatorKey(
        text = "2",
        tag = "button_2",
        modifier = Modifier.weight(1f),
        onClick = { onDigit("2") }
      )
      CalculatorKey(
        text = "3",
        tag = "button_3",
        modifier = Modifier.weight(1f),
        onClick = { onDigit("3") }
      )
      CalculatorKey(
        text = "+",
        textColor = Color.White,
        containerColor = OperatorOrange,
        tag = "button_add",
        modifier = Modifier.weight(1f),
        onClick = { onOperator("+") }
      )
    }

    // Row 5: ±, 0, ., =
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
      CalculatorKey(
        text = "±",
        textColor = MaterialTheme.colorScheme.onSurface,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        tag = "button_sign",
        modifier = Modifier.weight(1f),
        onClick = onToggleSign
      )
      CalculatorKey(
        text = "0",
        tag = "button_0",
        modifier = Modifier.weight(1f),
        onClick = { onDigit("0") }
      )
      CalculatorKey(
        text = ".",
        tag = "button_decimal",
        modifier = Modifier.weight(1f),
        onClick = onDecimal
      )
      CalculatorKey(
        text = "=",
        textColor = MaterialTheme.colorScheme.onPrimary,
        containerColor = MaterialTheme.colorScheme.primary,
        tag = "button_equals",
        modifier = Modifier.weight(1f),
        onClick = onEquals
      )
    }
  }
}

@Composable
private fun CalculatorKey(
  text: String,
  tag: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  textColor: Color = MaterialTheme.colorScheme.onSurface,
  containerColor: Color = MaterialTheme.colorScheme.surface,
  fontSize: Int = 24
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(24.dp),
    color = containerColor,
    shadowElevation = 2.dp,
    modifier = modifier
      .aspectRatio(1f)
      .testTag(tag)
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.fillMaxSize()
    ) {
      Text(
        text = text,
        style = MaterialTheme.typography.titleLarge.copy(
          fontSize = fontSize.sp,
          fontWeight = FontWeight.SemiBold
        ),
        color = textColor,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
private fun HistorySheetContent(
  history: List<HistoryItem>,
  onSelect: (HistoryItem) -> Unit,
  onClearAll: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 8.dp)
      .navigationBarsPadding()
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = stringResource(R.string.history_title),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )

      if (history.isNotEmpty()) {
        TextButton(
          onClick = onClearAll,
          modifier = Modifier.testTag("button_clear_history")
        ) {
          Icon(
            imageVector = Icons.Default.DeleteSweep,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = ClearRed
          )
          Spacer(modifier = Modifier.size(4.dp))
          Text(
            text = stringResource(R.string.clear_history),
            color = ClearRed,
            style = MaterialTheme.typography.labelMedium
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    if (history.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(180.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = stringResource(R.string.history_empty),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .height(280.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(history, key = { it.id }) { item ->
          Card(
            onClick = { onSelect(item) },
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("history_item_${item.id}")
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
              horizontalAlignment = Alignment.End
            ) {
              Text(
                text = "${item.expression} =",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = item.result,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}
