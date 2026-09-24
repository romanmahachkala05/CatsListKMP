import SwiftUI
import Shared

/// The whole UI is Compose; SwiftUI only hosts it, edge to edge. Compose reads the safe area
/// itself, through the same window insets the Android app uses.
struct ContentView: View {
    var body: some View {
        ComposeView().ignoresSafeArea()
    }
}

private struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
