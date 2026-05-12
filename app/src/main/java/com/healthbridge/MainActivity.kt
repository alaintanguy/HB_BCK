package com.healthbridge

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    private var selectedDeviceName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val spinner =
            findViewById<Spinner>(R.id.deviceSpinner)

        val selectedText =
            findViewById<TextView>(R.id.selectedDeviceText)

        val connectButton =
            findViewById<Button>(R.id.connectButton)

        val heartRateText =
            findViewById<TextView>(R.id.heartRateText)

        val statusText =
            findViewById<TextView>(R.id.statusText)

        val textHR =
            findViewById<TextView>(R.id.textHR)

        val textTime =
            findViewById<TextView>(R.id.textTime)

        val database =
            FirebaseDatabase.getInstance()

        val memberRef =
            database.getReference(
                "groups/family_001/members/alain"
            )

        val devices = listOf(
            "Polar H10",
            "Garmin HRM",
            "Wahoo Tickr"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            devices
        )

        spinner.adapter = adapter

        spinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    selectedDeviceName =
                        devices[position]

                    selectedText.text =
                        "Selected: $selectedDeviceName"
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {

                    selectedText.text =
                        "No device selected"
                }
            }

        connectButton.setOnClickListener {

            if (selectedDeviceName.isNotEmpty()) {

                statusText.text =
                    "Status: Connected"

                Toast.makeText(
                    this,
                    "Connected to $selectedDeviceName",
                    Toast.LENGTH_SHORT
                ).show()

                val handler =
                    Handler(Looper.getMainLooper())

                val runnable =
                    object : Runnable {

                        override fun run() {

                            val randomHR =
                                (65..90).random()

                            heartRateText.text =
                                "Heart Rate: $randomHR bpm"

                            textHR.text =
                                "$randomHR bpm"

                            textTime.text =
                                "Live update"

                            memberRef.child("heartRate")
                                .setValue(randomHR)

                            memberRef.child("timestamp")
                                .setValue(
                                    System.currentTimeMillis()
                                )

                            handler.postDelayed(
                                this,
                                2000
                            )
                        }
                    }

                handler.post(runnable)

            } else {

                statusText.text =
                    "Status: Device not connected"

                Toast.makeText(
                    this,
                    "Select a device first",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}