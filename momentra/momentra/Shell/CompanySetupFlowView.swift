import FirebaseAuth
import PhotosUI
import SwiftUI
import UIKit

/// Figma 695:4455 — Company Setup wizard (692:38403 / 38453 / 38549 / 38635).
struct CompanySetupFlowView: View {
    var onClose: () -> Void
    var onActivated: (CompanySummary) -> Void

    @State private var step = 1
    @State private var stepForward = true
    @State private var welcomeAppeared = false
    @State private var companyName = ""
    @State private var industry = "Retail / Kirana"
    @State private var companySize = "Solo (1)"
    @State private var entityType = "Sole Prop"
    @State private var gstin = ""
    @State private var currency = "₹ INR — Indian Rupee"
    @State private var fyCycle = "Apr-Mar"
    @State private var timezone = "IST (UTC+5:30)"
    @State private var audience = BusinessAudience.smallShop
    @State private var industryTemplate = IndustryTemplateCatalog.kirana
    @State private var templateManuallyPicked = false
    @State private var sellWhat = "Physical items"
    @State private var billHow = "Counter billing"
    @State private var customMoney = true
    @State private var customDaily = true
    @State private var customTeam = false
    @State private var structure = "Single Location"
    @State private var locations: [(name: String, area: String, primary: Bool, color: Color)] = []
    @State private var members: [(initials: String, name: String, role: String, scope: String, color: Color, you: Bool)] = [
        CompanySetupFlowView.ownerMember()
    ]
    @State private var inviteText = ""
    @State private var logoItem: PhotosPickerItem?
    @State private var logoImage: UIImage?
    @State private var logoData: Data?
    @State private var logoError: String?
    @State private var nameError: String?
    @State private var activateError: String?
    @State private var activateWarning: String?
    @State private var showLocationEditor = false
    @State private var editingLocationIndex: Int?
    @State private var locationNameDraft = ""
    @State private var locationAreaDraft = ""
    @State private var editingMemberIndex: Int?
    @State private var memberNameDraft = ""
    @State private var showAddPeople = false
    @State private var activating = false
    @State private var showJoinCode = false
    @State private var pendingActivation: CompanySummary?
    @State private var pendingInviteMessage: String?
    @State private var pendingInvitePhone: String?
    @State private var showPostInviteChooser = false

    private let bg = Color(hex: "#0C0F15")
    private let accent = Color(hex: "#818CF8")
    private let card = Color(hex: "#161B26")
    private let border = Color(hex: "#1E293B")
    private let muted = Color(hex: "#94A3B8")
    private let dim = Color(hex: "#64748B")
    private let green = Color(hex: "#10B981")
    private let figmaEase = Animation.timingCurve(0.16, 1, 0.3, 1, duration: 0.4)

