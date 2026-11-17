package com.example.twelveman.utils

import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.twelveman.data.api.RetrofitClient
import com.example.twelveman.data.local.SessionManager

object ServerConfigDialog {

    fun show(context: Context, onUpdated: (() -> Unit)? = null) {
        val currentUrl = RetrofitClient.getBaseUrl(context)

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 10)
        }

        val hintText = TextView(context).apply {
            text = "Ensure your phone and computer are on the same Wi-Fi.\nEnter PC IP address:"
            textSize = 14f
            setPadding(0, 0, 0, 20)
        }
        container.addView(hintText)

        val input = EditText(context).apply {
            setText(currentUrl)
            setSelection(text.length)
            hint = "http://172.23.51.147/12THMAN_BACKEND/"
        }
        container.addView(input)

        AlertDialog.Builder(context)
            .setTitle("Server Configuration")
            .setView(container)
            .setPositiveButton("Save & Connect") { _, _ ->
                val newUrl = input.text.toString().trim()
                if (newUrl.isNotBlank()) {
                    RetrofitClient.setServerUrl(context, newUrl)
                    Toast.makeText(context, "Server updated to: ${RetrofitClient.getBaseUrl(context)}", Toast.LENGTH_LONG).show()
                    onUpdated?.invoke()
                }
            }
            .setNeutralButton("Reset Default") { _, _ ->
                SessionManager.getInstance(context).saveCustomServerUrl(null)
                Toast.makeText(context, "Reset to default: ${RetrofitClient.getBaseUrl(context)}", Toast.LENGTH_SHORT).show()
                onUpdated?.invoke()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

