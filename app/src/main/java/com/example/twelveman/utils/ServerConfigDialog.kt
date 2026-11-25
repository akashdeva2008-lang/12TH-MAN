package com.example.twelveman.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import android.widget.Toast
import com.example.twelveman.data.api.RetrofitClient
import com.example.twelveman.data.local.SessionManager
import com.example.twelveman.databinding.DialogServerConfigBinding

object ServerConfigDialog {

    fun show(context: Context, onUpdated: (() -> Unit)? = null) {
        val currentUrl = RetrofitClient.getBaseUrl(context)
        val binding = DialogServerConfigBinding.inflate(LayoutInflater.from(context))

        val dialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(binding.root)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setCancelable(true)
        }

        binding.etServerUrl.setText(currentUrl)
        binding.etServerUrl.setSelection(binding.etServerUrl.text?.length ?: 0)

        binding.btnSaveServerUrl.setOnClickListener {
            val newUrl = binding.etServerUrl.text.toString().trim()
            if (newUrl.isNotBlank()) {
                RetrofitClient.setServerUrl(context, newUrl)
                Toast.makeText(context, "Server updated to: ${RetrofitClient.getBaseUrl(context)}", Toast.LENGTH_LONG).show()
                dialog.dismiss()
                onUpdated?.invoke()
            }
        }

        binding.btnResetServerDefault.setOnClickListener {
            SessionManager.getInstance(context).saveCustomServerUrl(null)
            Toast.makeText(context, "Reset to default: ${RetrofitClient.getBaseUrl(context)}", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            onUpdated?.invoke()
        }

        binding.btnCancelServerDialog.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
