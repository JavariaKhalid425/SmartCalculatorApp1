package com.example.smartcalculator

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity2 : AppCompatActivity() {

    private lateinit var tvResult: TextView
    private lateinit var tvHistory: TextView
    private lateinit var historyScroll: ScrollView

    private lateinit var prefs: SharedPreferences

    private var expression: String = ""
    private var lastResult = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        tvResult = findViewById(R.id.tvResult)
        tvHistory = findViewById(R.id.tvHistory)
        historyScroll = findViewById(R.id.historyScroll)

        prefs = getSharedPreferences("calc_history", MODE_PRIVATE)

        val savedHistory = prefs.getString("history_text", "")
        tvHistory.text = savedHistory ?: "History\n"

        setupNumberButtons()
        setupOperatorButtons()
        setupScientificButtons()

        findViewById<Button>(R.id.btnEqual).setOnClickListener { calculate() }

        findViewById<Button>(R.id.btnAC).setOnClickListener {
            expression = ""
            tvResult.text = "0"
        }

        findViewById<Button>(R.id.btnBackspace).setOnClickListener {
            if (expression.isNotEmpty()) {
                expression = expression.dropLast(1)
                tvResult.text = expression
            }
        }

        findViewById<Button>(R.id.btnRotateBack).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun setupNumberButtons() {

        val numbers = listOf(
            R.id.btn0,R.id.btn1,R.id.btn2,R.id.btn3,R.id.btn4,
            R.id.btn5,R.id.btn6,R.id.btn7,R.id.btn8,R.id.btn9,R.id.btnDot
        )

        numbers.forEach {

            findViewById<Button>(it).setOnClickListener { btn ->

                if(lastResult){
                    expression=""
                    lastResult=false
                }

                expression += (btn as Button).text
                tvResult.text = expression
            }
        }
    }

    private fun setupOperatorButtons(){

        val operators = listOf(
            R.id.btnPlus,R.id.btnMinus,R.id.btnMultiply,R.id.btnDivide
        )

        operators.forEach{

            findViewById<Button>(it).setOnClickListener { btn ->

                val op = when((btn as Button).text.toString()){
                    "×" -> "*"
                    "÷" -> "/"
                    "−" -> "-"
                    else -> btn.text.toString()
                }

                expression += op
                tvResult.text = expression
            }
        }
    }

    private fun setupScientificButtons(){

        findViewByText("sin")?.setOnClickListener { addFunction("sin(") }

        findViewByText("cos")?.setOnClickListener { addFunction("cos(") }

        findViewByText("tan")?.setOnClickListener { addFunction("tan(") }

        findViewByText("ln")?.setOnClickListener { addFunction("ln(") }

        findViewByText("lg")?.setOnClickListener { addFunction("lg(") }

        findViewByText("√")?.setOnClickListener { addFunction("sqrt(") }

        findViewByText("π")?.setOnClickListener {

            expression += Math.PI
            tvResult.text = expression
        }

        findViewByText("e")?.setOnClickListener {

            expression += Math.E
            tvResult.text = expression
        }

        findViewByText("^")?.setOnClickListener {

            expression += "^"
            tvResult.text = expression
        }

        findViewByText("(")?.setOnClickListener {

            expression += "("
            tvResult.text = expression
        }

        findViewByText(")")?.setOnClickListener {

            expression += ")"
            tvResult.text = expression
        }

        findViewByText("x²")?.setOnClickListener {

            expression += "^2"
            tvResult.text = expression
        }

        findViewByText("x³")?.setOnClickListener {

            expression += "^3"
            tvResult.text = expression
        }

        findViewByText("1/x")?.setOnClickListener {

            expression = "1/($expression)"
            tvResult.text = expression
        }

        findViewByText("x!")?.setOnClickListener {

            val num = expression.toIntOrNull()

            if(num!=null){

                var fact=1

                for(i in 1..num)
                    fact*=i

                expression=fact.toString()

                tvResult.text=expression
            }
        }
    }

    private fun addFunction(func:String){

        expression+=func
        tvResult.text=expression
    }

    private fun calculate(){

        try{

            val result = eval(expression)

            tvResult.text = "$expression\n= $result"

            val date = SimpleDateFormat("dd MMM yyyy",Locale.getDefault()).format(Date())

            tvHistory.append("\n[$date]\n$expression = $result\n")

            prefs.edit().putString("history_text",tvHistory.text.toString()).apply()

            expression=result.toString()

            lastResult=true

        }catch (e:Exception){

            tvResult.text="Error"
        }
    }

    private fun findViewByText(text:String):Button?{

        val root = findViewById<LinearLayout>(R.id.buttonsLayout)

        for(i in 0 until root.childCount){

            val row=root.getChildAt(i)

            if(row is LinearLayout){

                for(j in 0 until row.childCount){

                    val btn=row.getChildAt(j)

                    if(btn is Button && btn.text==text)
                        return btn
                }
            }
        }

        return null
    }

    private fun eval(str:String):Double{

        return object{

            var pos=-1
            var ch=0

            fun nextChar(){

                ch=if(++pos<str.length) str[pos].code else -1
            }

            fun eat(charToEat:Int):Boolean{

                while(ch==' '.code) nextChar()

                if(ch==charToEat){

                    nextChar()
                    return true
                }

                return false
            }

            fun parse():Double{

                nextChar()

                val x=parseExpression()

                if(pos<str.length)
                    throw RuntimeException("Unexpected")

                return x
            }

            fun parseExpression():Double{

                var x=parseTerm()

                while(true){

                    when{

                        eat('+'.code)-> x+=parseTerm()

                        eat('-'.code)-> x-=parseTerm()

                        else-> return x
                    }
                }
            }

            fun parseTerm():Double{

                var x=parseFactor()

                while(true){

                    when{

                        eat('*'.code)-> x*=parseFactor()

                        eat('/'.code)-> x/=parseFactor()

                        eat('^'.code)-> x=x.pow(parseFactor())

                        else-> return x
                    }
                }
            }

            fun parseFactor():Double{

                if(eat('+'.code)) return parseFactor()

                if(eat('-'.code)) return -parseFactor()

                var x:Double

                val startPos=pos

                if(eat('('.code)){

                    x=parseExpression()

                    eat(')'.code)

                }

                else if(ch in '0'.code..'9'.code || ch=='.'.code){

                    while(ch in '0'.code..'9'.code || ch=='.'.code)
                        nextChar()

                    x=str.substring(startPos,pos).toDouble()
                }

                else if(ch in 'a'.code..'z'.code){

                    while(ch in 'a'.code..'z'.code)
                        nextChar()

                    val func=str.substring(startPos,pos)

                    x=parseFactor()

                    x=when(func){

                        "sqrt"->sqrt(x)

                        "sin"->sin(Math.toRadians(x))

                        "cos"->cos(Math.toRadians(x))

                        "tan"->tan(Math.toRadians(x))

                        "ln"->ln(x)

                        "lg"->log10(x)

                        else-> throw RuntimeException("Unknown function")
                    }
                }

                else{

                    throw RuntimeException("Unexpected")
                }

                return x
            }

        }.parse()
    }
}