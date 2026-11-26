"""Builds docs/PROJECT_REPORT.docx (college format) from plain Python, no extra libraries."""
import os, struct, zipfile
from xml.sax.saxutils import escape

ROOT = r"C:\Users\janvi\Downloads\ServiceHub\docs"
ASSETS = os.path.join(ROOT, "report_assets")
OUT = os.path.join(ROOT, "PROJECT_REPORT.docx")

FONT = "Times New Roman"
HEADER_TEXT = "K.S. SCHOOL OF BUSINESS MANAGEMENT M.Sc.(CA & IT)"
GITHUB = "https://github.com/janvi0802/ServiceHub"

# ------------------------------------------------------------------ image helpers
def image_size(path):
    with open(path, "rb") as f:
        data = f.read()
    if data[:8] == b"\x89PNG\r\n\x1a\n":
        w, h = struct.unpack(">II", data[16:24])
        return w, h
    i = 2
    while i < len(data):
        if data[i] != 0xFF:
            i += 1; continue
        marker = data[i + 1]
        if marker in (0xC0, 0xC1, 0xC2):
            h, w = struct.unpack(">HH", data[i + 5:i + 9])
            return w, h
        seg = struct.unpack(">H", data[i + 2:i + 4])[0]
        i += 2 + seg
    raise ValueError(path)

images = []   # (rid, filename, path)
def add_image(path):
    rid = f"rIdImg{len(images) + 1}"
    name = f"image{len(images) + 1}{os.path.splitext(path)[1].lower()}"
    images.append((rid, name, path))
    return rid

EMU = 914400
def drawing(path, width_in=None, height_in=None):
    w, h = image_size(path)
    if width_in and not height_in:
        height_in = width_in * h / w
    elif height_in and not width_in:
        width_in = height_in * w / h
    cx, cy = int(width_in * EMU), int(height_in * EMU)
    rid = add_image(path)
    n = len(images)
    return (f'<w:r><w:drawing><wp:inline distT="0" distB="0" distL="0" distR="0">'
            f'<wp:extent cx="{cx}" cy="{cy}"/><wp:docPr id="{n}" name="Picture {n}"/>'
            f'<a:graphic xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">'
            f'<a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">'
            f'<pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">'
            f'<pic:nvPicPr><pic:cNvPr id="{n}" name="{os.path.basename(path)}"/><pic:cNvPicPr/></pic:nvPicPr>'
            f'<pic:blipFill><a:blip r:embed="{rid}"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>'
            f'<pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="{cx}" cy="{cy}"/></a:xfrm>'
            f'<a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr></pic:pic>'
            f'</a:graphicData></a:graphic></wp:inline></w:drawing></w:r>')

# ------------------------------------------------------------------ text helpers
def run(text, bold=False, size=None, color=None, italic=False):
    props = ""
    if bold: props += "<w:b/>"
    if italic: props += "<w:i/>"
    if color: props += f'<w:color w:val="{color}"/>'
    if size: props += f'<w:sz w:val="{size * 2}"/><w:szCs w:val="{size * 2}"/>'
    rpr = f"<w:rPr>{props}</w:rPr>" if props else ""
    return f'<w:r>{rpr}<w:t xml:space="preserve">{escape(text)}</w:t></w:r>'

def rich(text):
    """'**bold** rest' -> runs (only one bold part at the start is used)."""
    if text.startswith("**") and "**" in text[2:]:
        b, rest = text[2:].split("**", 1)
        return run(b, bold=True) + run(rest)
    return run(text)

def para(content, style=None, align=None, keep_next=False, page_break_before=False, space_after=None):
    ppr = ""
    if style: ppr += f'<w:pStyle w:val="{style}"/>'
    if keep_next: ppr += "<w:keepNext/>"
    ppr += "<w:keepLines/>"
    if page_break_before: ppr += "<w:pageBreakBefore/>"
    if space_after is not None: ppr += f'<w:spacing w:after="{space_after}"/>'
    if align: ppr += f'<w:jc w:val="{align}"/>'
    return f"<w:p><w:pPr>{ppr}</w:pPr>{content}</w:p>"

body = []
def H1(text):  # every chapter starts on a new page
    body.append(para(run(text), style="Heading1", page_break_before=True, keep_next=True))