    private func go(to next: Int) {
        stepForward = next > step
        withAnimation(figmaEase) { step = next }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                header
                Group {
                    switch step {
                    case 1: welcome
                    case 2: companyForm
                    case 3: locationsForm
                    default: launchForm
                    }
                }
                .id(step)
                .transition(.asymmetric(
                    insertion: .move(edge: stepForward ? .trailing : .leading).combined(with: .opacity),
                    removal: .move(edge: stepForward ? .leading : .trailing).combined(with: .opacity)
                ))
            }
            .padding(.horizontal, 24)
            .padding(.top, 16)
            .padding(.bottom, 40)
        }
        .background(bg.ignoresSafeArea())
        .trackScreen(AnalyticsScreens.companySetup)
        .confirmationDialog("Invite your team via…", isPresented: $showPostInviteChooser, titleVisibility: .visible) {
            Button("Messages") {
                if let message = pendingInviteMessage {
                    InviteOutboundShare.sendSms(phone: pendingInvitePhone, message: message)
                }
                finishActivation()
            }
            Button("WhatsApp") {
                if let message = pendingInviteMessage {
                    InviteOutboundShare.sendWhatsApp(phone: pendingInvitePhone, message: message)
                }
                finishActivation()
            }
            Button("Not now", role: .cancel) {
                finishActivation()
            }
        } message: {
            Text("Share the company invite link through Messages or WhatsApp.")
        }
        .sheet(isPresented: $showAddPeople) {
            BusinessAddPeopleSheet { name in
                let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
                guard !trimmed.isEmpty else { return }
                members.append((String(trimmed.prefix(2)).uppercased(), trimmed, "Member", "All Locations", accent, false))
            }
        }
        .sheet(isPresented: $showLocationEditor) {
            locationEditorSheet
        }
        .sheet(isPresented: Binding(
            get: { editingMemberIndex != nil },
            set: { if !$0 { editingMemberIndex = nil } }
        )) {
            memberEditorSheet
        }
        .onChange(of: logoItem) { _, item in
            guard let item else { return }
            Task {
                if let data = try? await item.loadTransferable(type: Data.self),
                   let image = UIImage(data: data) {
                    logoData = data
                    logoImage = image
                    logoError = nil
                } else {
                    logoError = "Could not open that photo"
                }
            }
        }
    }

    private static func ownerMember() -> (initials: String, name: String, role: String, scope: String, color: Color, you: Bool) {
        let user = Auth.auth().currentUser
        let name = PersonLabel.personName(user?.displayName, fallback: "You")
        return (String(name.prefix(2)).uppercased(), name, "Owner", "All Locations", Color(hex: "#818CF8"), true)
    }

    private func finishActivation() {
        showPostInviteChooser = false
        if let pending = pendingActivation {
            pendingActivation = nil
            pendingInviteMessage = nil
            pendingInvitePhone = nil
            onActivated(pending)
        }
    }

    private var header: some View {
        HStack {
            Button(action: onClose) {
                HStack(spacing: 4) {
                    Text("✕").font(.system(size: 14))
                    Text("Close").font(.system(size: 14, weight: .medium))
                }
                .foregroundStyle(dim)
            }
            .buttonStyle(.plain)
            Spacer()
            Text("ONBOARDING \(step)/4")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(accent)
        }
    }

    private var welcome: some View {
        VStack(spacing: 20) {
            Spacer().frame(height: 24)
            ZStack {
                RoundedRectangle(cornerRadius: 40)
                    .fill(card)
                    .overlay(RoundedRectangle(cornerRadius: 40).stroke(accent.opacity(0.2), lineWidth: 1))
                    .frame(width: 80, height: 80)
                Text("M").font(.system(size: 36, weight: .heavy)).foregroundStyle(accent)
            }
            .scaleEffect(welcomeAppeared ? 1 : 0.92)
            .opacity(welcomeAppeared ? 1 : 0)
            VStack(spacing: 8) {
                Text("Set Up Your Business").font(.system(size: 24, weight: .bold)).foregroundStyle(.white)
                Text("Tell us a little about your business to get started.")
                    .font(.system(size: 14))
                    .foregroundStyle(muted)
                    .multilineTextAlignment(.center)
            }
            .opacity(welcomeAppeared ? 1 : 0)
            .offset(y: welcomeAppeared ? 0 : 16)
            benefit("Quick Setup", "Takes about 2 minutes", accent.opacity(0.1))
                .opacity(welcomeAppeared ? 1 : 0)
                .offset(y: welcomeAppeared ? 0 : 20)
            benefit("Add locations anytime", "For shops, offices or branches", green.opacity(0.1))
                .opacity(welcomeAppeared ? 1 : 0)
                .offset(y: welcomeAppeared ? 0 : 20)
            benefit("Change anytime", "You can update these details later", Color(hex: "#F59E0B").opacity(0.1))
                .opacity(welcomeAppeared ? 1 : 0)
                .offset(y: welcomeAppeared ? 0 : 20)
            progressDots(current: 1, label: "Current: Welcome Setup")
                .opacity(welcomeAppeared ? 1 : 0)
            Spacer().frame(height: 24)
            primaryButton("Get Started →") { go(to: 2) }
                .opacity(welcomeAppeared ? 1 : 0)
                .offset(y: welcomeAppeared ? 0 : 12)
            Button("I already have a company code") { showJoinCode = true }
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(dim)
                .buttonStyle(.plain)
                .opacity(welcomeAppeared ? 1 : 0)
        }
        .frame(maxWidth: .infinity)
        .onAppear {
            welcomeAppeared = false
            withAnimation(figmaEase.delay(0.05)) { welcomeAppeared = true }
        }
        .sheet(isPresented: $showJoinCode) {
            CompanyJoinCodeSheet(
                onClose: { showJoinCode = false },
                onJoined: { company in
                    showJoinCode = false
                    onActivated(company)
                }
            )
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.visible)
        }
    }

    private func benefit(_ title: String, _ body: String, _ iconBg: Color) -> some View {
        HStack(spacing: 14) {
            Circle().fill(iconBg).frame(width: 36, height: 36)
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.system(size: 14, weight: .semibold)).foregroundStyle(.white)
                Text(body).font(.system(size: 12)).foregroundStyle(muted)
            }
            Spacer()
        }
        .padding(16)
        .background(card)
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(border, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func progressDots(current: Int, label: String) -> some View {
        VStack(spacing: 8) {
            HStack(spacing: 8) {
                ForEach(1...4, id: \.self) { i in
                    if i == current {
                        Text("\(i)")
                            .font(.system(size: 9, weight: .heavy))
                            .foregroundStyle(bg)
                            .frame(width: 16, height: 16)
                            .background(accent)
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    } else {
                        Circle().fill(border).frame(width: 8, height: 8)
                    }
                    if i < 4 { Rectangle().fill(border).frame(width: 40, height: 2) }
                }
            }
            Text(label).font(.system(size: 11, weight: .semibold)).foregroundStyle(accent)
        }
    }

    private func stepStrip(active: Int) -> some View {
        let labels = ["Welcome", "Company", "Locations", "Launch"]
        return HStack {
            ForEach(Array(labels.enumerated()), id: \.offset) { idx, label in
                let n = idx + 1
                let done = n < active
                let current = n == active
                HStack(spacing: 6) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 10)
                            .fill(done ? green : current ? accent : card)
                            .overlay(
                                RoundedRectangle(cornerRadius: 10)
                                    .stroke(border, lineWidth: done || current ? 0 : 1)
                            )
                            .frame(width: 20, height: 20)
                        Text(done ? "✓" : "\(n)")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundStyle(done || current ? bg : muted)
                    }
                    Text(label)
                        .font(.system(size: 11, weight: current ? .bold : .medium))
                        .foregroundStyle(current ? .white : muted)
                        .lineLimit(1)
                }
                .frame(maxWidth: .infinity)
            }
        }
    }

    private var companyForm: some View {
        VStack(alignment: .leading, spacing: 12) {
            stepStrip(active: 2)
            section("01", "COMPANY PROFILE") {
                fieldLabel("WHO IS THIS FOR?")
                pillRow(
                    ["Small shop / Retail", "Growing business"],
                    selected: audience == BusinessAudience.smallShop ? "Small shop / Retail" : "Growing business"
                ) { label in
                    audience = label.contains("Small") ? BusinessAudience.smallShop : BusinessAudience.growing
                    if audience == BusinessAudience.smallShop {
                        if structure == "Multi-Location" || structure == "Multi-Unit" {
                            structure = "Single Location"
                        }
                        industry = "Retail / Kirana"
                        companySize = "Solo (1)"
                        entityType = "Sole Prop"
                        industryTemplate = IndustryTemplateCatalog.kirana
                        templateManuallyPicked = false
                    } else {
                        industry = "Technology & Software"
                        companySize = "Small (2-25)"
                        entityType = "Pvt Ltd"
                    }
                }
                if BusinessAudience.isSmallShop(audience) {
                    fieldLabel("WHAT DO YOU SELL?")
                    pillRow(
                        ["Physical items", "Made-to-order", "Services & repairs"],
                        selected: sellWhat
                    ) { answer in
                        sellWhat = answer
                        if !templateManuallyPicked {
                            industryTemplate = IndustryTemplateCatalog.suggest(sells: answer, bills: billHow)
                            if let ind = industryTemplate.profileDefaults["industry"] as? String {
                                industry = ind
                            }
                        }
                    }
                    fieldLabel("HOW DO YOU BILL?")
                    pillRow(
                        ["Counter billing", "On-site / digital invoices"],
                        selected: billHow
                    ) { answer in
                        billHow = answer
                        if !templateManuallyPicked {
                            industryTemplate = IndustryTemplateCatalog.suggest(sells: sellWhat, bills: answer)
                            if let ind = industryTemplate.profileDefaults["industry"] as? String {
                                industry = ind
                            }
                        }
                    }
                    fieldLabel("STARTER TEMPLATE")
                    Text("Start with a template — change anytime in Company Settings.")
                        .font(.system(size: 12))
                        .foregroundStyle(dim)
                    ForEach(IndustryTemplateCatalog.all) { tpl in
                        Button {
                            industryTemplate = tpl
                            templateManuallyPicked = true
                            if let ind = tpl.profileDefaults["industry"] as? String {
                                industry = ind
                            }
                        } label: {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(tpl.label)
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundStyle(.white)
                                Text(tpl.subtitle)
                                    .font(.system(size: 12))
                                    .foregroundStyle(muted)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(12)
                            .background(industryTemplate.id == tpl.id ? Color(hex: "#818CF8").opacity(0.15) : bg)
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(industryTemplate.id == tpl.id ? Color(hex: "#818CF8") : border)
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                        .buttonStyle(.plain)
                    }
                    if industryTemplate.isCustom {
                        fieldLabel("WHICH MOMENTS TO START WITH?")
                        moduleToggle("Money & Cash Flow", isOn: $customMoney)
                        moduleToggle("Daily Business", isOn: $customDaily)
                        moduleToggle("Team & Work", isOn: $customTeam)
                    }
                }
                fieldLabel("COMPANY NAME")
                textField($companyName, placeholder: "Your shop or company name")
                    .onChange(of: companyName) { _, next in
                        if nameError != nil, !next.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                            nameError = nil
                        }
                    }
                if let nameError, !nameError.isEmpty {
                    Text(nameError)
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(Color(hex: "#F87171"))
                }
                fieldLabel("INDUSTRY")
                Menu {
                    ForEach([
                        "Retail / Kirana",
                        "Pet store / Specialty",
                        "Manufacturing / Workshop",
                        "Restaurant / F&B",
                        "Fashion / Apparel",
                        "Services",
                        "Wholesale",
                        "Technology & Software",
                        "E-commerce",
                        "Other",
                    ], id: \.self) { option in
                        Button(option) { industry = option }
                    }
                } label: {
                    HStack {
                        Text(industry).foregroundStyle(.white)
                        Spacer()
                        Text("▼").foregroundStyle(dim)
                    }
                    .padding(12)
                    .frame(height: 44)
                    .background(bg)
                    .overlay(RoundedRectangle(cornerRadius: 10).stroke(border))
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                }
                fieldLabel("COMPANY SIZE")
                pillRow(["Solo (1)", "Small (2-25)", "Medium (26-100)"], selected: companySize) { companySize = $0 }
                fieldLabel("COMPANY LOGO")
                PhotosPicker(selection: $logoItem, matching: .images) {
                    ZStack {
                        if let logoImage {
                            Image(uiImage: logoImage)
                                .resizable()
                                .scaledToFill()
                                .frame(maxWidth: .infinity)
                                .frame(height: 64)
                                .clipped()
                        } else {
                            Text(BusinessAudience.isSmallShop(audience) ? "Upload shop logo" : "Upload corporate logo")
                                .font(.system(size: 13, weight: .medium))
                                .foregroundStyle(muted)
                                .frame(maxWidth: .infinity)
                                .frame(height: 64)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 64)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                    .overlay(RoundedRectangle(cornerRadius: 10).stroke(border, style: StrokeStyle(lineWidth: 1, dash: [6])))
                }
                .buttonStyle(.plain)
                if let logoError {
                    Text(logoError).font(.system(size: 12)).foregroundStyle(Color(hex: "#F87171"))
                }
            }
            section("02", "LEGAL & FINANCIAL") {
                fieldLabel("ENTITY TYPE")
                pillRow(["Pvt Ltd", "LLP", "Partnership", "Sole Prop"], selected: entityType) { entityType = $0 }
                fieldLabel("GSTIN")
                textField($gstin, placeholder: "Enter 15-digit GSTIN")
                fieldLabel("PRIMARY CURRENCY")
                dropdown(currency)
                fieldLabel("FINANCIAL YEAR CYCLE")
                fyRow
                fieldLabel("TIMEZONE")
                dropdown(timezone)
            }
            primaryButton("Continue") { go(to: 3) }
            Button("Back") { go(to: 1) }
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(dim)
                .frame(maxWidth: .infinity)
                .buttonStyle(.plain)
        }
    }

    private var locationsForm: some View {
        VStack(alignment: .leading, spacing: 12) {
            stepStrip(active: 3)
            section("01", "BUSINESS STRUCTURE") {
                Text("How is your business organized?")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(.white)
                structureRow("Single Location", "One office or store")
                if !BusinessAudience.isSmallShop(audience) {
                    structureRow("Multi-Location", "Multiple branches or offices")
                    structureRow("Multi-Unit", "Different business units or brands")
                }
            }
            section("02", "YOUR LOCATIONS") {
                if locations.isEmpty {
                    Text("No locations yet").font(.system(size: 13)).foregroundStyle(muted)
                }
                ForEach(Array(locations.enumerated()), id: \.offset) { index, loc in
                    HStack(spacing: 0) {
                        Rectangle().fill(loc.color).frame(width: 4, height: 52)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(loc.name).font(.system(size: 13, weight: .semibold)).foregroundStyle(.white)
                            Text(loc.area).font(.system(size: 11)).foregroundStyle(muted)
                        }
                        .padding(.horizontal, 12)
                        Spacer()
                        if loc.primary {
                            Text("Primary")
                                .font(.system(size: 9, weight: .bold))
                                .foregroundStyle(green)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(green.opacity(0.08))
                                .overlay(RoundedRectangle(cornerRadius: 4).stroke(green.opacity(0.2)))
                                .clipShape(RoundedRectangle(cornerRadius: 4))
                        }
                        Button {
                            editingLocationIndex = index
                            locationNameDraft = loc.name
                            locationAreaDraft = loc.area
                            showLocationEditor = true
                        } label: {
                            Text("✎").foregroundStyle(dim).padding(.trailing, 12)
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel("Edit location")
                    }
                    .background(bg)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                }
                Button {
                    editingLocationIndex = nil
                    locationNameDraft = ""
                    locationAreaDraft = ""
                    showLocationEditor = true
                } label: {
                    Text("+ Add another location")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(accent)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .buttonStyle(.plain)
                VStack(alignment: .leading, spacing: 4) {
                    Text("Locations inherit company defaults")
                        .font(.system(size: 12, weight: .semibold)).foregroundStyle(.white)
                    Text("Currency: ₹ INR · Budget: Company default · Reporting: Consolidated")
                        .font(.system(size: 10)).foregroundStyle(muted)
                }
                .padding(12)
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(border, style: StrokeStyle(lineWidth: 1, dash: [5])))
            }
            primaryButton("Continue") { go(to: 4) }
            Button("Back") { go(to: 2) }
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(dim)
                .frame(maxWidth: .infinity)
                .buttonStyle(.plain)
        }
    }

    private var launchForm: some View {
        VStack(alignment: .leading, spacing: 12) {
            stepStrip(active: 4)
            section("01", "INVITE YOUR TEAM") {
                Text("Add people to invite after activation — share the invite link when you’re ready")
                    .font(.system(size: 14, weight: .medium)).foregroundStyle(.white)
                ForEach(Array(members.enumerated()), id: \.offset) { index, m in
                    HStack(spacing: 12) {
                        Text(m.initials)
                            .font(.system(size: 12, weight: .bold))
                            .foregroundStyle(bg)
                            .frame(width: 36, height: 36)
                            .background(m.color)
                            .clipShape(Circle())
                        VStack(alignment: .leading, spacing: 2) {
                            HStack(spacing: 6) {
                                Text(m.name).font(.system(size: 13, weight: .semibold)).foregroundStyle(.white)
                                Text(m.role)
                                    .font(.system(size: 9, weight: .bold))
                                    .foregroundStyle(m.color)
                                    .padding(.horizontal, 6).padding(.vertical, 2)
                                    .background(m.color.opacity(0.1))
                                    .overlay(RoundedRectangle(cornerRadius: 4).stroke(m.color.opacity(0.2)))
                            }
                            Text(m.scope).font(.system(size: 11)).foregroundStyle(muted)
                        }
                        Spacer()
                        if m.you {
                            Text("You").font(.system(size: 12)).foregroundStyle(dim)
                        } else {
                            Button {
                                editingMemberIndex = index
                                memberNameDraft = m.name
                            } label: {
                                Text("✎").font(.system(size: 12)).foregroundStyle(dim)
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel("Edit member")
                            Button {
                                members.remove(at: index)
                            } label: {
                                Text("✕").font(.system(size: 12)).foregroundStyle(dim)
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel("Remove member")
                        }
                    }
                }
                if let logoError {
                    Text(logoError).font(.system(size: 12)).foregroundStyle(Color(hex: "#F87171"))
                }
                HStack(spacing: 8) {
                    TextField("Enter email or name", text: $inviteText)
                        .padding(10)
                        .background(bg)
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(border))
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                    Button("Add") {
                        let typed = inviteText.trimmingCharacters(in: .whitespacesAndNewlines)
                        if typed.isEmpty {
                            showAddPeople = true
                        } else {
                            members.append((String(typed.prefix(2)).uppercased(), typed, "Member", "All Locations", accent, false))
                            inviteText = ""
                        }
                    }
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(bg)
                    .padding(.horizontal, 14).padding(.vertical, 10)
                    .background(accent)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    .buttonStyle(.plain)
                }
                Text("Invite teammates after you activate")
                    .font(.system(size: 11)).foregroundStyle(dim).frame(maxWidth: .infinity)
            }
            section("02", "WHAT HAPPENS NEXT") {
                let next = launchNextSteps
                Text(next.intro)
                    .font(.system(size: 13, weight: .medium)).foregroundStyle(.white)
                ForEach(Array(next.steps.enumerated()), id: \.offset) { _, row in
                    nextRow(row.title, row.body, row.color)
                }
                if !next.steps.isEmpty {
                    Text("You can customize each moment later from Create.")
                        .font(.system(size: 12).italic()).foregroundStyle(dim).frame(maxWidth: .infinity)
                }
            }
            VStack(spacing: 6) {
                Text("4 sections configured • \(members.filter { !$0.you }.count) people to invite")
                    .font(.system(size: 13)).foregroundStyle(dim)
                HStack(spacing: 6) {
                    Text("✓").foregroundStyle(green)
                    Text("Ready to activate").font(.system(size: 12, weight: .semibold)).foregroundStyle(green)
                }
                .padding(.horizontal, 10).padding(.vertical, 6)
                .background(green.opacity(0.08))
                .overlay(Capsule().stroke(green.opacity(0.2)))
                .clipShape(Capsule())
            }
            .frame(maxWidth: .infinity)
            if let nameError, !nameError.isEmpty {
                Text(nameError)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(Color(hex: "#F87171"))
            }
            if let activateError, !activateError.isEmpty {
                Text(activateError)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(Color(hex: "#F87171"))
            }
            if let activateWarning, !activateWarning.isEmpty {
                Text(activateWarning)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(Color(hex: "#F59E0B"))
            }
            primaryButton("Activate \(companyName.isEmpty ? "Company" : companyName) →", color: green) {
                Task { await activate() }
            }
            Button("Close", action: onClose)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(dim)
                .frame(maxWidth: .infinity)
                .buttonStyle(.plain)
                .disabled(activating)
        }
    }

    private var launchNextSteps: (intro: String, steps: [(title: String, body: String, color: Color)]) {
        if !BusinessAudience.isSmallShop(audience) {
            return (
                "After activation, open Create to add Money, Daily Business, or Team moments",
                []
            )
        }
        let customMods: [String: Bool] = [
            "money": customMoney,
            "dailyBusiness": customDaily,
            "teamOps": customTeam,
        ]
        let kinds = IndustryTemplateCatalog.setupKinds(for: industryTemplate, customModules: customMods)
        if kinds.isEmpty {
            return ("Turn on at least one moment before activating", [])
        }
        let steps: [(title: String, body: String, color: Color)] = kinds.map { kind in
            switch kind {
            case .businessRunway:
                return ("Money & Cash Flow", "Revenue, Khata, invoices, and spend tracking", Color(hex: "#F59E0B"))
            case .businessOperations:
                return ("Daily Business", "Day-to-day ops, vendors, and routines", Color(hex: "#A78BFA"))
            case .teamOperations:
                return ("Team & Work", "Team rhythm, reviews, and collaboration", green)
            }
        }
        let intro = steps.count == 1
            ? "After activation, we'll set up:"
            : "After activation, we'll set up these moments:"
        return (intro, steps)
    }

    private func nextRow(_ title: String, _ body: String, _ color: Color) -> some View {
        HStack(spacing: 12) {
            Circle().fill(color.opacity(0.1)).frame(width: 28, height: 28)
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.system(size: 13, weight: .semibold)).foregroundStyle(.white)
                Text(body).font(.system(size: 11)).foregroundStyle(muted)
            }
        }
    }

    private func structureRow(_ title: String, _ body: String) -> some View {
        let selected = structure == title
        return Button {
            structure = title
        } label: {
            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).font(.system(size: 13, weight: selected ? .bold : .semibold)).foregroundStyle(.white)
                    Text(body).font(.system(size: 11)).foregroundStyle(muted)
                }
                Spacer()
                if selected { Circle().fill(accent).frame(width: 8, height: 8) }
            }
            .padding(12)
            .background(selected ? card : bg)
            .overlay(RoundedRectangle(cornerRadius: 10).stroke(selected ? accent : border, lineWidth: selected ? 1.5 : 1))
            .clipShape(RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(.plain)
    }

    private var fyRow: some View {
        HStack(spacing: 0) {
            ForEach(["Jan-Dec", "Apr-Mar", "Custom"], id: \.self) { opt in
                Button { fyCycle = opt } label: {
                    Text(opt)
                        .font(.system(size: 12, weight: fyCycle == opt ? .semibold : .medium))
                        .foregroundStyle(fyCycle == opt ? .white : muted)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .background(fyCycle == opt ? card : Color.clear)
                        .overlay(
                            RoundedRectangle(cornerRadius: 8)
                                .stroke(fyCycle == opt ? border : Color.clear)
                        )
                }
                .buttonStyle(.plain)
            }
        }
        .padding(3)
        .background(bg)
        .overlay(RoundedRectangle(cornerRadius: 10).stroke(border))
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }

    private func section(_ number: String, _ title: String, @ViewBuilder content: () -> some View) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 12) {
                Text(number).font(.system(size: 48, weight: .heavy)).foregroundStyle(accent.opacity(0.12))
                Text(title).font(.system(size: 14, weight: .bold)).foregroundStyle(accent)
            }
            content()
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(border))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func fieldLabel(_ text: String) -> some View {
        Text(text).font(.system(size: 12, weight: .semibold)).foregroundStyle(dim)
    }

    private func textField(_ binding: Binding<String>, placeholder: String = "") -> some View {
        TextField(placeholder, text: binding)
            .padding(12)
            .frame(height: 44)
            .background(bg)
            .overlay(RoundedRectangle(cornerRadius: 10).stroke(border))
            .clipShape(RoundedRectangle(cornerRadius: 10))
            .foregroundStyle(.white)
    }

    private func dropdown(_ value: String) -> some View {
        HStack {
            Text(value).foregroundStyle(.white)
            Spacer()
            Text("▼").foregroundStyle(dim)
        }
        .padding(12)
        .frame(height: 44)
        .background(bg)
        .overlay(RoundedRectangle(cornerRadius: 10).stroke(border))
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }

    private func pillRow(_ options: [String], selected: String, onSelect: @escaping (String) -> Void) -> some View {
        HStack(spacing: 6) {
            ForEach(options, id: \.self) { opt in
                Button { onSelect(opt) } label: {
                    Text(opt)
                        .font(.system(size: 12, weight: selected == opt ? .semibold : .medium))
                        .foregroundStyle(selected == opt ? bg : muted)
                        .padding(.horizontal, 12).padding(.vertical, 8)
                        .background(selected == opt ? accent : bg)
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(selected == opt ? Color.clear : border))
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                }
                .buttonStyle(.plain)
            }
        }
    }

    private func moduleToggle(_ label: String, isOn: Binding<Bool>) -> some View {
        Button {
            isOn.wrappedValue.toggle()
        } label: {
            HStack {
                Text(label)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(.white)
                Spacer()
                Text(isOn.wrappedValue ? "On" : "Off")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(isOn.wrappedValue ? green : dim)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(card)
            .clipShape(RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(.plain)
    }

    private func primaryButton(_ label: String, color: Color = Color(hex: "#818CF8"), action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(color == green ? .white : bg)
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .background(activating ? color.opacity(0.4) : color)
                .clipShape(RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
        .disabled(activating)
    }

    @MainActor
    private func activate() async {
        guard !activating else { return }
        let name = companyName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !name.isEmpty else {
            nameError = "Enter a company name to activate"
            go(to: 2)
            return
        }
        let smallShop = BusinessAudience.isSmallShop(audience)
        let customMods: [String: Bool] = [
            "money": customMoney,
            "dailyBusiness": customDaily,
            "teamOps": customTeam,
        ]
        if smallShop && industryTemplate.isCustom {
            let kinds = IndustryTemplateCatalog.setupKinds(for: industryTemplate, customModules: customMods)
            if kinds.isEmpty {
                activateError = "Turn on at least one moment (Money, Daily Business, or Team)"
                return
            }
        }
        nameError = nil
        activateError = nil
        activateWarning = nil
        activating = true
        defer { activating = false }
        let tz = timezone.contains("IST") ? "Asia/Kolkata" : "UTC"
        do {
            let template = smallShop ? industryTemplate : nil
            let modules: [String: Bool]
            if let template {
                modules = IndustryTemplateCatalog.modules(for: template, customModules: customMods)
            } else {
                modules = [
                    "money": true,
                    "dailyBusiness": true,
                    "teamOps": true,
                    "vendors": true,
                ]
            }
            var profile: [String: Any] = [
                "industry": industry,
                "companySize": companySize,
                "currency": currency,
                "financialYear": fyCycle,
                "structure": structure,
                "audience": audience,
                "settings": ["modules": modules] as [String: Any],
            ]
            if let template {
                profile[IndustryTemplateCatalog.profileKey] = template.id
                for (k, v) in template.profileDefaults where profile[k] == nil {
                    profile[k] = v
                }
            }
            let created = try await APIClient.shared.createCompany(
                displayName: name,
                legalName: name,
                timezone: tz,
                companyType: entityType,
                taxIdentifier: gstin.isEmpty ? nil : gstin,
                profileJson: profile
            )
            BusinessAudience.saveForCompany(companyId: created.companyId, audience: audience)
            CompanyModules.saveModules(companyId: created.companyId, modules: modules)
            if let hint = IndustryTemplateCatalog.hubHint(from: profile) {
                IndustryTemplateCatalog.saveHubHint(companyId: created.companyId, hint: hint)
            }
            for loc in locations {
                _ = try? await APIClient.shared.createLocation(
                    companyId: created.companyId,
                    name: loc.name,
                    addressText: loc.area,
                    timezone: tz
                )
            }
            var softWarnings: [String] = []
            if let logoData {
                do {
                    let mediaId = try await APIClient.shared.uploadCompanyLogo(companyId: created.companyId, bytes: logoData)
                    _ = try await APIClient.shared.patchCompany(
                        companyId: created.companyId,
                        body: [
                            "expectedVersion": created.version,
                            "profileJson": ["logoMediaId": mediaId],
                        ]
                    )
                } catch {
                    softWarnings.append("Logo could not be saved")
                    logoError = error.localizedDescription
                }
            }
            if let template {
                let kinds = IndustryTemplateCatalog.setupKinds(for: template, customModules: customMods)
                let repo = MomentCreateRepository()
                var failedTitles: [String] = []
                for kind in kinds {
                    let entry = BusinessSetupCatalog.forKind(kind)
                    var prefs = BusinessSetupCatalog.defaultPreferences(kind: kind, audience: audience)
                    prefs.removeValue(forKey: IndustryTemplateCatalog.profileKey)
                    let allowed = Set(BusinessSetupCatalog.forKind(kind).defaultPreferences.keys)
                    prefs = prefs.filter { allowed.contains($0.key) }
                    do {
                        _ = try await repo.createBusinessSetup(
                            draftKey: "template-\(created.companyId)-\(kind.rawValue)",
                            companyId: created.companyId,
                            familyCode: kind.familyCode,
                            title: entry.defaultTitle,
                            description: nil,
                            momentTypeCode: entry.momentTypeCode,
                            preferences: prefs,
                            status: "ACTIVE"
                        )
                    } catch {
                        failedTitles.append(entry.defaultTitle)
                    }
                }
                if !failedTitles.isEmpty {
                    softWarnings.append(
                        "Company created; \(failedTitles.joined(separator: ", ")) could not be set up—open Create to add"
                    )
                }
            }
            if !softWarnings.isEmpty {
                activateWarning = softWarnings.joined(separator: " · ")
            }
            let summary = CompanySummary(companyId: created.companyId, displayName: created.displayName)
            if let invite = try? await APIClient.shared.mintCompanyInvite(
                companyId: created.companyId,
                membershipType: "MEMBER",
                idempotencyKey: UUID().uuidString
            ) {
                let path = invite.invitePath.isEmpty
                    ? CompanyJoinLink.displayPath(code: invite.inviteCode)
                    : invite.invitePath
                pendingInviteMessage = InviteOutboundShare.inviteMessage(title: created.displayName, url: path)
                pendingInvitePhone = members.reversed().map(\.name).first { InviteOutboundShare.looksLikePhone($0) }
                    ?? (InviteOutboundShare.looksLikePhone(inviteText) ? inviteText : nil)
                pendingActivation = summary
                showPostInviteChooser = true
            } else {
                onActivated(summary)
            }
        } catch {
            activateError = error.localizedDescription.isEmpty
                ? "Could not activate company. Check your connection and try again."
                : error.localizedDescription
        }
    }

    private var locationEditorSheet: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 12) {
                fieldLabel("NAME")
                textField($locationNameDraft, placeholder: "Location name")
                fieldLabel("ADDRESS")
                textField($locationAreaDraft, placeholder: "Address")
                primaryButton("Save") { saveLocationDraft() }
                    .disabled(locationNameDraft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                if editingLocationIndex != nil {
                    Button("Remove location", role: .destructive) {
                        removeEditingLocation()
                    }
                    .frame(maxWidth: .infinity)
                }
                Spacer()
            }
            .padding(20)
            .background(bg.ignoresSafeArea())
            .navigationTitle(editingLocationIndex == nil ? "Add location" : "Edit location")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { showLocationEditor = false }
                }
            }
        }
        .presentationDetents([.medium])
    }

    private var memberEditorSheet: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 12) {
                fieldLabel("NAME")
                textField($memberNameDraft, placeholder: "Name or email")
                primaryButton("Save") { saveMemberDraft() }
                    .disabled(memberNameDraft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                Spacer()
            }
            .padding(20)
            .background(bg.ignoresSafeArea())
            .navigationTitle("Edit member")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { editingMemberIndex = nil }
                }
            }
        }
        .presentationDetents([.medium])
    }

    private func saveLocationDraft() {
        let name = locationNameDraft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !name.isEmpty else { return }
        let area = locationAreaDraft.trimmingCharacters(in: .whitespacesAndNewlines)
        if let index = editingLocationIndex, locations.indices.contains(index) {
            let current = locations[index]
            locations[index] = (name, area, current.primary, current.color)
        } else {
            let primary = locations.isEmpty
            locations.append((name, area, primary, primary ? green : Color(hex: "#F59E0B")))
        }
        showLocationEditor = false
    }

    private func removeEditingLocation() {
        guard let index = editingLocationIndex, locations.indices.contains(index) else {
            showLocationEditor = false
            return
        }
        let wasPrimary = locations[index].primary
        locations.remove(at: index)
        if wasPrimary, !locations.isEmpty, !locations.contains(where: \.primary) {
            let first = locations[0]
            locations[0] = (first.name, first.area, true, green)
        }
        showLocationEditor = false
    }

    private func saveMemberDraft() {
        let name = memberNameDraft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let index = editingMemberIndex, members.indices.contains(index), !name.isEmpty else {
            editingMemberIndex = nil
            return
        }
        let current = members[index]
        guard !current.you else {
            editingMemberIndex = nil
            return
        }
        members[index] = (String(name.prefix(2)).uppercased(), name, current.role, current.scope, current.color, false)
        editingMemberIndex = nil
    }
}

