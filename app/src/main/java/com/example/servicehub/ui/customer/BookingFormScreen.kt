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
import com.example.servicehub.data.model.isOffOn
import com.example.servicehub.data.model.nextWorkingDay
import com.example.servicehub.ui.components.InfoBanner
import com.example.servicehub.ui.components.BottomActionBar
import com.example.servicehub.ui.components.AppCard
import com.example.servicehub.ui.components.Caption
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import com.example.servicehub.ui.theme.StatusRequested
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventBusy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    /** "Book for someone else": the person at the address (e.g. parents). */
    val bookingForOther: Boolean = false,
    val contactName: String = "",
    val contactPhone: String = "",
    /** false = filling the form, true = the "Review booking" summary is shown. */
    val reviewing: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val createdBookingId: String? = null,   // set after saving -> go to success screen
)

class BookingFormViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val providerId: String = savedStateHandle["providerId"] ?: ""
    private val serviceId: String = savedStateHandle["serviceId"] ?: ""

    private val _uiState = MutableStateFlow(BookingFormUiState())
    val uiState: StateFlow<BookingFormUiState> = _uiState

    /**
     * Slots this provider already has booked on the chosen date (live).
     * flatMapLatest: whenever the date changes, stop listening to the old date and listen to the new one.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val busySlots: StateFlow<Set<String>> = _uiState
        .map { it.date }
        .distinctUntilChanged()
        .flatMapLatest { date -> BookingRepository.busySlots(providerId, date) }
        .catch { emit(emptySet()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init { load() }

    /** Loads the customer (for the saved address), the provider and the service. */
    private fun load() = viewModelScope.launch {
        try {
            val uid = AuthRepository.currentUserId ?: throw Exception("Please log in again.")
            val customer = UserRepository.getUser(uid)
            val provider = UserRepository.getProvider(providerId)
            val service = ServiceRepository.getService(serviceId)
            if (customer == null || provider == null || service == null) throw Exception("Could not load booking details.")
            // Start on the first day the provider works (skip days off) that still has a slot left
            // (e.g. after 8 PM, today has no slots, so start tomorrow).
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val firstWorkingDay = DateUtils.nextDays(7).firstOrNull { date ->
                !isOffOn(provider, date) &&
                    FixedData.timeSlots.any { FixedData.isBookableTime(date, it, DateUtils.today(), hour) }
            } ?: DateUtils.today()
            _uiState.update {
                it.copy(
                    customer = customer, provider = provider, service = service,
                    address = customer.address, date = firstWorkingDay,
                )
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
    fun onBookingForChange(other: Boolean) = _uiState.update { it.copy(bookingForOther = other, error = null) }
    fun onContactNameChange(v: String) = _uiState.update { it.copy(contactName = v.take(40), error = null) }
    fun onContactPhoneChange(v: String) = _uiState.update { it.copy(contactPhone = v.filter(Char::isDigit).take(10), error = null) }

    /** Back from the summary to the form (nothing is saved). */
    fun backToEdit() = _uiState.update { it.copy(reviewing = false, error = null) }

    /** Checks the form; returns a message if something is wrong, or null if it can be booked. */
    private fun problemWith(s: BookingFormUiState, provider: Provider): String? = when {
        isOffOn(provider, s.date) -> "${provider.name} is off on this day. Please choose another date."
        s.timeSlot.isBlank() -> "Please choose a time slot."
        isSlotDisabled(s.date, s.timeSlot) -> "This slot is already over today. Please choose another."
        s.timeSlot in busySlots.value -> "This slot was just booked by someone else. Please choose another."
        s.address.isBlank() -> "Please enter the address."
        s.bookingForOther && s.contactName.isBlank() -> "Please enter the name of the person at the address."
        s.bookingForOther && s.contactPhone.length != 10 -> "Their phone number must be 10 digits."
        else -> null
    }

    /** "Review booking": check the form, then show the summary (still not saved). */
    fun review() {
        val s = _uiState.value
        val provider = s.provider ?: return
        val problem = problemWith(s, provider)
        _uiState.update { if (problem != null) it.copy(error = problem) else it.copy(reviewing = true, error = null) }
    }

    /** Today's slots that have already ended cannot be booked. */
    fun isSlotDisabled(date: String, slot: String): Boolean =
        date == DateUtils.today() &&
            FixedData.isSlotOverToday(slot, Calendar.getInstance().get(Calendar.HOUR_OF_DAY))

    /** "Confirm booking" on the summary: only now is the booking saved (all checks run again). */
    fun confirm() {
        val s = _uiState.value
        val customer = s.customer ?: return
        val provider = s.provider ?: return
        val service = s.service ?: return

        val problem = problemWith(s, provider)
        if (problem != null) {
            _uiState.update { it.copy(error = problem, reviewing = false) }   // back to the form to fix it
            return
        }

        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                val id = BookingRepository.createBooking(
                    customer, provider, service,
                    date = s.date, timeSlot = s.timeSlot,
                    address = s.address.trim(), note = s.note.trim(), paymentMode = s.paymentMode,
                    contactName = if (s.bookingForOther) s.contactName.trim() else "",
                    contactPhone = if (s.bookingForOther) s.contactPhone else "",
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
    val busy by viewModel.busySlots.collectAsState()

    LaunchedEffect(state.createdBookingId) {
        state.createdBookingId?.let(onBooked)
    }

    // On the summary, the phone's Back button returns to the form instead of leaving
    BackHandler(enabled = state.reviewing) { viewModel.backToEdit() }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            if (state.reviewing) "Review booking" else "Book service",
            onBack = { if (state.reviewing) viewModel.backToEdit() else onBack() },
            subtitle = if (state.reviewing) "Check everything before confirming" else null,
        )
        val service = state.service
        val provider = state.provider
        if (service == null || provider == null) {
            if (state.loadError != null) Text(state.loadError!!, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
            else LoadingBox()
            return@Column
        }

        if (state.reviewing) {
            ReviewSummary(state, service, provider, onEdit = viewModel::backToEdit, modifier = Modifier.weight(1f))
        } else Column(
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
                    val off = isOffOn(provider, date)
                    FilterChip(
                        selected = date == state.date,
                        onClick = { viewModel.onDateChange(date) },
                        enabled = !off,   // provider's weekly day off
                        label = { Text(if (off) "${DateUtils.chipLabel(date)} · off" else DateUtils.chipLabel(date)) },
                    )
                }
            }
            if (provider.offDays.isNotEmpty()) {
                val today = DateUtils.today()
                val next = nextWorkingDay(provider, DateUtils.nextDays(7))
                InfoBanner(
                    icon = Icons.Filled.EventBusy,
                    color = StatusRequested,
                    text = if (isOffOn(provider, today)) {
                        "${provider.name} is off today (${DateUtils.weekdayShortNames[DateUtils.weekdayOf(today) - 1]})." +
                            (next?.let { " Next available: ${DateUtils.pretty(it)}." } ?: "") +
                            " Need it today? Choose another provider."
                    } else {
                        "${provider.name} is off every ${DateUtils.weekdayNames(provider.offDays)}."
                    },
                )
            }

            SectionTitle("Time slot")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FixedData.timeSlots.forEach { slot ->
                    val taken = slot in busy
                    FilterChip(
                        selected = slot == state.timeSlot && !taken,
                        onClick = { viewModel.onSlotChange(slot) },
                        enabled = !taken && !viewModel.isSlotDisabled(state.date, slot),
                        label = { Text(if (taken) "$slot · Booked" else slot) },
                    )
                }
            }

            SectionTitle("Address")
            // One tap to use the profile address or a saved one (Home / Work / ...)
            val choices = buildList {
                state.customer?.address?.takeIf { it.isNotBlank() }?.let { add("Profile" to it) }
                state.customer?.savedAddresses.orEmpty().forEach { add(it.label to it.address) }
            }
            if (choices.size > 1) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    choices.forEach { (label, address) ->
                        FilterChip(
                            selected = state.address == address,
                            onClick = { viewModel.onAddressChange(address) },
                            label = { Text(label) },
                        )
                    }
                }
            }
            TextInput(state.address, viewModel::onAddressChange, "Service address", singleLine = false)

            SectionTitle("Booking for")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !state.bookingForOther, onClick = { viewModel.onBookingForChange(false) }, label = { Text("Myself") })
                FilterChip(selected = state.bookingForOther, onClick = { viewModel.onBookingForChange(true) }, label = { Text("Someone else") })
            }
            if (state.bookingForOther) {
                Caption("The professional will contact this person at the address.")
                TextInput(state.contactName, viewModel::onContactNameChange, "Their name, e.g. Mum")
                TextInput(state.contactPhone, viewModel::onContactPhoneChange, "Their phone (10 digits)", keyboardType = KeyboardType.Phone)
            }

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
                    Text(FixedData.PRICE_NOTE, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }

        // Fixed bottom bar: total on the left, the main action on the right
        BottomActionBar {
            Column(Modifier.weight(1f)) {
                Text(rupees(service.price + FixedData.VISIT_FEE), style = MaterialTheme.typography.titleLarge)
                Text("incl. ₹${FixedData.VISIT_FEE} visit fee · pay after service", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                // Step 1: review the details. Step 2 (on the summary): really save the booking.
                onClick = if (state.reviewing) viewModel::confirm else viewModel::review,
                enabled = !state.isSaving,
                modifier = Modifier.height(50.dp),
            ) {
                Text(
                    when {
                        state.isSaving -> "Booking..."
                        state.reviewing -> "Confirm booking"
                        else -> "Review booking"
                    }
                )
            }
        }
    }
}