def H1_same_page(text):
    body.append(para(run(text), style="Heading1", keep_next=True))
def H2(text, new_page=False):
    body.append(para(run(text), style="Heading2", keep_next=True, page_break_before=new_page))
def P(text, keep_next=False):
    body.append(para(rich(text), keep_next=keep_next, align="both"))
def BULLETS(items):
    # every item but the last keeps with the next, so a list never breaks across pages
    for i, t in enumerate(items):
        last = i == len(items) - 1
        body.append(f'<w:p><w:pPr><w:pStyle w:val="ListBullet"/>{"" if last else "<w:keepNext/>"}<w:keepLines/>'
                    f'<w:numPr><w:ilvl w:val="0"/><w:numId w:val="1"/></w:numPr></w:pPr>{rich(t)}</w:p>')
def NUMBERED(items):
    for i, t in enumerate(items):
        last = i == len(items) - 1
        body.append(f'<w:p><w:pPr><w:pStyle w:val="ListBullet"/>{"" if last else "<w:keepNext/>"}<w:keepLines/>'
                    f'<w:numPr><w:ilvl w:val="0"/><w:numId w:val="2"/></w:numPr></w:pPr>{rich(t)}</w:p>')

PAGE_W = 11906 - 2 * 720   # A4 width minus 0.5" margins (twips)

def TABLE(header, rows, widths):
    total = sum(widths)
    widths = [int(w * PAGE_W / total) for w in widths]
    grid = "".join(f'<w:gridCol w:w="{w}"/>' for w in widths)
    def cell(text, w, head=False, keep=True):
        shade = '<w:shd w:val="clear" w:color="auto" w:fill="D9E2F3"/>' if head else ""
        ppr = f'<w:pPr>{"<w:keepNext/>" if keep else ""}<w:spacing w:before="40" w:after="40"/></w:pPr>'
        return (f'<w:tc><w:tcPr><w:tcW w:w="{w}" w:type="dxa"/>{shade}</w:tcPr>'
                f'<w:p>{ppr}{run(text, bold=head)}</w:p></w:tc>')
    xml = [f'<w:tbl><w:tblPr><w:tblW w:w="{PAGE_W}" w:type="dxa"/><w:tblBorders>'
           + "".join(f'<w:{s} w:val="single" w:sz="6" w:space="0" w:color="808080"/>' for s in ("top", "left", "bottom", "right", "insideH", "insideV"))
           + '</w:tblBorders><w:tblCellMar><w:left w:w="100" w:type="dxa"/><w:right w:w="100" w:type="dxa"/></w:tblCellMar></w:tblPr>'
           + f"<w:tblGrid>{grid}</w:tblGrid>"]
    all_rows = [header] + rows
    for ri, r in enumerate(all_rows):
        head = ri == 0
        last_row = ri == len(all_rows) - 1
        trpr = "<w:trPr><w:cantSplit/>" + ("<w:tblHeader/>" if head else "") + "</w:trPr>"
        xml.append(f"<w:tr>{trpr}" + "".join(cell(t, widths[i], head, keep=not last_row) for i, t in enumerate(r)) + "</w:tr>")
    xml.append("</w:tbl>")
    body.append("".join(xml))
    body.append(para("", space_after=60))

def FIGURE(path, caption, width_in=None, height_in=None):
    body.append(para(drawing(path, width_in, height_in), align="center", keep_next=True, space_after=60))
    body.append(para(run(caption, italic=True, size=11), align="center"))

def SHOT_ROW(items, height_in=4.15):
    """Up to 3 screenshots side by side (borderless table), caption under each."""
    n = len(items)
    w = PAGE_W // 3
    grid = "".join(f'<w:gridCol w:w="{w}"/>' for _ in range(n))
    cells = ""
    for fname, cap in items:
        img = drawing(os.path.join(ASSETS, fname), height_in=height_in)
        cells += (f'<w:tc><w:tcPr><w:tcW w:w="{w}" w:type="dxa"/></w:tcPr>'
                  f'<w:p><w:pPr><w:keepNext/><w:jc w:val="center"/><w:spacing w:after="40"/></w:pPr>{img}</w:p>'
                  f'<w:p><w:pPr><w:jc w:val="center"/><w:spacing w:after="160"/></w:pPr>{run(cap, italic=True, size=11)}</w:p></w:tc>')
    body.append(f'<w:tbl><w:tblPr><w:jc w:val="center"/><w:tblW w:w="{w * n}" w:type="dxa"/>'
                f'<w:tblBorders><w:top w:val="nil"/><w:left w:val="nil"/><w:bottom w:val="nil"/><w:right w:val="nil"/>'
                f'<w:insideH w:val="nil"/><w:insideV w:val="nil"/></w:tblBorders></w:tblPr>'
                f'<w:tblGrid>{grid}</w:tblGrid><w:tr><w:trPr><w:cantSplit/></w:trPr>{cells}</w:tr></w:tbl>')
    body.append(para("", space_after=0))

