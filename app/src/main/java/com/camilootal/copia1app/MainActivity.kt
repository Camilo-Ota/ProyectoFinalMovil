package com.camilootal.copia1app

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {

    val SUMA = "+"
    val RESTA = "-"
    val MULTIPLICACION = "*"
    val DIVISION = "/"
    val PORCENTAJE = "%"

    var operacionActual =""
    var primerNumero:Double = Double.NaN
    var segundoNumero:Double = Double.NaN

    lateinit var tvTemp: TextView
    lateinit var tvResultado: TextView
    lateinit var formatoDecimal: DecimalFormat



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

        formatoDecimal = DecimalFormat("#.##########")

        tvTemp = findViewById(R.id.tvTemp)
        tvResultado = findViewById(R.id.tvResultado)

    }

    fun cambiarOperador(b: View){
        if(tvTemp.text.isNotEmpty() || primerNumero.toString()!="NaN"){

            calcular() //funcion clave
            val boton: Button = b as Button
            if(boton.text.toString().trim()=="/"){
                operacionActual = "/"
            }else if(boton.text.toString().trim()=="X"){
                operacionActual = "*"
            }else{
                operacionActual = boton.text.toString().trim()
            }

            if(tvTemp.text.toString().isEmpty()){
                tvTemp.text=tvResultado.text
            }

            tvResultado.text = formatoDecimal.format(primerNumero) + operacionActual
            tvTemp.text =""
        }
    }

    fun calcular(){
        try {
            if(primerNumero.toString()!="NaN"){
                if(tvTemp.text.toString().isEmpty()){
                    tvTemp.text = tvResultado.text.toString()
                }
                segundoNumero = tvTemp.text.toString().toDouble()
                tvTemp.text=""

                when(operacionActual){
                    SUMA-> primerNumero = (primerNumero+segundoNumero)
                    "-"-> primerNumero = (primerNumero-segundoNumero)
                    "*"-> primerNumero = (primerNumero*segundoNumero)
                    "/"-> primerNumero = (primerNumero/segundoNumero)
                    "%"-> primerNumero = (primerNumero%segundoNumero)
                }
            }else{
                primerNumero = tvTemp.text.toString().toDouble()
            }
        }catch (e:Exception){

        }
    }

    fun seleccionarNumero(b: View){
        val boton: Button = b as Button
        tvTemp.text = tvTemp.text.toString() + boton.text.toString()
    }

    fun igual(b: View){
        calcular()
        tvResultado.text = formatoDecimal.format(primerNumero)
        //primerNumero = Double.NaN
        operacionActual = ""
    }

    fun borrar(b: View){
        val boton: Button = b as Button
        if(boton.text.toString().trim()=="C"){

            if(tvTemp.text.toString().isNotEmpty()){
                var datosActuales:CharSequence = tvTemp.text as CharSequence
                tvTemp.text = datosActuales.subSequence(0,datosActuales.length-1)
            }else{
                primerNumero = Double.NaN
                segundoNumero = Double.NaN
                tvTemp.text = ""
                tvResultado.text = ""
            }
        }else if(boton.text.toString().trim()=="CA"){
            primerNumero = Double.NaN
            segundoNumero = Double.NaN
            tvTemp.text = ""
            tvResultado.text = ""
        }

    }
}