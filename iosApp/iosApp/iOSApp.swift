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
                AppGraph.shared.dispatchAppForeground()
            case .background:
                AppGraph.shared.dispatchAppBackground()
            default:
                break
            }
        }
    }
}