# ================================================================== COVER (no header/footer)
body.append(para(drawing(os.path.join(ASSETS, "ks_logo.png"), width_in=2.4), align="center", space_after=200))
body.append(para(run("Project Title:", bold=True, size=16), align="center", space_after=60))
body.append(para(run("ServiceHub", bold=True, size=26), align="center", space_after=40))
body.append(para(run("An Android App for Booking Local Home Services", size=16), align="center", space_after=300))
body.append(para(run("(By SEMESTER – IX of FIFTH Year M.Sc.(CA & IT) 2026-27)", size=14), align="center", space_after=1400))
body.append(para(run("Submitted By:", bold=True, size=14), align="left", space_after=100))
body.append(para(run("GROUP ID: ________", size=13), align="left", space_after=100))
body.append(para(run("Student Name: Janvi Panchal", size=13), align="left", space_after=60))
body.append(para(run("Roll No.: 5058", size=13), align="left", space_after=200))
gh = ('<w:hyperlink r:id="rIdGithub" w:history="1"><w:r><w:rPr><w:rStyle w:val="Hyperlink"/><w:sz w:val="26"/></w:rPr>'
      f'<w:t>{GITHUB}</w:t></w:r></w:hyperlink>')
body.append(para(run("GitHub Repository: ", bold=True, size=13) + gh, align="left", space_after=1400))
body.append(para(run("Submitted To:", bold=True, size=14), align="left", space_after=100))
body.append(para(run("K. S. School of Business Management", size=13), align="left", space_after=40))
body.append(para(run("M.Sc. - Computer Applications and Information Technology", size=13), align="left"))

# ================================================================== CONTENTS (Word fills in page numbers)
body.append(para(run("Contents", bold=True, size=16), page_break_before=True, keep_next=True, space_after=200))
toc = ('<w:p><w:r><w:fldChar w:fldCharType="begin" w:dirty="true"/></w:r>'
       '<w:r><w:instrText xml:space="preserve"> TOC \\o "1-2" \\h \\z \\u </w:instrText></w:r>'
       '<w:r><w:fldChar w:fldCharType="separate"/></w:r>'
       + run("Right-click here and choose Update Field to show the contents with page numbers.", italic=True)
       + '<w:r><w:fldChar w:fldCharType="end"/></w:r></w:p>')
body.append(toc)

# ================================================================== 1 + 2
H1("1. Introduction")
P("ServiceHub is an Android app for booking local home services such as plumber, electrician, cleaning, AC repair, "
  "carpenter and painter. Customers book a verified professional at a fixed price and track the job live, providers "
  "manage their jobs and earnings, and an admin keeps the platform trustworthy.")
H2("1.1 Problem Statement")
BULLETS([
    "People find service providers only by word of mouth.",
    "Prices are not known before calling, so customers cannot compare.",
    "There is no way to know whether a provider is verified and trustworthy.",
    "After calling, a customer cannot track when the provider will come.",
    "Providers have little digital visibility and no simple way to manage jobs and payments.",
])
H1_same_page("2. Objectives")
BULLETS([
    "One app for Customers, Providers and Admin; the right home opens after login.",
    "Show every service with a fixed price and the visit fee before booking.",
    "Show only admin-approved providers in the customer's area.",
    "Book a date and time slot; the same provider can never be booked twice for one slot.",
    "Live status tracking on both phones, with a 4-digit PIN to start the job safely.",
    "Clear payment (Cash or UPI), receipt, rating and a 7-day free redo.",
])
H2("2.1 Scope")
BULLETS([
    "Registration, login, roles and admin approval of providers",
    "Service catalogue, search and provider list by area",
    "Booking, live tracking, cancel and reschedule",
    "Payment (Cash / UPI test mode), coupons, receipt and rating",
    "Chat, in-app and push notifications, complaints and admin dashboard",
])
H2("2.2 Existing System and its Drawbacks")
TABLE(["Existing system (calls, word of mouth)", "Drawback"], [
    ["Ask neighbours or call a known person", "Few choices, no verification"],
    ["Price decided after the visit", "No clear or fixed pricing"],
    ["No booking record", "No tracking, no proof, no receipt"],
    ["No feedback system", "Good and bad providers look the same"],
], [50, 50])

