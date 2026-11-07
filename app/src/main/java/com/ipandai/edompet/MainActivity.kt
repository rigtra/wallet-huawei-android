package com.ipandai.edompet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ipandai.edompet.data.CardRepository
import com.ipandai.edompet.data.MembershipCard
import com.ipandai.edompet.ui.AddCardScreen
import com.ipandai.edompet.ui.EditCardSheet
import com.ipandai.edompet.ui.WalletScreen
import com.ipandai.edompet.wear.WearEngineManager

class MainActivity : ComponentActivity() {

    private lateinit var repo: CardRepository
    private lateinit var wear: WearEngineManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repo = CardRepository(applicationContext)
        wear = WearEngineManager(this)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color.Black, surface = Color(0xFF1C1C1E))) {
                var cards by remember { mutableStateOf(repo.load()) }
                var selectedTab by rememberSaveable { mutableIntStateOf(0) }
                var editing by remember { mutableStateOf<MembershipCard?>(null) }

                fun update(newCards: List<MembershipCard>) { cards = newCards; repo.save(newCards) }

                LaunchedEffect(Unit) { wear.checkDeviceConnection() }

                Scaffold(
                    containerColor = Color.Black,
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == 0, onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Filled.CreditCard, null) }, label = { Text("Wallet") },
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1, onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Filled.AddCircle, null) }, label = { Text("Add Card") },
                            )
                        }
                    },
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) {
                        when (selectedTab) {
                            0 -> WalletScreen(cards, wear, onSync = ::sendToWatch, onEdit = { editing = it })
                            else -> AddCardScreen { newCard ->
                                update(listOf(newCard) + cards)
                                sendToWatch(newCard)
                                selectedTab = 0
                            }
                        }
                    }
                }

                editing?.let { card ->
                    EditCardSheet(card, onDismiss = { editing = null }) { updated ->
                        update(cards.map { if (it.id == updated.id) updated else it })
                        sendToWatch(updated)
                        editing = null
                    }
                }
            }
        }
    }

    private fun sendToWatch(card: MembershipCard) {
        wear.sendCardData(
            cardId = card.id,
            merchantName = card.merchantName,
            codeType = if (card.isBarcode) "barcode" else "qrcode",
            codeValue = card.codeValue,
            backgroundColor = card.style.solidHex,
            gradientColors = listOf(card.style.topHex, card.style.bottomHex),
            textColor = card.style.textHex,
        )
    }
}
