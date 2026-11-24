import Testing
@testable import momentra

struct TradingChromeIconsTests {
    @Test
    func pulseActionsMapToRestTileAssets() {
        #expect(TradingChromeIcons.pulseActionAsset("SALE") == "ic_rest_tile_sale")
        #expect(TradingChromeIcons.pulseActionAsset("purchase") == "ic_rest_tile_purchase")
        #expect(TradingChromeIcons.pulseActionAsset("COLLECTION") == "ic_rest_tile_collection")
        #expect(TradingChromeIcons.pulseActionAsset("PAYMENT") == "ic_rest_tile_payment")
        #expect(TradingChromeIcons.pulseActionCodes.count == 4)
    }

    @Test
    func tradingHubCoreAndSupportHaveAssets() {
        for id in TradingChromeIcons.tradingHubCoreIds {
            #expect(!TradingChromeIcons.hubAsset(id).isEmpty)
        }
        for id in TradingChromeIcons.tradingHubSupportIds {
            #expect(!TradingChromeIcons.hubAsset(id).isEmpty)
        }
        #expect(TradingChromeIcons.hubAsset("PURCHASE") == "biz_qa_wallet")
        #expect(TradingChromeIcons.hubAsset("SEARCH") == "biz_qa_search")
    }
}