# ================================================================== 3
H1("3. System Requirements")
H2("3.1 Technology Stack")
TABLE(["Item", "Used"], [
    ["Programming language", "Kotlin"],
    ["IDE", "Android Studio"],
    ["Database", "Firebase Cloud Firestore"],
    ["UI design", "Jetpack Compose (Material 3)"],
], [40, 60])
H2("3.2 Functional Requirements")
P("**Customer side**", keep_next=True)
BULLETS([
    "Register, login, forgot password, edit profile, change email, saved addresses",
    "Browse categories and services with prices; search services",
    "Book: category → service → professional → date and slot → review → confirm",
    "Apply coupons from \"Offers for you\"",
    "Track status live, see the start PIN, call and chat with the professional",
    "Cancel, reschedule, change payment method, pay by UPI or cash",
    "Rate and review, view receipt, free redo within 7 days, report a problem",
])
P("**Provider side**", keep_next=True)
BULLETS([
    "Register and wait for admin approval",
    "Set availability and weekly days off",
    "Accept or reject requests, \"I'm on my way\", start the job with the customer's PIN",
    "Complete the job with extra parts; confirm cash or see UPI payment received",
    "See today's jobs, earnings, history and reviews; chat with customers",
])
P("**Admin side**", keep_next=True)
BULLETS([
    "Dashboard with counts and items needing attention",
    "Approve or reject providers; block or unblock users; change a user's password",
    "Manage services, categories and coupons",
    "View all bookings with filters and details; resolve complaints",
])
H2("3.3 Non-Functional Requirements")
BULLETS([
    "**Real-time:** changes appear on the other phone within seconds.",
    "**Security:** Firestore security rules protect all data.",
    "**Reliability:** booking changes use transactions, so data stays correct.",
    "**Usability:** simple screens with a \"Next step\" box.",
    "**Cost:** only free plans are used.",
])

# ================================================================== 4
H1("4. System Design")
H2("4.1 Frontend")
BULLETS([
    "Android app written in Kotlin with Jetpack Compose (Material 3)",
    "One Activity; screens connected with Navigation Compose",
    "Separate home and bottom bar for Customer, Provider and Admin",
])
H2("4.2 Backend")
BULLETS([
    "Firebase Authentication for login (email and password)",
    "Firebase Cloud Messaging for push notifications",
    "Razorpay (test mode) for UPI payments",
    "Small free Vercel server for admin password change and sending pushes",
])
H2("4.3 Database")
BULLETS([
    "Firebase Cloud Firestore (cloud, real-time, shared by all phones)",
    "Main collections: users, providers, services, bookings, slots, reviews, coupons, notifications, complaints",
    "Security rules decide who can read and change each record",
])
H2("4.4 ER Diagram", new_page=True)
FIGURE(os.path.join(ASSETS, "d_er.png"), "Figure 4.1: ER diagram of ServiceHub", width_in=7.2)
H2("4.5 Activity Diagram (Booking a Service)", new_page=True)
FIGURE(os.path.join(ASSETS, "d_activity.png"), "Figure 4.2: Activity diagram – booking a service", height_in=8.6)
H2("4.6 Booking Status Flow", new_page=True)
FIGURE(os.path.join(ASSETS, "d_status.png"), "Figure 4.3: Booking status flow", width_in=7.2)
H2("4.7 Use Case Diagram")
FIGURE(os.path.join(ASSETS, "d_usecase.png"), "Figure 4.4: Use case diagram", width_in=7.2)