/** The summary shown before confirming, with "Edit" to go back and change anything. */
@Composable
private fun ReviewSummary(
    state: BookingFormUiState,
    service: Service,
    provider: Provider,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ReviewSection("Service", onEdit) {
            InfoRow("Service", service.name)
            InfoRow("Professional", provider.name)
        }
        ReviewSection("When", onEdit) {
            InfoRow("Date", DateUtils.pretty(state.date))
            InfoRow("Slot", state.timeSlot)
        }
        ReviewSection("Where", onEdit) {
            InfoRow("Address", state.address.trim())
            if (state.bookingForOther) {
                InfoRow("Contact person", state.contactName.trim())
                InfoRow("Their phone", state.contactPhone)
            } else {
                InfoRow("Contact person", "You")
            }
            if (state.note.isNotBlank()) InfoRow("Problem", state.note.trim())
        }
        ReviewSection("Payment", onEdit) {
            InfoRow(service.name, rupees(service.price))
            InfoRow("Visit fee", rupees(FixedData.VISIT_FEE))
            HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
            InfoRow("Total", rupees(service.price + FixedData.VISIT_FEE), bold = true)
            InfoRow("Pay by", "${state.paymentMode}, after the service")
            Caption(FixedData.PRICE_NOTE, modifier = Modifier.padding(top = 6.dp))
        }
        Caption("Nothing is booked yet. Tap Confirm booking to send the request to ${provider.name}.")
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun ReviewSection(title: String, onEdit: () -> Unit, content: @Composable () -> Unit) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onEdit) { Text("Edit") }
        }
        content()
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
}
