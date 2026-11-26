package com.example.servicehub.data.repository

import com.example.servicehub.data.await
import com.example.servicehub.data.liveList
import com.example.servicehub.data.model.Coupon
import com.example.servicehub.data.model.CouponType
import com.example.servicehub.data.model.couponDiscount
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Discount codes: the admin creates them, customers apply them on the review page. */
object CouponRepository {

    private val coupons = FirebaseFirestore.getInstance().collection("coupons")

    /** "first 50" -> "FIRST50": codes are stored in capitals without spaces. */
    fun clean(code: String) = code.filterNot(Char::isWhitespace).uppercase()

    /** Admin: every coupon, live, newest first. */
    fun all(): Flow<List<Coupon>> =
        coupons.liveList(Coupon::class.java).map { list -> list.sortedByDescending { it.createdAt } }

    /** Customer: coupons the admin has switched on, live (for "Offers for you"). */
    fun active(): Flow<List<Coupon>> =
        coupons.whereEqualTo("isActive", true).liveList(Coupon::class.java)

    /**
     * Checks [code] against a bill of [amount] and returns the coupon and its discount.
     * Throws a BookingException with a friendly message if it cannot be used.
     */
    suspend fun check(code: String, amount: Int): Pair<Coupon, Int> {
        val id = clean(code)
        if (id.isEmpty()) throw BookingException("Type a coupon code.")
        val coupon = coupons.document(id).get().await().toObject(Coupon::class.java)
            ?: throw BookingException("No coupon called $id.")
        val discount = couponDiscount(coupon, amount).getOrElse { throw BookingException(it.message ?: "Coupon not valid.") }
        return coupon to discount
    }

    /** Admin creates (or replaces) a coupon. */
    suspend fun save(code: String, type: String, value: Int, minOrder: Int, maxDiscount: Int) {
        val id = clean(code)
        if (id.length !in 3..15) throw BookingException("The code must be 3 to 15 letters or numbers.")
        if (type == CouponType.PERCENT && value !in 1..90) throw BookingException("A % coupon must be 1 to 90 % off.")
        if (type == CouponType.FLAT && value !in 1..5000) throw BookingException("A ₹ coupon must be ₹1 to ₹5,000 off.")
        coupons.document(id).set(
            Coupon(
                code = id, type = type, value = value, minOrder = minOrder.coerceAtLeast(0),
                maxDiscount = maxDiscount.coerceAtLeast(0), isActive = true, createdAt = System.currentTimeMillis(),
            )
        ).await()
    }

    /** Admin turns a coupon on or off (old bookings keep their discount). */
    suspend fun setActive(code: String, active: Boolean) {
        coupons.document(code).update("isActive", active).await()
    }
}
