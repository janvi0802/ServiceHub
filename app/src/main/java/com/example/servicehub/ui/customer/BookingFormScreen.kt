package com.example.servicehub.ui.customer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.servicehub.data.DateUtils
import com.example.servicehub.data.FixedData
import com.example.servicehub.data.model.Provider
import com.example.servicehub.data.model.Service
import com.example.servicehub.data.model.User
import com.example.servicehub.data.repository.AuthRepository
import com.example.servicehub.data.repository.BookingRepository
import com.example.servicehub.data.repository.ServiceRepository
import com.example.servicehub.data.repository.UserRepository
import com.example.servicehub.ui.components.InfoRow
import com.example.servicehub.ui.components.LoadingBox
import com.example.servicehub.ui.components.ScreenHeader
import com.example.servicehub.ui.components.TextInput
import com.example.servicehub.ui.components.rupees
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class BookingFormUiState(
    val customer: User? = null,
    val provider: Provider? = null,
    val service: Service? = null,
    val loadError: String? = null,
    val date: String = DateUtils.today(),
    val timeSlot: String = "",
    val address: String = "",
    val note: String = "",
    val paymentMode: String = FixedData.paymentModes.first(),
    val isSaving: Boolean = false,
    val error: String? = null,
    val createdBookingId: String? = null,   // set after saving -> go to success screen
)

class BookingFormViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val providerId: String = savedStateHandle["providerId"] ?: ""
    private val serviceId: String = savedStateHandle["serviceId"] ?: ""

    private val _uiState = MutableStateFlow(BookingFormUiState())
    val uiState: StateFlow<BookingFormUiState> = _uiState

    init { load() }

    /** Loads the customer (for the saved address), the provider and the service. */
    private fun load() = viewModelScope.launch {
        try {
            val uid = AuthRepository.currentUserId ?: throw Exception("Please log in again.")
            val customer = UserRepository.getUser(uid)
            val provider = UserRepository.getProvider(providerId)
            val service = ServiceRepository.getService(serviceId)
            if (customer == null || provider == null || service == null) throw Exception("Could not load booking details.")
            _uiState.update {
                it.copy(customer = customer, provider = provider, service = service, address = customer.address)
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(loadError = e.message ?: "Could not load. Check internet.") }
        }
    }

    fun onDateChange(date: String) = _uiState.update {
        // If the chosen slot is already over on the new date (today), clear it.
        val keepSlot = !isSlotDisabled(date, it.timeSlot)
        it.copy(date = date, timeSlot = if (keepSlot) it.timeSlot else "", error = null)
    }
    fun onSlotChange(slot: String) = _uiState.update { it.copy(timeSlot = slot, error = null) }
    fun onAddressChange(v: String) = _uiState.update { it.copy(address = v, error = null) }
    fun onNoteChange(v: String) = _uiState.update { it.copy(note = v, error = null) }
    fun onPaymentModeChange(v: String) = _uiState.update { it.copy(paymentMode = v, error = null) }

    /** Today's slots that have already ended cannot be booked. */
    fun isSlotDisabled(date: String, slot: String): Boolean =
        date == DateUtils.today() &&
            FixedData.isSlotOverToday(slot, Calendar.getInstance().get(Calendar.HOUR_OF_DAY))

    fun confirm() {
        val s = _uiState.value
        val customer = s.customer ?: return
        val provider = s.provider ?: return
        val service = s.service ?: return

        val problem = when {
            s.timeSlot.isBlank() -> "Please choose a time slot."
            isSlotDisabled(s.date, s.timeSlot) -> "This slot is already over today. Please choose another."
            s.address.isBlank() -> "Please enter the address."
            else -> null
        }
        if (problem != null) {
            _uiState.update { it.copy(error = problem) }
            return
        }

        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                val id = BookingRepository.createBooking(
                    customer, provider, service,
                    date = s.date, timeSlot = s.timeSlot,
                    address = s.address.trim(), note = s.note.trim(), paymentMode = s.paymentMode,
                )
                _uiState.update { it.copy(isSaving = false, createdBookingId = id) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = BookingRepository.friendlyError(e)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)   // FlowRow (chips that wrap onto the next line)
@Composable
fun BookingFormScreen(
    onBack: () -> Unit,
    onBooked: (bookingId: String) -> Unit,
    viewModel: BookingFormViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.createdBookingId) {
        state.createdBookingId?.let(onBooked)
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Book service", onBack)
        val service = state.service
        val provider = state.provider
        if (service == null || provider == null) {
            if (state.loadError != null) Text(state.loadError!!, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
            else LoadingBox()
            return@Column
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("${service.name} with ${provider.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            SectionTitle("Date")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateUtils.nextDays(7).forEach { date ->
                    FilterChip(
                        selected = date == state.date,
                        onClick = { viewModel.onDateChange(date) },
                        label = { Text(DateUtils.chipLabel(date)) },
                    )
                }
            }

            SectionTitle("Time slot")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FixedData.timeSlots.forEach { slot ->
                    FilterChip(
                        selected = slot == state.timeSlot,
                        onClick = { viewModel.onSlotChange(slot) },
                        enabled = !viewModel.isSlotDisabled(state.date, slot),
                        label = { Text(slot) },
                    )
                }
            }

            SectionTitle("Address")
            TextInput(state.address, viewModel::onAddressChange, "Service address", singleLine = false)

            SectionTitle("Problem (optional)")
            TextInput(state.note, viewModel::onNoteChange, "e.g. kitchen tap leaking", singleLine = false)

            SectionTitle("Pay after service by")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FixedData.paymentModes.forEach { mode ->
                    FilterChip(
                        selected = mode == state.paymentMode,
                        onClick = { viewModel.onPaymentModeChange(mode) },
                        label = { Text(mode) },
                    )
                }
            }

            // Price summary
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Price summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    InfoRow(service.name, rupees(service.price))
                    InfoRow("Visit fee", rupees(FixedData.VISIT_FEE))
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    InfoRow("Total", rupees(service.price + FixedData.VISIT_FEE), bold = true)
                }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }

        Button(
            onClick = viewModel::confirm,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
        ) {
            Text(if (state.isSaving) "Booking..." else "Confirm booking • ${rupees(service.price + FixedData.VISIT_FEE)}")
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
}
