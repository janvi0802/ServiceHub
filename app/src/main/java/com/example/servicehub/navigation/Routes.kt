package com.example.servicehub.navigation

/*
 * Every screen has a "route": a short text name, like a web address inside the app.
 * NavController.navigate("customer_home") opens that screen.
 * Routes with {something} carry a value, e.g. "booking_detail/abc123".
 */
object Routes {
    // Common
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val PROFILE = "profile"
    const val NOTIFICATIONS = "notifications"

    // Customer
    const val CUSTOMER_HOME = "customer_home"
    const val SEARCH = "search"
    const val CATEGORY_SERVICES = "category_services/{categoryId}"
    const val PROVIDER_LIST = "provider_list/{categoryId}/{serviceId}"
    const val PROVIDER_DETAIL = "provider_detail/{providerId}/{serviceId}"
    const val BOOKING_FORM = "booking_form/{providerId}/{serviceId}"
    const val BOOKING_SUCCESS = "booking_success/{bookingId}"
    const val MY_BOOKINGS = "my_bookings"
    const val BOOKING_DETAIL = "booking_detail/{bookingId}"
    const val RATE_REVIEW = "rate_review/{bookingId}"
    const val RECEIPT = "receipt/{bookingId}"

    // Provider
    const val PENDING_APPROVAL = "pending_approval"
    const val PROVIDER_HOME = "provider_home"
    const val PROVIDER_REQUESTS = "provider_requests"
    const val JOB_DETAIL = "job_detail/{bookingId}"
    const val JOB_HISTORY = "job_history"

    // Admin
    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val ADMIN_PENDING_PROVIDERS = "admin_pending_providers"
    const val ADMIN_ALL_PROVIDERS = "admin_all_providers"
    const val ADMIN_USERS = "admin_users"
    const val ADMIN_SERVICES = "admin_services"
    const val ADMIN_BOOKINGS = "admin_bookings"

    // Helpers that fill in the {values}, so we never type a route by hand.
    fun categoryServices(categoryId: String) = "category_services/$categoryId"
    fun providerList(categoryId: String, serviceId: String) = "provider_list/$categoryId/$serviceId"
    fun providerDetail(providerId: String, serviceId: String) = "provider_detail/$providerId/$serviceId"
    fun bookingForm(providerId: String, serviceId: String) = "booking_form/$providerId/$serviceId"
    fun bookingSuccess(bookingId: String) = "booking_success/$bookingId"
    fun bookingDetail(bookingId: String) = "booking_detail/$bookingId"
    fun rateReview(bookingId: String) = "rate_review/$bookingId"
    fun receipt(bookingId: String) = "receipt/$bookingId"
    fun jobDetail(bookingId: String) = "job_detail/$bookingId"
}
