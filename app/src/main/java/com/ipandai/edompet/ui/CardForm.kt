package com.ipandai.edompet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ipandai.edompet.data.CODE_TYPE_BARCODE
import com.ipandai.edompet.data.CODE_TYPE_QR

/** Fields shared by the Add tab and the Edit sheet. */
@Composable
fun CardFields(
    merchantName: String, onMerchantName: (String) -> Unit,
    codeType: String, onCodeType: (String) -> Unit,
    codeValue: String, onCodeValue: (String) -> Unit,
    styleId: String, onStyle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            merchantName, onMerchantName,
            label = { Text("Merchant name") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Code type", modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically))
            listOf(CODE_TYPE_QR, CODE_TYPE_BARCODE).forEach { type ->
                FilterChip(selected = codeType == type, onClick = { onCodeType(type) }, label = { Text(type) })
            }
        }
        OutlinedTextField(
            codeValue, onCodeValue,
            label = { Text("QR / Barcode number") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        CardStylePicker(styleId, onStyle)
    }
}
