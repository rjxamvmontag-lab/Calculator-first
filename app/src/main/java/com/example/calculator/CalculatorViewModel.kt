package com.example.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
import java.math.MathContext

data class HistoryItem(
  val id: Long = System.currentTimeMillis(),
  val expression: String,
  val result: String,
  val timestamp: Long = System.currentTimeMillis()
)

data class CalculatorUiState(
  val displayValue: String = "0",
  val secondaryExpression: String = "",
  val isError: Boolean = false,
  val isCalculated: Boolean = false,
  val history: List<HistoryItem> = emptyList(),
  val showHistorySheet: Boolean = false,
  val copyMessage: String? = null
)

class CalculatorViewModel : ViewModel() {

  private val _uiState = MutableStateFlow(CalculatorUiState())
  val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

  private var operandNeedsReset = false

  fun onDigit(digit: String) {
    _uiState.update { state ->
      if (state.isError) {
        operandNeedsReset = false
        state.copy(
          displayValue = digit,
          secondaryExpression = "",
          isError = false,
          isCalculated = false
        )
      } else if (state.isCalculated || operandNeedsReset) {
        operandNeedsReset = false
        val newSecondary = if (state.isCalculated) "" else state.secondaryExpression
        state.copy(
          displayValue = digit,
          secondaryExpression = newSecondary,
          isCalculated = false
        )
      } else {
        val current = state.displayValue
        val updated = when {
          current == "0" && digit == "00" -> "0"
          current == "0" -> digit
          current.replace("-", "").length >= 15 -> current // Max limit
          else -> current + digit
        }
        state.copy(displayValue = updated)
      }
    }
  }

  fun onDecimal() {
    _uiState.update { state ->
      if (state.isError || state.isCalculated || operandNeedsReset) {
        operandNeedsReset = false
        state.copy(
          displayValue = "0.",
          secondaryExpression = if (state.isCalculated) "" else state.secondaryExpression,
          isError = false,
          isCalculated = false
        )
      } else {
        val current = state.displayValue
        if (!current.contains('.')) {
          state.copy(displayValue = "$current.")
        } else {
          state
        }
      }
    }
  }

  fun onOperator(op: String) {
    _uiState.update { state ->
      if (state.isError) return@update state

      val currentDisplay = state.displayValue
      val sec = state.secondaryExpression

      if (operandNeedsReset && sec.isNotEmpty()) {
        // Just replace the trailing operator
        val tokens = sec.trim().split(" ")
        if (tokens.isNotEmpty()) {
          val base = tokens.dropLast(1).joinToString(" ")
          val updated = if (base.isEmpty()) "$currentDisplay $op" else "$base $op"
          return@update state.copy(secondaryExpression = updated)
        }
      }

      if (sec.isEmpty() || state.isCalculated) {
        operandNeedsReset = true
        state.copy(
          secondaryExpression = "$currentDisplay $op",
          isCalculated = false
        )
      } else {
        // Calculate intermediate evaluation
        val fullExpr = "$sec $currentDisplay"
        val intermediate = CalculatorEngine.evaluate(fullExpr)
        operandNeedsReset = true
        if (intermediate != null) {
          state.copy(
            displayValue = intermediate,
            secondaryExpression = "$intermediate $op",
            isCalculated = false
          )
        } else {
          state.copy(
            secondaryExpression = "$fullExpr $op",
            isCalculated = false
          )
        }
      }
    }
  }

  fun onEquals() {
    _uiState.update { state ->
      if (state.isError) return@update state
      if (state.secondaryExpression.isEmpty()) return@update state

      val fullExpr = "${state.secondaryExpression} ${state.displayValue}".trim()
      val result = CalculatorEngine.evaluate(fullExpr)

      if (result != null) {
        val newHistory = listOf(
          HistoryItem(expression = fullExpr, result = result)
        ) + state.history.take(29) // keep up to 30 items

        operandNeedsReset = true
        state.copy(
          displayValue = result,
          secondaryExpression = "$fullExpr =",
          isCalculated = true,
          isError = false,
          history = newHistory
        )
      } else {
        state.copy(
          displayValue = "Error",
          secondaryExpression = fullExpr,
          isError = true,
          isCalculated = false
        )
      }
    }
  }

  fun onPercentage() {
    _uiState.update { state ->
      if (state.isError) return@update state
      try {
        val bd = BigDecimal(state.displayValue)
        val res = bd.divide(BigDecimal(100), MathContext.DECIMAL64)
        val formatted = CalculatorEngine.formatResult(res)
        state.copy(displayValue = formatted)
      } catch (e: Exception) {
        state
      }
    }
  }

  fun onToggleSign() {
    _uiState.update { state ->
      if (state.isError || state.displayValue == "0") return@update state
      val current = state.displayValue
      val toggled = if (current.startsWith("-")) {
        current.removePrefix("-")
      } else {
        "-$current"
      }
      state.copy(displayValue = toggled)
    }
  }

  fun onBackspace() {
    _uiState.update { state ->
      if (state.isError || state.isCalculated || operandNeedsReset) {
        state.copy(displayValue = "0", isError = false, isCalculated = false)
      } else {
        val current = state.displayValue
        val updated = if (current.length > 1) {
          val next = current.dropLast(1)
          if (next == "-" || next.isEmpty()) "0" else next
        } else {
          "0"
        }
        state.copy(displayValue = updated)
      }
    }
  }

  fun onAllClear() {
    operandNeedsReset = false
    _uiState.update {
      it.copy(
        displayValue = "0",
        secondaryExpression = "",
        isError = false,
        isCalculated = false
      )
    }
  }

  fun toggleHistorySheet(show: Boolean) {
    _uiState.update { it.copy(showHistorySheet = show) }
  }

  fun onSelectHistory(item: HistoryItem) {
    operandNeedsReset = false
    _uiState.update {
      it.copy(
        displayValue = item.result,
        secondaryExpression = "",
        isCalculated = true,
        isError = false,
        showHistorySheet = false
      )
    }
  }

  fun onClearHistory() {
    _uiState.update { it.copy(history = emptyList()) }
  }

  fun setCopyMessage(message: String?) {
    _uiState.update { it.copy(copyMessage = message) }
  }
}