# ================================================================== 5
H1("5. Implementation")
modules = [
    ("5.1 Authentication Module", ["Register as customer or provider; login and stay logged in",
                                   "Role check opens the right home; blocked users are logged out",
                                   "Forgot password and change email"]),
    ("5.2 Catalogue and Search Module", ["Categories and services with fixed prices, loaded live",
                                         "Search by service name; admin adds or hides categories and services"]),
    ("5.3 Booking Module", ["Guided form: category → service → professional → date and slot",
                            "Slot shown as Booked or On hold; double booking stopped with a transaction",
                            "Review page with price details and coupons; random 4-digit start PIN"]),
    ("5.4 Job Management Module (Provider)", ["Accept or reject with a reason, \"I'm on my way\", start with PIN",
                                              "Complete job with extra parts (all extras together up to ₹50,000)",
                                              "Earnings: today, this week, to collect"]),
    ("5.5 Payment Module", ["Cash: the provider confirms \"Cash collected\"",
                            "UPI: the customer pays with Razorpay test mode; booking marked Paid automatically",
                            "Receipt with all amounts and payment ID"]),
    ("5.6 Communication Module", ["In-app notifications with unread badge",
                                  "Push notifications that open the right booking",
                                  "Chat between customer and provider for each booking"]),
    ("5.7 Review and Support Module", ["Rating once per booking; provider average updated",
                                       "7-day free redo with a redo PIN; report a problem to the admin"]),
    ("5.8 Admin Module", ["Dashboard, approvals, users, bookings, services, categories, coupons, complaints"]),
    ("5.9 Security", ["Users change only their own data; only the admin approves providers and edits services",
                      "A booking is visible only to its customer, its provider and the admin"]),
]
for title, pts in modules:
    H2(title)
    BULLETS(pts)

# ================================================================== 6
H1("6. Testing")
P("Testing was done on two real Android phones (one customer, one provider) and with 50 automatic logic tests.")
TABLE(["No.", "Test case", "Expected result", "Result"], [
    ["1", "Register and login", "Correct home opens for each role", "Pass"],
    ["2", "New provider registers", "Waits for approval; visible after admin approves", "Pass"],
    ["3", "Book a service", "Booking saved as Requested; provider notified", "Pass"],
    ["4", "Same slot booked twice", "Second booking refused", "Pass"],
    ["5", "Provider accepts", "Customer sees Accepted and the PIN live", "Pass"],
    ["6", "Wrong start PIN", "Job does not start", "Pass"],
    ["7", "Complete job with extras", "New total shown to both", "Pass"],
    ["8", "UPI payment (test)", "Booking Paid, provider notified", "Pass"],
    ["9", "Cash collected", "Booking Paid, earnings updated", "Pass"],
    ["10", "Apply coupon", "Discount shown, total reduced", "Pass"],
    ["11", "Rate service", "Rating saved once, average updated", "Pass"],
    ["12", "Cancel / reschedule", "Slot freed or moved, provider told", "Pass"],
    ["13", "Chat and push", "Message arrives; push opens the chat", "Pass"],
    ["14", "Admin blocks a user", "User logged out, cannot log in", "Pass"],
    ["15", "Admin edits a price", "New price shown to customers", "Pass"],
], [7, 30, 48, 15])

# ================================================================== 7 SCREENSHOTS
H1("7. Screenshots")
H2("7.1 Customer")
SHOT_ROW([("c01_home.jpg", "Home"), ("c02_book_service.jpg", "Book a service"), ("c03_choose_provider.jpg", "Choose a provider")])
SHOT_ROW([("c04_booking_form.jpg", "Booking form"), ("c05_review_booking.jpg", "Review booking"), ("c06_booking_details_pay.jpg", "Booking details and payment")])
body.append(para("", page_break_before=True, space_after=0))
SHOT_ROW([("c07_rate_service.jpg", "Rate service"), ("c08_my_bookings.jpg", "My bookings"), ("c09_notifications.jpg", "Notifications")])
SHOT_ROW([("c10_profile.jpg", "Profile")])
H2("7.2 Provider", new_page=True)
SHOT_ROW([("p01_provider_home.jpg", "Provider home and earnings"), ("p02_job_pin.jpg", "Start job with PIN"), ("p03_complete_job.jpg", "Complete job")])
SHOT_ROW([("p04_job_completed.jpg", "Completed job details"), ("p05_job_history.jpg", "Job history"), ("p06_provider_notifications.jpg", "Provider notifications")])
H2("7.3 Admin", new_page=True)
SHOT_ROW([("a01_dashboard.jpg", "Admin dashboard"), ("a02_all_bookings.jpg", "All bookings"), ("a03_manage_services.jpg", "Manage services")])

