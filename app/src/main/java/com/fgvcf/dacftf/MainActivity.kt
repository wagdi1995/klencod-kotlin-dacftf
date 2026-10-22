
package com.example.iphonecalculator

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.graphics.drawable.GradientDrawable

class MainActivity : Activity() {

    private lateinit var expressionView: TextView
    private lateinit var resultView: TextView

    private var expression = ""
    private var result = "0"

    private val orange = Color.rgb(255, 159, 10)
    private val darkGray = Color.rgb(51, 51, 51)
    private val lightGray = Color.rgb(165, 165, 165)
    private val black = Color.BLACK
    private val white = Color.WHITE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = black
        window.navigationBarColor = black
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(black)
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }

        expressionView = TextView(this).apply {
            text = ""
            textSize = 30f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            maxLines = 2
        }

        resultView = TextView(this).apply {
            text = "0"
            textSize = 68f
            setTextColor(white)
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            typeface = Typeface.create(
                "sans-serif-light",
                Typeface.NORMAL
            )
            maxLines = 1
        }

        root.addView(
            expressionView,
            LinearLayout.LayoutParams(
                -1, 0, 1f
            )
        )

        root.addView(
            resultView,
            LinearLayout.LayoutParams(
                -1, 0, 1.2f
            )
        )

        val rows = listOf(
            listOf("AC", "+/-", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "=")
        )

        rows.forEach { row ->
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }

            row.forEach { label ->
                val button = createButton(label)

                val params = LinearLayout.LayoutParams(
                    0, 0,
                    if (label == "0") 2f else 1f
                ).apply {
                    width = 0
                    height = 0
                    weight = if (label == "0") 2f else 1f
                    setMargins(dp(4), dp(4), dp(4), dp(4))
                }

                rowLayout.addView(button, params)
            }

            root.addView(
                rowLayout,
                LinearLayout.LayoutParams(
                    -1, 0, 1f
                )
            )
        }

        setContentView(root)
    }

    private fun createButton(label: String): TextView {
        val backgroundColor = when (label) {
            "÷", "×", "-", "+", "=" -> orange
            "AC", "+/-", "%" -> lightGray
            else -> darkGray
        }

        val foregroundColor = when (label) {
            "AC", "+/-", "%" -> black
            else -> white
        }

        return TextView(this).apply {
            text = label
            textSize = 27f
            gravity = Gravity.CENTER
            setTextColor(foregroundColor)

            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(backgroundColor)
            }

            setOnClickListener {
                handleInput(label)
            }
        }
    }

    private fun handleInput(value: String) {
        when (value) {
            "AC" -> {
                expression = ""
                result = "0"
            }

            "+/-" -> {
                val number = result.toDoubleOrNull()
                if (number != null) {
                    result = formatNumber(-number)
                    expression = result
                }
            }

            "%" -> {
                val number = result.toDoubleOrNull()
                if (number != null) {
                    result = formatNumber(number / 100.0)
                    expression = result
                }
            }

            "=" -> calculate()

            else -> {
                if (value in listOf("+", "-", "×", "÷")) {
                    if (expression.isEmpty() && value != "-") {
                        return
                    }

                    if (expression.isNotEmpty() &&
                        expression.last().toString() in
                        listOf("+", "-", "×", "÷")
                    ) {
                        expression = expression.dropLast(1)
                    }
                }

                if (value == ".") {
                    val currentNumber = expression
                        .split('+', '-', '×', '÷')
                        .last()

                    if (currentNumber.contains(".")) return
                }

                expression += value
                result = expression
            }
        }

        updateDisplay()
    }

    private fun calculate() {
        try {
            if (expression.isBlank()) return

            val answer = evaluateExpression(expression)
            result = formatNumber(answer)
            expression = result

        } catch (e: Exception) {
            result = "خطأ"
        }

        updateDisplay()
    }

    private fun evaluateExpression(input: String): Double {
        val tokens = Regex(
            """\d+(?:\.\d+)?|[+\-×÷]"""
        ).findAll(input).map { it.value }.toList()

        require(tokens.isNotEmpty())
        require(tokens.joinToString("") == input)

        val numbers = mutableListOf<Double>()
        val operators = mutableListOf<String>()

        var expectNumber = true

        for (token in tokens) {
            if (expectNumber) {
                if (token == "-" && numbers.size == operators.size) {
                    numbers.add(-1.0)
                    operators.add("×")
                    expectNumber = true
                    continue
                }

                val number = token.toDoubleOrNull()
                    ?: throw IllegalArgumentException()

                numbers.add(number)
                expectNumber = false
            } else {
                if (token !in listOf("+", "-", "×", "÷")) {
                    throw IllegalArgumentException()
                }
                operators.add(token)
                expectNumber = true
            }
        }

        require(!expectNumber && numbers.size == operators.size + 1)

        var i = 0
        while (i < operators.size) {
            if (operators[i] == "×" || operators[i] == "÷") {
                val a = numbers[i]
                val b = numbers[i + 1]

                require(operators[i] != "÷" || b != 0.0)

                val value = if (operators[i] == "×") a * b else a / b

                numbers[i] = value
                numbers.removeAt(i + 1)
                operators.removeAt(i)
            } else {
                i++
            }
        }

        var answer = numbers[0]

        for (j in operators.indices) {
            answer = when (operators[j]) {
                "+" -> answer + numbers[j + 1]
                "-" -> answer - numbers[j + 1]
                else -> throw IllegalArgumentException()
            }
        }

        require(answer.isFinite())
        return answer
    }

    private fun formatNumber(number: Double): String {
        if (!number.isFinite()) return "خطأ"

        return if (number == Math.floor(number) &&
            kotlin.math.abs(number) < 1e15
        ) {
            number.toLong().toString()
        } else {
            java.math.BigDecimal.valueOf(number)
                .stripTrailingZeros()
                .toPlainString()
        }
    }

    private fun updateDisplay() {
        expressionView.text = expression
        resultView.text = result
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}