import SwiftUI
import ComposeApp

/// Hosts the shared Compose UI (exported from the Kotlin `ComposeApp` framework).
struct ContentView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
