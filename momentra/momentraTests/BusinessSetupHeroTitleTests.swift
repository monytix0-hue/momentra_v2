import Foundation
import Testing
@testable import momentra

struct BusinessSetupHeroTitleTests {

    @Test func emptyNameUsesPlaceholder() {
        #expect(BusinessSetupHeroTitle.format(name: nil, placeholder: "Brightline Setup") == "Brightline Setup")
        #expect(BusinessSetupHeroTitle.format(name: "  ", placeholder: "Brightline Setup") == "Brightline Setup")
    }

    @Test func typedNameAppendsSetup() {
        #expect(
            BusinessSetupHeroTitle.format(name: "Brightline Creative", placeholder: "Brightline Setup")
                == "Brightline Creative Setup"
        )
    }

    @Test func nameAlreadyEndingInSetupIsNotDoubled() {
        #expect(
            BusinessSetupHeroTitle.format(name: "Brightline Setup", placeholder: "Harbor & Loom Setup")
                == "Brightline Setup"
        )
        #expect(
            BusinessSetupHeroTitle.format(name: "brightline setup", placeholder: "Harbor & Loom Setup")
                == "Brightline Setup"
        )
    }
}
