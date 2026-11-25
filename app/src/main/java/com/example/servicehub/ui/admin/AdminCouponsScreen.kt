package com.example.servicehub.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.servicehub.data.model.Coupon
import com.example.servicehub.data.model.CouponType
import com.example.servicehub.data.repository.BookingRepository
import com.example.servicehub.data.repository.CouponRepository
import com.example.servicehub.ui.components.AppCard
import com.example.servicehub.ui.components.Caption
import com.example.servicehub.ui.components.EmptyBox
import com.example.servicehub.ui.components.ScreenHeader
import com.example.servicehub.ui.components.SoftIcon
import com.example.servicehub.ui.components.TextInput
import com.example.servicehub.ui.theme.BrandOrange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminCouponsViewModel : ViewModel() {
    /** null = still loading */
    val coupons: StateFlow<List<Coupon>?> = CouponRepository.all()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val message = MutableStateFlow<String?>(null)

    fun save(code: String, type: String, value: Int, minOrder: Int, maxDiscount: Int, onDone: () -> Unit) = viewModelScope.launch {
        try {
            CouponRepository.save(code, type, value, minOrder, maxDiscount)
            message.value = "Coupon ${CouponRepository.clean(code)} saved."
            onDone()
        } catch (e: Exception) {
            message.value = BookingRepository.friendlyError(e)
        }
    }

    fun setActive(coupon: Coupon, active: Boolean) = viewModelScope.launch {
        try {
            CouponRepository.setActive(coupon.code, active)
        } catch (e: Exception) {
            message.value = "Could not change it. Check internet."
        }
    }
}

/** "₹50 off · min ₹299" or "10% off up to ₹100" */
fun couponSummary(c: Coupon): String {
    val off = if (c.type == CouponType.PERCENT) "${c.value}% off" + (if (c.maxDiscount > 0) " up to ₹${c.maxDiscount}" else "")
    else "₹${c.value} off"
    return off + if (c.minOrder > 0) " · min bill ₹${c.minOrder}" else ""
}

/** Admin: list of coupons with an on/off switch, and "+ Add" to create one. */
@Composable
fun AdminCouponsScreen(onBack: () -> Unit, viewModel: AdminCouponsViewModel = viewModel()) {
    val coupons by viewModel.coupons.collectAsState()
    val message by viewModel.message.collectAsState()
    var adding by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Coupons", onBack, subtitle = "Discounts customers type on the review page") {
            TextButton(onClick = { adding = true }) { Text("+ Add") }
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp)) }
        val list = coupons
        when {
            list == null -> com.example.servicehub.ui.components.LoadingBox()
            list.isEmpty() -> EmptyBox(
                title = "No coupons yet",
                message = "Tap + Add to create one, e.g. FIRST50 for ₹50 off.",
                icon = Icons.Filled.LocalOffer,
            )
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.code }) { coupon ->
                    AppCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SoftIcon(Icons.Filled.LocalOffer, BrandOrange)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(coupon.code, style = MaterialTheme.typography.titleSmall)
                                Caption(couponSummary(coupon))
                            }
                            Switch(checked = coupon.isActive, onCheckedChange = { viewModel.setActive(coupon, it) })
                        }
                    }
                }
            }
        }
    }

    if (adding) {
        AddCouponDialog(
            onSave = { code, type, value, min, max -> viewModel.save(code, type, value, min, max) { adding = false } },
            onDismiss = { adding = false },
        )
    }
}

@Composable
private fun AddCouponDialog(onSave: (String, String, Int, Int, Int) -> Unit, onDismiss: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(CouponType.FLAT) }
    var value by remember { mutableStateOf("") }
    var minOrder by remember { mutableStateOf("") }
    var maxDiscount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New coupon") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextInput(code, { code = it.take(15) }, "Code, e.g. FIRST50")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == CouponType.FLAT, onClick = { type = CouponType.FLAT }, label = { Text("₹ off") })
                    FilterChip(selected = type == CouponType.PERCENT, onClick = { type = CouponType.PERCENT }, label = { Text("% off") })
                }
                TextInput(value, { value = it.filter(Char::isDigit).take(4) },
                    if (type == CouponType.FLAT) "Rupees off" else "Percent off", keyboardType = KeyboardType.Number)
                TextInput(minOrder, { minOrder = it.filter(Char::isDigit).take(5) }, "Minimum bill ₹ (optional)", keyboardType = KeyboardType.Number)
                if (type == CouponType.PERCENT) {
                    TextInput(maxDiscount, { maxDiscount = it.filter(Char::isDigit).take(5) }, "Maximum discount ₹ (optional)", keyboardType = KeyboardType.Number)
                }
                Caption("The discount is ServiceHub's offer: the provider still gets their full share.")
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(code, type, value.toIntOrNull() ?: 0, minOrder.toIntOrNull() ?: 0, maxDiscount.toIntOrNull() ?: 0) },
                enabled = code.isNotBlank() && (value.toIntOrNull() ?: 0) > 0,
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Back") } },
    )
}
