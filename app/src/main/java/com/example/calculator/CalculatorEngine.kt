package com.example.calculator

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.ArrayDeque

object CalculatorEngine {

  private val MATH_CONTEXT = MathContext(12, RoundingMode.HALF_UP)

  /**
   * Evaluates a mathematical expression string containing numbers, decimal points,
   * and operators: +, − (or -), × (or *), ÷ (or /), and % (percent).
   * Returns a clean formatted string, or null if evaluation fails / division by zero.
   */
  fun evaluate(expression: String): String? {
    if (expression.isBlank()) return null

    val cleanExpr = expression
      .replace("×", "*")
      .replace("÷", "/")
      .replace("−", "-")
      .trim()

    // Don't evaluate if ends with a dangling binary operator
    if (cleanExpr.endsWith("+") || cleanExpr.endsWith("-") ||
        cleanExpr.endsWith("*") || cleanExpr.endsWith("/")) {
      return null
    }

    return try {
      val tokens = tokenize(cleanExpr)
      if (tokens.isEmpty()) return null
      val postfix = infixToPostfix(tokens)
      val result = evaluatePostfix(postfix)
      formatResult(result)
    } catch (e: Exception) {
      null
    }
  }

  private sealed class Token {
    data class NumberToken(val value: BigDecimal) : Token()
    data class OpToken(val op: Char, val precedence: Int) : Token()
    object PercentToken : Token()
  }

  private fun tokenize(expr: String): List<Token> {
    val tokens = mutableListOf<Token>()
    var i = 0
    val len = expr.length

    while (i < len) {
      val c = expr[i]

      when {
        c.isWhitespace() -> {
          i++
        }
        c.isDigit() || c == '.' -> {
          val start = i
          var dotCount = if (c == '.') 1 else 0
          i++
          while (i < len && (expr[i].isDigit() || expr[i] == '.')) {
            if (expr[i] == '.') {
              dotCount++
              if (dotCount > 1) break
            }
            i++
          }
          val numStr = expr.substring(start, i)
          val bd = BigDecimal(if (numStr == ".") "0" else numStr)
          tokens.add(Token.NumberToken(bd))
        }
        c == '%' -> {
          tokens.add(Token.PercentToken)
          i++
        }
        c in listOf('+', '-', '*', '/') -> {
          // Check if this '-' is a unary minus (at beginning or preceded by another operator)
          if (c == '-' && (tokens.isEmpty() || tokens.last() is Token.OpToken)) {
            // Unary minus: read the subsequent number
            i++
            val start = i
            var dotCount = 0
            while (i < len && (expr[i].isDigit() || expr[i] == '.')) {
              if (expr[i] == '.') {
                dotCount++
                if (dotCount > 1) break
              }
              i++
            }
            if (i > start) {
              val numStr = expr.substring(start, i)
              val bd = BigDecimal(if (numStr == ".") "0" else numStr).negate()
              tokens.add(Token.NumberToken(bd))
            } else {
              // Standalone minus without number
              tokens.add(Token.OpToken('-', 1))
            }
          } else {
            val prec = if (c == '*' || c == '/') 2 else 1
            tokens.add(Token.OpToken(c, prec))
            i++
          }
        }
        else -> {
          i++
        }
      }
    }
    return tokens
  }

  private fun infixToPostfix(tokens: List<Token>): List<Token> {
    val output = mutableListOf<Token>()
    val stack = ArrayDeque<Token.OpToken>()

    for (token in tokens) {
      when (token) {
        is Token.NumberToken -> output.add(token)
        is Token.PercentToken -> output.add(token)
        is Token.OpToken -> {
          while (stack.isNotEmpty() && stack.peek().precedence >= token.precedence) {
            output.add(stack.pop())
          }
          stack.push(token)
        }
      }
    }

    while (stack.isNotEmpty()) {
      output.add(stack.pop())
    }

    return output
  }

  private fun evaluatePostfix(tokens: List<Token>): BigDecimal {
    val stack = ArrayDeque<BigDecimal>()

    for (token in tokens) {
      when (token) {
        is Token.NumberToken -> stack.push(token.value)
        is Token.PercentToken -> {
          if (stack.isEmpty()) throw IllegalArgumentException("Invalid percent operation")
          val a = stack.pop()
          stack.push(a.divide(BigDecimal(100), MATH_CONTEXT))
        }
        is Token.OpToken -> {
          if (stack.size < 2) throw IllegalArgumentException("Invalid expression syntax")
          val b = stack.pop()
          val a = stack.pop()
          val res = when (token.op) {
            '+' -> a.add(b, MATH_CONTEXT)
            '-' -> a.subtract(b, MATH_CONTEXT)
            '*' -> a.multiply(b, MATH_CONTEXT)
            '/' -> {
              if (b.compareTo(BigDecimal.ZERO) == 0) {
                throw ArithmeticException("Division by zero")
              }
              a.divide(b, MATH_CONTEXT)
            }
            else -> throw IllegalArgumentException("Unknown operator ${token.op}")
          }
          stack.push(res)
        }
      }
    }

    if (stack.size != 1) {
      throw IllegalArgumentException("Malformed expression")
    }

    return stack.pop()
  }

  fun formatResult(result: BigDecimal): String {
    // Strip trailing zeroes after decimal point
    val stripped = result.stripTrailingZeros()
    val plain = stripped.toPlainString()

    // If result is integer or small decimal, return plain string
    if (plain.length > 14 && (stripped.scale() > 6 || stripped.scale() < -4)) {
      return String.format("%.6e", stripped.toDouble())
    }

    // Replace -0 with 0
    if (plain == "-0" || plain == "-0.0") return "0"
    return plain
  }
}
