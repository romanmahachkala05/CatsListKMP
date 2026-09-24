import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        // Koin and Coil, before anything asks for them.
        StartCatsAppKt.startCatsApp()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
