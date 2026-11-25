package com.example.servicehub.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.servicehub.data.FixedData
import com.example.servicehub.data.model.GUARANTEE_DAYS
import com.example.servicehub.ui.components.AppCard
import com.example.servicehub.ui.components.InfoBanner
import com.example.servicehub.ui.components.ScreenHeader

/** One question and its answer. */
data class Faq(val question: String, val answer: String)

/** The FAQs, written for customers and providers. Fixed in the app (no database needed). */
val customerFaqs = listOf(
    Faq("How do I book a service?",
        "On Home, tap a category or search for a service. Open it to see what's included, tap Book, choose a professional, " +
            "a date and a time slot, then Review booking and Confirm."),
    Faq("What is the 4-digit PIN?",
        "Once the professional accepts, your booking shows a PIN. Share it only when they arrive at your address. " +
            "They need it to start the job, so nobody else can start it."),
    Faq("How do I pay?",
        "Pay by Cash or UPI after the work is done. The price you see before booking is fixed, plus a ₹${FixedData.VISIT_FEE} visit fee. " +
            "Spare parts, if needed, are added only after you agree and appear on your receipt."),
    Faq("Can I cancel or reschedule?",
        "Yes, while the booking is Requested or Accepted. Open the booking and tap Cancel booking or Reschedule. " +
            "If you reschedule an accepted booking, the professional must accept the new time again."),
    Faq("I'm not happy with the work. What can I do?",
        "Every completed job has a $GUARANTEE_DAYS-day guarantee. Open the booking and tap \"Not happy with the work? Get it redone free\". " +
            "Describe what's wrong and the professional will come back and redo the job at no extra cost."),
    Faq("What if the professional doesn't arrive?",
        "You'll see \"On the way\" when they leave. If they don't come, call them from the booking, reschedule, or cancel. " +
            "You can also report it to us."),
    Faq("Can I book for someone else?",
        "Yes. In the booking form choose \"Booking for: Someone else\" and enter their name and phone. " +
            "The professional will contact them at the address."),
    Faq("How do I report a problem?",
        "Open the booking and tap \"Report a problem with this booking\". The ServiceHub team will call you and resolve it."),
)

val providerFaqs = listOf(
    Faq("When can customers see me?",
        "After the admin approves your account, while your Available switch is on, and not on your weekly days off."),
    Faq("How do I start a job?",
        "Tap \"I'm on my way\" when you leave, then ask the customer for their 4-digit PIN at the address and tap Start job."),
    Faq("What if I need spare parts?",
        "Tell the customer first. When you complete the job, add each part with its price. It appears on the customer's receipt."),
    Faq("How much do I earn?",
        "You keep the service price and any extra parts. The ₹${FixedData.VISIT_FEE} visit fee is ServiceHub's platform fee."),
    Faq("I can't come to an accepted job.",
        "Open the job and tap \"Can't come? Cancel this job\" with a reason. The customer is told at once."),
    Faq("A customer complained. What do I do?",
        "Within $GUARANTEE_DAYS days of a job, a customer who is not happy can ask you to redo it for free. " +
            "It appears under Jobs → Job redo requests. Call them, redo the work, then tap \"Mark as redone\"."),
)

/** Help & support: tap a question to show its answer. */
@Composable
fun HelpScreen(isProvider: Boolean, onBack: () -> Unit) {
    val faqs = if (isProvider) providerFaqs else customerFaqs
    var open by rememberSaveable { mutableIntStateOf(-1) }   // which question is expanded (-1 = none)

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Help & support", onBack, subtitle = "Answers to common questions")
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(faqs) { index, faq ->
                AppCard(onClick = { open = if (open == index) -1 else index }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(faq.question, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Icon(if (open == index) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
                    }
                    AnimatedVisibility(visible = open == index) {
                        Text(
                            faq.answer,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
            item {
                InfoBanner(
                    Icons.Filled.SupportAgent,
                    if (isProvider) "Still need help? Contact the ServiceHub admin."
                    else "Still need help? Open your booking and tap \"Report a problem\". We'll call you.",
                    MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
