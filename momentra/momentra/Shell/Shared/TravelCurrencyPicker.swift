import SwiftUI

struct TravelCurrencyPicker: View {
    @Binding var selectedCode: String
    let preferredCodes: [String]
    var label: String = "Currency"
    var showLabel: Bool = true
    var textColor: Color = Color(hex: "#E5E0EE")
    var secondaryColor: Color = Color(hex: "#C9C4D8")
    var accentColor: Color = Color(hex: "#14B8A6")

    private var options: [String] {
        MomentCurrencyResolver.pickerOptions(preferred: preferredCodes)
    }

    var body: some View {
        Menu {
            ForEach(options, id: \.self) { code in
                Button(TravelCurrencyCatalog.display(code)) { selectedCode = code }
            }
        } label: {
            HStack {
                if showLabel {
                    Text(label)
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(secondaryColor)
                    Spacer()
                }
                HStack(spacing: 6) {
                    Text(TravelCurrencyCatalog.symbol(selectedCode))
                        .font(.system(size: showLabel ? 14 : 28, weight: .bold))
                        .foregroundStyle(accentColor)
                    Text(selectedCode)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(textColor)
                    Image(systemName: "chevron.down")
                        .font(.system(size: 12))
                        .foregroundStyle(secondaryColor)
                }
            }
            .padding(12)
            .background(Color(hex: "#201E28"))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(hex: "#938EA1"), lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }
}
