// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "MomentraAPI",
    platforms: [.iOS(.v16), .macOS(.v13)],
    products: [
        .library(name: "MomentraAPI", targets: ["MomentraAPI"]),
    ],
    dependencies: [
        .package(url: "https://github.com/Flight-School/AnyCodable.git", from: "0.6.7"),
    ],
    targets: [
        .target(
            name: "MomentraAPI",
            dependencies: [
                .product(name: "AnyCodable", package: "AnyCodable"),
            ],
            path: "Generated/MomentraAPI/Classes/OpenAPIs"
        ),
    ]
)
