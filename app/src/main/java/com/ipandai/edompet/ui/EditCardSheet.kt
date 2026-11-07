package com.ipandai.edompet.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ipandai.edompet.data.MembershipCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCardSheet(card: MembershipCard, onDismiss: () -> Unit, onSave: (MembershipCard) -> Unit) {
    var draft by remember(card.id) { mutableStateOf(card) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Text("Edit Card", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                TextButton(
                    onClick = { onSave(draft.copy(merchantName = draft.merchantName.trim())) },
                    enabled = draft.merchantName.isNotBlank() && draft.codeValue.isNotEmpty(),
                ) { Text("Save", fontWeight = FontWeight.Bold) }
            }
            CardFields(
                draft.merchantName, { draft = draft.copy(merchantName = it) },
                draft.codeType, { draft = draft.copy(codeType = it) },
                draft.codeValue, { draft = draft.copy(codeValue = it) },
                draft.styleId, { draft = draft.copy(styleId = it) },
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
