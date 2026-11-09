//
//  MomentScreen.swift
//  momentra
//
//  Created by Momentra Team
//  Copyright © 2024 Momentra. All rights reserved.
//

import SwiftUI

/// Moment screen with emerald topbar and trading/distribution options.
///
/// This screen displays trading and distribution setup options with the
/// unified emerald color palette (#10B981) from the business tokens.
///
/// Figma: node 2943-9075 in file TzLvwVwlPbeVB8ug1zB3GM
struct MomentScreen: View {
    // MARK: - Properties

    @EnvironmentObject private var appShellModel: AppShellModel
    @State private var selectedSetupKind: BusinessSetupKind?

    /// The emerald accent color from business tokens
    private let emeraldAccent = Color(hex: 0x10B981)

    // MARK: - Body

    var body: some View {
        VStack(spacing: 0) {
            // MARK: - Topbar with Emerald Accent
            HStack {
                Text("Moments")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(.white)
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(emeraldAccent)
            .frame(height: 56)

            // MARK: - Main Content
            ScrollView {
                VStack(spacing: 24) {
                    // Trading Option
                    TradingDistributionOptionView(
                        icon: "trading_icon",
                        title: "Trading",
                        subtitle: "Wholesale pricing, inventory, and sales",
                        accent: emeraldAccent,
                        onTap: {
                            selectAndSetup(.trading)
                        }
                    )

                    // Distribution Option
                    TradingDistributionOptionView(
                        icon: "distribution_icon",
                        title: "Distribution",
                        subtitle: "Dealership, shipments, and network",
                        accent: emeraldAccent,
                        onTap: {
                            selectAndSetup(.distribution)
                        }
                    )
                }
                .padding(.horizontal, 20)
                .padding(.top, 30)
                .padding(.bottom, 40)
            }
            .background(Color.black)
        }
        .background(Color.black)
        .edgesIgnoringSafeArea(.all)
        .sheet(item: $selectedSetupKind) { kind in
            BusinessSetupWizardView(
                kind: kind,
                createModel: MomentCreateModel(),
                companyId: "",
                ownerName: "",
                onClose: { dismissSetup() },
                onCreated: { outcome in
                    dismissSetup()
                    handleSetupCompletion(outcome)
                }
            )
            .presentationDetents([.fraction(0.94)])
            .presentationCornerRadius(24)
            .presentationBackground(Color(hex: 0x0F172A))
            .presentationDragIndicator(.visible)
        }
    }

    // MARK: - Actions

    private func selectAndSetup(_ kind: BusinessSetupKind) {
        selectedSetupKind = kind
    }

    private func dismissSetup() {
        selectedSetupKind = nil
    }

    /// Called only on success. `CreateMomentOutcome` is the created moment's
    /// authoritative record — `MomentCreateModel.state.error` owns failures.
    private func handleSetupCompletion(_ outcome: CreateMomentOutcome) {
        appShellModel.selectedMomentId = outcome.momentId
    }
}

// MARK: - Trading/Distribution Option View

/// A reusable view for trading/distribution setup options.
struct TradingDistributionOptionView: View {
    let icon: String
    let title: String
    let subtitle: String
    let accent: Color
    let onTap: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                Image(icon)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 24, height: 24)
                    .foregroundStyle(accent)

                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.system(size: 18, weight: .bold))
                        .foregroundStyle(.white)
                    Text(subtitle)
                        .font(.system(size: 14, weight: .regular))
                        .foregroundStyle(Color(hex: 0x94A3B8))
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.system(size: 20, weight: .medium))
                    .foregroundStyle(Color(hex: 0x64748B))
            }
        }
        .padding(20)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(hex: 0x151D2E))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(accent.opacity(0.3), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .onTapGesture(perform: onTap)
    }
}

// MARK: - Preview

struct MomentScreen_Previews: PreviewProvider {
    static var previews: some View {
        MomentScreen()
            .environmentObject(AppShellModel())
    }
}
