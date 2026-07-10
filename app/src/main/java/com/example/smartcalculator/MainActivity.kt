package com.example.smartcalculator

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var tvResult: TextView
    private lateinit var tvHistory: TextView
    private var expression: String = ""
    private var lastResult: Boolean = false  // Track result shown state

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvResult = findViewById(R.id.tvResult)
        tvHistory = findViewById(R.id.tvHistory)

        prefs = getSharedPreferences("calc_history", MODE_PRIVATE)

        // --- Load saved history ---
        val savedHistory = prefs.getString("history_text", "")
        if (!savedHistory.isNullOrEmpty()) {
            tvHistory.text = savedHistory
            tvHistory.alpha = 0.4f // dim old history
        } else {
            tvHistory.text = "History\n"
            tvHistory.alpha = 0.4f
        }

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val todayDate = dateFormat.format(Date())

        // Numbers
        val numbers = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btnDot
        )
        for (id in numbers) {
            findViewById<Button>(id).setOnClickListener {
                val value = (it as Button).text.toString()
                if (lastResult) {
                    expression = ""
                    tvResult.text = ""
                    lastResult = false
                }
                expression += value
                tvResult.text = expression
            }
        }

        // Operations
        val operations = listOf(
            R.id.btnPlus, R.id.btnMinus, R.id.btnMultiply,
            R.id.btnDivide
        )
        for (id in operations) {
            findViewById<Button>(id).setOnClickListener {
                var value = (it as Button).text.toString()
                value = when (value) {
                    "×" -> "*"
                    "÷" -> "/"
                    "−" -> "-"
                    "–" -> "-"
                    "-" -> "-"
                    else -> value
                }

                if (lastResult) lastResult = false
                expression += value
                tvResult.text = expression
            }
        }

        // Percent
        findViewById<Button>(R.id.btnPercent).setOnClickListener {
            if (expression.isNotEmpty() && expression.trim().toDoubleOrNull() != null) {
                val num = expression.trim().toDouble()
                val percentValue = num / 100
                val displayResult = if (percentValue % 1 == 0.0) {
                    percentValue.toInt().toString()
                } else {
                    percentValue.toString()
                }
                addToHistory("$num% = $displayResult", todayDate)
                expression = displayResult
                tvResult.text = displayResult
                lastResult = true
            } else {
                expression += "%"
                tvResult.text = expression
                lastResult = false
            }
        }

        // Clear
        findViewById<Button>(R.id.btnAC).setOnClickListener {
            expression = ""
            tvResult.text = "0"
            lastResult = false
        }

        // Backspace
        findViewById<Button>(R.id.btnBackspace).setOnClickListener {
            if (expression.isNotEmpty()) {
                expression = expression.dropLast(1)
                tvResult.text = if (expression.isEmpty()) "0" else expression
            }
            lastResult = false
        }

        // Equal
        findViewById<Button>(R.id.btnEqual).setOnClickListener {
            try {
                val normalizedExpr = normalizeExpression(expression)
                val result = eval(normalizedExpr)

                val displayResult = if (result % 1 == 0.0) {
                    result.toInt().toString()
                } else {
                    result.toString()
                }

                // ✅ Add to date-wise history
                addToHistory("$expression = $displayResult", todayDate)

                tvResult.text = displayResult
                expression = displayResult
                lastResult = true
            } catch (e: Exception) {
                tvResult.text = "Error"
                expression = ""
                lastResult = false
            }
        }

        // Rotate
        findViewById<Button>(R.id.btnRotate).setOnClickListener {
            val intent = Intent(this, MainActivity2::class.java)
            startActivity(intent)
        }
    }

    // Add expression to history with date
    private fun addToHistory(entry: String, date: String) {
        var currentHistory = tvHistory.text.toString()
        if (!currentHistory.contains("[$date]")) {
            currentHistory += "\n[$date]\n"
        }
        currentHistory += "$entry\n"
        tvHistory.text = currentHistory
        tvHistory.alpha = 0.4f

        // Save to SharedPreferences
        prefs.edit().putString("history_text", currentHistory).apply()
    }

    private fun normalizeExpression(expr: String): String {
        return expr.replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace("–", "-")
            .replace("—", "-")
    }

    private fun eval(expr: String): Double {
        return object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() { ch = if (++pos < expr.length) expr[pos].code else -1 }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) { nextChar(); return true }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < expr.length) throw RuntimeException("Unexpected: " + expr[pos])
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    when {
                        eat('+'.code) -> x += parseTerm()
                        eat('-'.code) -> x -= parseTerm()
                        else -> return x
                    }
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    when {
                        eat('*'.code) -> x *= parseFactor()
                        eat('/'.code) -> x /= parseFactor()
                        eat('%'.code) -> x %= parseFactor()
                        else -> return x
                    }
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()

                var x: Double
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if (ch in '0'.code..'9'.code || ch == '.'.code) {
                    while (ch in '0'.code..'9'.code || ch == '.'.code) nextChar()
                    x = expr.substring(startPos, pos).toDouble()
                } else {
                    throw RuntimeException("Unexpected: " + ch.toChar())
                }
                return x
            }
        }.parse()
    }
}