# ================================================================== 8
H1("8. Conclusion")
BULLETS([
    "ServiceHub is a working Android app used on two real phones at the same time.",
    "It covers the full booking journey: search, book, track, start with PIN, complete, pay and rate.",
    "Customers get fixed prices and verified providers; providers get jobs and clear earnings.",
    "Firestore transactions and security rules keep the data correct and safe.",
    "The whole project was built with free services.",
])
H2("8.1 Future Scope")
BULLETS([
    "Real online payments (live mode) with a server-side payment check",
    "Live GPS tracking of the professional on a map",
    "Photo upload of the problem and provider ID documents",
    "Phone OTP login",
    "Hindi and Gujarati languages, and dark mode",
])

# ================================================================== 9
H1("9. References")
NUMBERED([
    "Android Developers – Jetpack Compose: developer.android.com/compose",
    "Android Developers – Guide to app architecture: developer.android.com/topic/architecture",
    "Firebase Documentation: firebase.google.com/docs",
    "Kotlin Documentation: kotlinlang.org/docs",
    "Razorpay Documentation: razorpay.com/docs",
    "Vercel Documentation: vercel.com/docs",
    f"Project repository: {GITHUB}",
])

# ------------------------------------------------------------------ document parts
NS = ('xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" '
      'xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" '
      'xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing" '
      'xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" '
      'xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture"')

sect = ('<w:sectPr><w:headerReference w:type="default" r:id="rIdHeader"/>'
        '<w:footerReference w:type="default" r:id="rIdFooter"/>'
        '<w:pgSz w:w="11906" w:h="16838"/>'
        '<w:pgMar w:top="720" w:right="720" w:bottom="720" w:left="720" w:header="360" w:footer="360" w:gutter="0"/>'
        '<w:titlePg/></w:sectPr>')   # titlePg: no header/footer on the cover page
document = f'<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:document {NS}><w:body>{"".join(body)}{sect}</w:body></w:document>'

header = (f'<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:hdr {NS}>'
          f'<w:p><w:pPr><w:jc w:val="center"/><w:pBdr><w:bottom w:val="single" w:sz="6" w:space="1" w:color="808080"/></w:pBdr></w:pPr>'
          f'{run(HEADER_TEXT, bold=True, size=12)}</w:p></w:hdr>')
footer = (f'<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:ftr {NS}><w:p><w:pPr><w:jc w:val="center"/></w:pPr>'
          '<w:r><w:fldChar w:fldCharType="begin"/></w:r><w:r><w:instrText xml:space="preserve"> PAGE </w:instrText></w:r>'
          '<w:r><w:fldChar w:fldCharType="separate"/></w:r><w:r><w:t>1</w:t></w:r><w:r><w:fldChar w:fldCharType="end"/></w:r>'
          '</w:p></w:ftr>')

def rpr(size, bold=False, color=None):
    return (f'<w:rPr><w:rFonts w:ascii="{FONT}" w:hAnsi="{FONT}" w:cs="{FONT}" w:eastAsia="{FONT}"/>'
            + ("<w:b/>" if bold else "") + (f'<w:color w:val="{color}"/>' if color else "")
            + f'<w:sz w:val="{size * 2}"/><w:szCs w:val="{size * 2}"/></w:rPr>')

