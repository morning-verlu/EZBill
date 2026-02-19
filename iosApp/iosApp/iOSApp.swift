import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
        .onChange(of: scenePhase) { phase in
            switch phase {
            case .active:
                AppSessionStore.shared.onAppForeground()
            case .background:
                AppSessionStore.shared.onAppBackground()
            default:
                break
            }
        }
    }
}
