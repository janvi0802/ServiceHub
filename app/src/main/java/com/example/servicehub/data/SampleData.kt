package com.example.servicehub.data

import com.example.servicehub.data.model.ApprovalStatus
import com.example.servicehub.data.model.Provider
import com.example.servicehub.data.model.Service

/*
 * Starting data the admin loads once with the "Load sample data" button.
 * Ids are fixed (e.g. "plumber_tap_repair"), so pressing the button twice
 * simply overwrites the same documents instead of creating duplicates.
 */
object SampleData {

    val services: List<Service> = listOf(
        // Plumber
        Service("plumber_tap_repair", "plumber", "Tap repair", "Fix leaking or broken taps", 149, 30),
        Service("plumber_pipe_leak", "plumber", "Pipe leakage fix", "Find and seal leaking pipes", 299, 60),
        Service("plumber_toilet_repair", "plumber", "Toilet repair", "Flush, seat or leakage repair", 349, 60),
        Service("plumber_tank_cleaning", "plumber", "Water tank cleaning", "Clean overhead water tank (up to 1000 L)", 599, 90),
        // Electrician
        Service("electrician_switch", "electrician", "Switch/socket repair", "Replace or fix a switch or socket", 99, 30),
        Service("electrician_fan", "electrician", "Fan installation", "Install a ceiling fan", 199, 45),
        Service("electrician_light", "electrician", "Light fitting", "Install tube light or LED panel", 149, 30),
        Service("electrician_wiring", "electrician", "Wiring check", "Find and fix wiring faults", 399, 90),
        // Cleaning
        Service("cleaning_bathroom", "cleaning", "Bathroom cleaning", "Deep cleaning of one bathroom", 399, 60),
        Service("cleaning_kitchen", "cleaning", "Kitchen cleaning", "Deep cleaning of kitchen and slab", 699, 120),
        Service("cleaning_sofa", "cleaning", "Sofa cleaning", "Shampoo cleaning, up to 5 seats", 549, 90),
        Service("cleaning_full_home", "cleaning", "Full home cleaning", "Deep cleaning of a 2BHK", 2999, 300),
        // AC Repair
        Service("ac_service", "ac_repair", "AC service", "Filter and coil cleaning", 499, 60),
        Service("ac_gas_refill", "ac_repair", "Gas refill", "Refill AC gas and check leaks", 1999, 90),
        Service("ac_installation", "ac_repair", "AC installation", "Install a split AC", 1499, 120),
        Service("ac_not_cooling", "ac_repair", "Not cooling repair", "Diagnose and fix cooling problem", 349, 60),
        // Carpenter
        Service("carpenter_door", "carpenter", "Door repair", "Fix hinges, lock or alignment", 249, 60),
        Service("carpenter_furniture", "carpenter", "Furniture assembly", "Assemble bed, table or wardrobe", 499, 120),
        Service("carpenter_shelf", "carpenter", "Shelf installation", "Install wall shelf", 199, 45),
        // Painter
        Service("painter_room", "painter", "Room painting", "Paint one room (walls only)", 2499, 480),
        Service("painter_touch_up", "painter", "Wall touch-up", "Patch and paint small areas", 499, 90),
        Service("painter_door_polish", "painter", "Door polish", "Polish one wooden door", 699, 120),
    )

    /** 2–3 approved providers per category, spread over different areas. */
    val providers: List<Provider> = listOf(
        sampleProvider("sample_plumber_1", "Ramesh Patel", "9800000001", "plumber", "Navrangpura", 8, "Expert in leak fixing and bathroom fittings."),
        sampleProvider("sample_plumber_2", "Suresh Shah", "9800000002", "plumber", "Satellite", 5, "Quick and clean plumbing work."),
        sampleProvider("sample_plumber_3", "Imran Khan", "9800000003", "plumber", "Bopal", 3, "Tap, pipe and tank specialist."),
        sampleProvider("sample_electrician_1", "Mahesh Joshi", "9800000004", "electrician", "Navrangpura", 10, "Licensed electrician, home wiring expert."),
        sampleProvider("sample_electrician_2", "Vikram Desai", "9800000005", "electrician", "Maninagar", 4, "Fans, lights and switch repairs."),
        sampleProvider("sample_electrician_3", "Arjun Mehta", "9800000006", "electrician", "Vastrapur", 6, "Safe and careful electrical work."),
        sampleProvider("sample_cleaning_1", "Priya Sharma", "9800000007", "cleaning", "Satellite", 5, "Deep cleaning with safe products."),
        sampleProvider("sample_cleaning_2", "Neha Parmar", "9800000008", "cleaning", "Chandkheda", 3, "Kitchen and bathroom cleaning expert."),
        sampleProvider("sample_ac_1", "Kiran Rathod", "9800000009", "ac_repair", "Navrangpura", 7, "All brands of split and window AC."),
        sampleProvider("sample_ac_2", "Deepak Solanki", "9800000010", "ac_repair", "Bopal", 4, "AC service and gas refill."),
        sampleProvider("sample_ac_3", "Hardik Trivedi", "9800000011", "ac_repair", "Vastrapur", 9, "AC installation specialist."),
        sampleProvider("sample_carpenter_1", "Babulal Mistry", "9800000012", "carpenter", "Maninagar", 15, "Furniture and door work, 15 years experience."),
        sampleProvider("sample_carpenter_2", "Jignesh Panchal", "9800000013", "carpenter", "Satellite", 6, "Modular furniture assembly."),
        sampleProvider("sample_painter_1", "Rajesh Chauhan", "9800000014", "painter", "Chandkheda", 12, "Neat interior painting."),
        sampleProvider("sample_painter_2", "Anil Vaghela", "9800000015", "painter", "Navrangpura", 5, "Wall touch-ups and polish work."),
    )

    private fun sampleProvider(
        id: String, name: String, phone: String, categoryId: String,
        area: String, experienceYears: Int, bio: String,
    ) = Provider(
        id = id, name = name, phone = phone, categoryId = categoryId, area = area,
        experienceYears = experienceYears, bio = bio,
        approvalStatus = ApprovalStatus.APPROVED,
        isAvailable = true,
        createdAt = System.currentTimeMillis(),
    )
}