styles = f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:docDefaults><w:rPrDefault>{rpr(12)}</w:rPrDefault>
<w:pPrDefault><w:pPr><w:spacing w:after="120" w:line="276" w:lineRule="auto"/></w:pPr></w:pPrDefault></w:docDefaults>
<w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/><w:qFormat/>{rpr(12)}</w:style>
<w:style w:type="paragraph" w:styleId="Heading1"><w:name w:val="heading 1"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:qFormat/>
<w:pPr><w:keepNext/><w:keepLines/><w:spacing w:before="120" w:after="200"/><w:outlineLvl w:val="0"/></w:pPr>{rpr(16, True)}</w:style>
<w:style w:type="paragraph" w:styleId="Heading2"><w:name w:val="heading 2"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:qFormat/>
<w:pPr><w:keepNext/><w:keepLines/><w:spacing w:before="200" w:after="100"/><w:outlineLvl w:val="1"/></w:pPr>{rpr(14, True)}</w:style>
<w:style w:type="paragraph" w:styleId="ListBullet"><w:name w:val="List Bullet"/><w:basedOn w:val="Normal"/>
<w:pPr><w:spacing w:after="60"/></w:pPr></w:style>
<w:style w:type="paragraph" w:styleId="TOC1"><w:name w:val="toc 1"/><w:basedOn w:val="Normal"/><w:pPr><w:spacing w:after="80"/></w:pPr>{rpr(12, True)}</w:style>
<w:style w:type="paragraph" w:styleId="TOC2"><w:name w:val="toc 2"/><w:basedOn w:val="Normal"/><w:pPr><w:spacing w:after="60"/><w:ind w:left="400"/></w:pPr>{rpr(12)}</w:style>
<w:style w:type="character" w:styleId="Hyperlink"><w:name w:val="Hyperlink"/><w:rPr><w:color w:val="0563C1"/><w:u w:val="single"/></w:rPr></w:style>
</w:styles>'''

numbering = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:numbering xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:abstractNum w:abstractNumId="0"><w:lvl w:ilvl="0"><w:start w:val="1"/><w:numFmt w:val="bullet"/><w:lvlText w:val="&#8226;"/>
<w:lvlJc w:val="left"/><w:pPr><w:ind w:left="720" w:hanging="360"/></w:pPr><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/></w:rPr></w:lvl></w:abstractNum>
<w:abstractNum w:abstractNumId="1"><w:lvl w:ilvl="0"><w:start w:val="1"/><w:numFmt w:val="decimal"/><w:lvlText w:val="%1."/>
<w:lvlJc w:val="left"/><w:pPr><w:ind w:left="720" w:hanging="360"/></w:pPr></w:lvl></w:abstractNum>
<w:num w:numId="1"><w:abstractNumId w:val="0"/></w:num><w:num w:numId="2"><w:abstractNumId w:val="1"/></w:num>
</w:numbering>'''

settings = ('<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
            '<w:settings xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">'
            '<w:updateFields w:val="true"/><w:defaultTabStop w:val="720"/></w:settings>')

rels_doc = ['<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">',
            '<Relationship Id="rIdStyles" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>',
            '<Relationship Id="rIdNum" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/numbering" Target="numbering.xml"/>',
            '<Relationship Id="rIdSettings" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/settings" Target="settings.xml"/>',
            '<Relationship Id="rIdHeader" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/header" Target="header1.xml"/>',
            '<Relationship Id="rIdFooter" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/footer" Target="footer1.xml"/>',
            f'<Relationship Id="rIdGithub" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink" Target="{GITHUB}" TargetMode="External"/>']
for rid, name, _ in images:
    rels_doc.append(f'<Relationship Id="{rid}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/{name}"/>')
rels_doc.append("</Relationships>")

content_types = ('<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
                 '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">'
                 '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>'
                 '<Default Extension="xml" ContentType="application/xml"/>'
                 '<Default Extension="png" ContentType="image/png"/><Default Extension="jpg" ContentType="image/jpeg"/>'
                 '<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>'
                 '<Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>'
                 '<Override PartName="/word/numbering.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.numbering+xml"/>'
                 '<Override PartName="/word/settings.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.settings+xml"/>'
                 '<Override PartName="/word/header1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.header+xml"/>'
                 '<Override PartName="/word/footer1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml"/>'
                 '</Types>')
root_rels = ('<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
             '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
             '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>'
             '</Relationships>')

with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as z:
    z.writestr("[Content_Types].xml", content_types)
    z.writestr("_rels/.rels", root_rels)
    z.writestr("word/document.xml", document)
    z.writestr("word/styles.xml", styles)
    z.writestr("word/numbering.xml", numbering)
    z.writestr("word/settings.xml", settings)
    z.writestr("word/header1.xml", header)
    z.writestr("word/footer1.xml", footer)
    z.writestr("word/_rels/document.xml.rels", "".join(rels_doc))
    for rid, name, path in images:
        z.write(path, f"word/media/{name}")
print("written", OUT, len(images), "images")
