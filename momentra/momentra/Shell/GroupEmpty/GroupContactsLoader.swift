import Contacts
import Foundation

/// Contact I/O lives outside SwiftUI MainActor types so CNContactStore.enumerateContacts
/// never runs on the UI thread (runtime warning 2759).
enum GroupContactsLoader {
    struct Row: Sendable {
        let id: String
        let name: String
        let subtitle: String
        let photoData: Data?
        let email: String
        let phone: String
    }

    enum LoaderError: Error {
        case permissionDenied
    }

    /// Requests access (if needed) and loads contacts entirely off the main thread.
    static func fetchRowsIfAuthorized() async throws -> [Row] {
        let granted = try await requestAccess()
        guard granted else { throw LoaderError.permissionDenied }
        return try await enumerateOffMain()
    }

    private static func requestAccess() async throws -> Bool {
        try await withCheckedThrowingContinuation { continuation in
            DispatchQueue.global(qos: .userInitiated).async {
                CNContactStore().requestAccess(for: .contacts) { granted, error in
                    if let error {
                        continuation.resume(throwing: error)
                    } else {
                        continuation.resume(returning: granted)
                    }
                }
            }
        }
    }

    private static func enumerateOffMain() async throws -> [Row] {
        try await withCheckedThrowingContinuation { continuation in
            // requestAccess completion can land on the main queue — always hop again.
            DispatchQueue.global(qos: .userInitiated).async {
                do {
                    continuation.resume(returning: try loadSync())
                } catch {
                    continuation.resume(throwing: error)
                }
            }
        }
    }

    private static func loadSync() throws -> [Row] {
        precondition(!Thread.isMainThread, "CNContactStore.enumerateContacts must not run on main")
        let store = CNContactStore()
        let keys: [CNKeyDescriptor] = [
            CNContactGivenNameKey as CNKeyDescriptor,
            CNContactFamilyNameKey as CNKeyDescriptor,
            CNContactPhoneNumbersKey as CNKeyDescriptor,
            CNContactEmailAddressesKey as CNKeyDescriptor,
            CNContactThumbnailImageDataKey as CNKeyDescriptor,
        ]
        let request = CNContactFetchRequest(keysToFetch: keys)
        request.sortOrder = .givenName
        var loaded: [Row] = []
        try store.enumerateContacts(with: request) { contact, _ in
            let name = [contact.givenName, contact.familyName]
                .map { $0.trimmingCharacters(in: .whitespaces) }
                .filter { !$0.isEmpty }
                .joined(separator: " ")
            guard !name.isEmpty else { return }
            let phone = contact.phoneNumbers.first?.value.stringValue ?? ""
            let email = contact.emailAddresses.first.map { $0.value as String } ?? ""
            let subtitle = phone.isEmpty ? email : phone
            loaded.append(
                Row(
                    id: contact.identifier,
                    name: name,
                    subtitle: subtitle,
                    photoData: contact.thumbnailImageData,
                    email: email,
                    phone: phone
                )
            )
        }
        return loaded
    }
}