/// Figma 702:9524 — Company Settings (read-mostly v1).
struct CompanySettingsView: View {
    let companyName: String
    var onBack: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Button(action: onBack) {
                    Text("← Back").font(.system(size: 14, weight: .semibold)).foregroundStyle(Color(hex: "#818CF8"))
                }
                .buttonStyle(.plain)
                Text("Company Settings").font(.system(size: 28, weight: .bold)).foregroundStyle(.white)
                Text("Manage your business profile and preferences after setup.")
                    .font(.system(size: 14)).foregroundStyle(Color(hex: "#94A3B8"))
                settingsCard("COMPANY PROFILE") {
                    HStack(spacing: 12) {
                        Text(String(companyName.prefix(1)))
                            .font(.system(size: 20, weight: .bold))
                            .foregroundStyle(Color(hex: "#0C0F15"))
                            .frame(width: 48, height: 48)
                            .background(Color(hex: "#818CF8"))
                            .clipShape(Circle())
                        VStack(alignment: .leading, spacing: 4) {
                            Text(companyName).font(.system(size: 16, weight: .bold)).foregroundStyle(.white)
                            Text("Open company settings after setup to edit your profile.")
                                .font(.system(size: 13)).foregroundStyle(Color(hex: "#94A3B8"))
                        }
                    }
                }
                settingsCard("LOCATIONS") {
                    Text("Manage locations from Company Setup after activation.")
                        .font(.system(size: 13)).foregroundStyle(Color(hex: "#94A3B8"))
                }
                settingsCard("ACTIVE MODULES") {
                    moduleRow("Team & Work", Color(hex: "#10B981"))
                    moduleRow("Money & Cash Flow", Color(hex: "#F59E0B"))
                    moduleRow("Daily Business", Color(hex: "#818CF8"))
                }
            }
            .padding(20)
        }
        .background(Color(hex: "#0C0F15").ignoresSafeArea())
    }

    private func settingsCard(_ title: String, @ViewBuilder content: () -> some View) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title).font(.system(size: 12, weight: .semibold)).foregroundStyle(Color(hex: "#64748B"))
            content()
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "#161B26"))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color(hex: "#1E293B")))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func moduleRow(_ title: String, _ color: Color) -> some View {
        HStack {
            Circle().fill(color).frame(width: 8, height: 8)
            Text(title).foregroundStyle(.white)
            Spacer()
            Text("Active").foregroundStyle(Color(hex: "#10B981")).font(.system(size: 12, weight: .semibold))
        }
    }
}
