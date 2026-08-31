import SwiftUI

/// Figma 695:4455 — Company Setup wizard (692:38403 / 38453 / 38549 / 38635).
struct CompanySetupFlowView: View {
    var onClose: () -> Void
    var onActivated: (CompanySummary) -> Void

    @State private var step = 1
    @State private var stepForward = true
    @State private var welcomeAppeared = false
    @State private var companyName = "Pureborn Ops"
    @State private var industry = "Technology & Software"
    @State private var companySize = "Small (2-25)"
    @State private var entityType = "Pvt Ltd"
    @State private var gstin = ""
    @State private var currency = "₹ INR — Indian Rupee"
    @State private var fyCycle = "Apr-Mar"
    @State private var timezone = "IST (UTC+5:30)"
    @State private var structure = "Multi-Location"
    @State private var locations: [(name: String, area: String, primary: Bool, color: Color)] = [
        ("HQ — Mumbai", "Andheri East", true, Color(hex: "#10B981")),
        ("Branch — Bangalore", "Koramangala", false, Color(hex: "#F59E0B")),
        ("Branch — Delhi", "Connaught Place", false, Color(hex: "#EF4444")),
    ]
    @State private var members: [(initials: String, name: String, role: String, scope: String, color: Color, you: Bool)] = [
        ("SM", "Sahil M.", "Owner", "All Locations", Color(hex: "#818CF8"), true),
        ("AR", "Ananya R.", "Admin", "Bangalore Branch", Color(hex: "#F59E0B"), false),
    ]
    @State private var inviteText = ""
    @State private var activating = false
    @State private var showJoinCode = false

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
                Text("Get your company running on momentra in just a few steps.")
                    .font(.system(size: 14))
                    .foregroundStyle(muted)
                    .multilineTextAlignment(.center)
            }
            .opacity(welcomeAppeared ? 1 : 0)
            .offset(y: welcomeAppeared ? 0 : 16)
            benefit("2-minute setup", "Quick guided configuration", accent.opacity(0.1))
                .opacity(welcomeAppeared ? 1 : 0)
                .offset(y: welcomeAppeared ? 0 : 20)
            benefit("Multi-location ready", "Support for branches & units", green.opacity(0.1))
                .opacity(welcomeAppeared ? 1 : 0)
                .offset(y: welcomeAppeared ? 0 : 20)
            benefit("Edit anytime", "All settings adjustable later", Color(hex: "#F59E0B").opacity(0.1))
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
                fieldLabel("COMPANY NAME")
                textField($companyName)
                fieldLabel("INDUSTRY")
                dropdown(industry)
                fieldLabel("COMPANY SIZE")
                pillRow(["Solo (1)", "Small (2-25)", "Medium (26-100)"], selected: companySize) { companySize = $0 }
                fieldLabel("COMPANY LOGO")
                Text("Upload corporate logo")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(muted)
                    .frame(maxWidth: .infinity)
                    .frame(height: 64)
                    .overlay(RoundedRectangle(cornerRadius: 10).stroke(border, style: StrokeStyle(lineWidth: 1, dash: [6])))
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
                structureRow("Multi-Location", "Multiple branches or offices")
                structureRow("Multi-Unit", "Different business units or brands")
            }
            section("02", "YOUR LOCATIONS") {
                ForEach(Array(locations.enumerated()), id: \.offset) { _, loc in
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
                        Text("✎").foregroundStyle(dim).padding(.trailing, 12)
                    }
                    .background(bg)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                }
                Text("+ Add another location")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(accent)
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
                Text("Add team members to get started")
                    .font(.system(size: 14, weight: .medium)).foregroundStyle(.white)
                ForEach(Array(members.enumerated()), id: \.offset) { _, m in
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
                        Text(m.you ? "You" : "✎").font(.system(size: 12)).foregroundStyle(dim)
                    }
                }
                HStack(spacing: 8) {
                    TextField("Enter email or name", text: $inviteText)
                        .padding(10)
                        .background(bg)
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(border))
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                    Button("Add") {
                        let t = inviteText.trimmingCharacters(in: .whitespaces)
                        guard !t.isEmpty else { return }
                        members.append((String(t.prefix(2)).uppercased(), t, "Member", "All Locations", accent, false))
                        inviteText = ""
                    }
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(bg)
                    .padding(.horizontal, 14).padding(.vertical, 10)
                    .background(accent)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    .buttonStyle(.plain)
                }
                Text("3 free members included · Upgrade for more")
                    .font(.system(size: 11)).foregroundStyle(dim).frame(maxWidth: .infinity)
            }
            section("02", "WHAT HAPPENS NEXT") {
                Text("After activation, three module wizards will guide you:")
                    .font(.system(size: 13, weight: .medium)).foregroundStyle(.white)
                nextRow("Team Operations", "Set review cycles, monitoring style, team pods", green)
                nextRow("Business Runway", "Configure financials, cash tracking, burn alerts", Color(hex: "#F59E0B"))
                nextRow("Business Operations", "Define budgets, approval workflows, vendors", Color(hex: "#A78BFA"))
                Text("Each takes about 1 minute to configure.")
                    .font(.system(size: 12).italic()).foregroundStyle(dim).frame(maxWidth: .infinity)
            }
            VStack(spacing: 6) {
                Text("4 sections configured • \(members.count) team members added")
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
            primaryButton("Activate \(companyName.isEmpty ? "Company" : companyName) →", color: green) {
                Task { await activate() }
            }
            Button("Save as draft", action: onClose)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(dim)
                .frame(maxWidth: .infinity)
                .buttonStyle(.plain)
                .disabled(activating)
        }
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
        activating = true
        defer { activating = false }
        let tz = timezone.contains("IST") ? "Asia/Kolkata" : "UTC"
        let name = companyName.isEmpty ? "My Company" : companyName
        do {
            let created = try await APIClient.shared.createCompany(
                displayName: name,
                legalName: name,
                timezone: tz,
                companyType: entityType,
                taxIdentifier: gstin.isEmpty ? nil : gstin,
                profileJson: [
                    "industry": industry,
                    "companySize": companySize,
                    "currency": currency,
                    "financialYear": fyCycle,
                    "structure": structure,
                ]
            )
            for loc in locations {
                _ = try? await APIClient.shared.createLocation(
                    companyId: created.companyId,
                    name: loc.name,
                    addressText: loc.area,
                    timezone: tz
                )
            }
            onActivated(CompanySummary(companyId: created.companyId, displayName: created.displayName))
        } catch {
            onClose()
        }
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
                            Text("Technology • Mumbai, India").font(.system(size: 13)).foregroundStyle(Color(hex: "#94A3B8"))
                        }
                    }
                }
                settingsCard("LOCATIONS") {
                    Text("Manage locations from Company Setup after activation.")
                        .font(.system(size: 13)).foregroundStyle(Color(hex: "#94A3B8"))
                }
                settingsCard("ACTIVE MODULES") {
                    moduleRow("Team Operations", Color(hex: "#10B981"))
                    moduleRow("Business Runway", Color(hex: "#F59E0B"))
                    moduleRow("Business Operations", Color(hex: "#818CF8"))
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
