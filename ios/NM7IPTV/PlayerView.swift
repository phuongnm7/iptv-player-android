import SwiftUI
import AVFoundation
import MediaPlayer
import UIKit
import Combine

@MainActor
final class PlayerStore: ObservableObject {
    let player = AVPlayer()
    @Published var currentChannel: IPTVChannel?
    @Published var isPlaying = false
    @Published var statusText = ""
    @Published var errorMessage: String?
    @Published var controlsVisible = false

    private var timeObservation: NSKeyValueObservation?
    private var failedObserver: NSObjectProtocol?
    weak var volumeSlider: UISlider?

    init() {
        player.automaticallyWaitsToMinimizeStalling = true
        timeObservation = player.observe(\.timeControlStatus, options: [.initial, .new]) { [weak self] player, _ in
            DispatchQueue.main.async {
                guard let self else { return }
                self.isPlaying = player.timeControlStatus == .playing
                if player.timeControlStatus == .waitingToPlayAtSpecifiedRate { self.statusText = "Đang tải luồng…" }
                if player.timeControlStatus == .playing { self.statusText = "Đang phát" }
            }
        }
        failedObserver = NotificationCenter.default.addObserver(
            forName: .AVPlayerItemFailedToPlayToEndTime, object: nil, queue: .main
        ) { [weak self] notification in
            let error = notification.userInfo?[AVPlayerItemFailedToPlayToEndTimeErrorKey] as? Error
            self?.errorMessage = error?.localizedDescription ?? "Không phát được nguồn này"
        }
    }

    deinit {
        timeObservation?.invalidate()
        if let failedObserver { NotificationCenter.default.removeObserver(failedObserver) }
    }

    func play(_ channel: IPTVChannel) {
        errorMessage = nil
        statusText = "Đang kết nối…"
        currentChannel = channel
        controlsVisible = false

        if let drm = channel.drmKind {
            player.pause()
            player.replaceCurrentItem(with: nil)
            errorMessage = "Kênh này dùng \(drm). iPhone/iPad cần FairPlay hoặc SDK DRM dành riêng cho iOS; không thể dùng trực tiếp cấu hình DRM Android."
            return
        }

        let lowerURL = channel.url.lowercased()
        if channel.mimeHint.lowercased().contains("dash") || lowerURL.contains(".mpd") {
            player.pause()
            player.replaceCurrentItem(with: nil)
            errorMessage = "Nguồn DASH/MPD chưa được AVPlayer của iOS hỗ trợ trực tiếp. HLS/M3U8 và MP4 được ưu tiên trong bản iOS hiện tại."
            return
        }

        guard let url = URL(string: channel.url), ["http", "https"].contains(url.scheme?.lowercased() ?? "") else {
            errorMessage = "Giao thức của kênh này chưa được bản iOS hỗ trợ."
            return
        }

        do {
            let audio = AVAudioSession.sharedInstance()
            try audio.setCategory(.playback, mode: .moviePlayback, options: [])
            try audio.setActive(true)
        } catch { }

        var options: [String: Any] = [:]
        if !channel.headers.isEmpty {
            // AVFoundation accepts request headers through AVURLAsset options.
            options["AVURLAssetHTTPHeaderFieldsKey"] = channel.headers
        }
        let asset = AVURLAsset(url: url, options: options)
        let item = AVPlayerItem(asset: asset)
        player.replaceCurrentItem(with: item)
        player.play()
    }

    func togglePlayback() {
        if player.timeControlStatus == .playing { player.pause() } else { player.play() }
    }

    func stop() {
        player.pause()
        player.replaceCurrentItem(with: nil)
        currentChannel = nil
        errorMessage = nil
        statusText = ""
        controlsVisible = false
    }

    var systemVolume: Float {
        volumeSlider?.value ?? AVAudioSession.sharedInstance().outputVolume
    }

    func setSystemVolume(_ value: Float) {
        let value = min(1, max(0, value))
        volumeSlider?.setValue(value, animated: false)
        volumeSlider?.sendActions(for: .valueChanged)
    }
}

struct PlayerSurface: UIViewRepresentable {
    let player: AVPlayer

    func makeUIView(context: Context) -> PlayerLayerView {
        let view = PlayerLayerView()
        view.playerLayer.videoGravity = .resizeAspect
        view.playerLayer.player = player
        return view
    }

    func updateUIView(_ uiView: PlayerLayerView, context: Context) {
        uiView.playerLayer.player = player
    }
}

final class PlayerLayerView: UIView {
    override static var layerClass: AnyClass { AVPlayerLayer.self }
    var playerLayer: AVPlayerLayer { layer as! AVPlayerLayer }
}

struct SystemVolumeBridge: UIViewRepresentable {
    @ObservedObject var store: PlayerStore

    func makeUIView(context: Context) -> MPVolumeView {
        let view = MPVolumeView(frame: .zero)
        DispatchQueue.main.async { store.volumeSlider = view.subviews.compactMap { $0 as? UISlider }.first }
        return view
    }

    func updateUIView(_ uiView: MPVolumeView, context: Context) {
        if store.volumeSlider == nil {
            DispatchQueue.main.async { store.volumeSlider = uiView.subviews.compactMap { $0 as? UISlider }.first }
        }
    }
}

struct NM7PlayerPane: View {
    @ObservedObject var playerStore: PlayerStore
    let fullscreen: () -> Void

    @State private var gestureSide = 0
    @State private var startBrightness: CGFloat = 0.5
    @State private var startVolume: Float = 0.5
    @State private var feedback: String?

    var body: some View {
        GeometryReader { geometry in
            ZStack {
                Color.black
                PlayerSurface(player: playerStore.player)
                SystemVolumeBridge(store: playerStore)
                    .frame(width: 1, height: 1)
                    .opacity(0.001)

                if playerStore.controlsVisible {
                    ZStack {
                        Color.black.opacity(0.22)
                        HStack(spacing: 34) {
                            Button(action: playerStore.togglePlayback) {
                                Image(systemName: playerStore.isPlaying ? "pause.fill" : "play.fill")
                                    .font(.system(size: 27, weight: .bold))
                                    .frame(width: 58, height: 58)
                                    .background(.ultraThinMaterial, in: Circle())
                            }
                            Button(action: fullscreen) {
                                Image(systemName: "arrow.up.left.and.arrow.down.right")
                                    .font(.system(size: 21, weight: .semibold))
                                    .frame(width: 50, height: 50)
                                    .background(.ultraThinMaterial, in: Circle())
                            }
                        }
                        .buttonStyle(.plain)
                        .foregroundStyle(.white)
                    }
                }

                if let feedback {
                    Text(feedback)
                        .font(.headline)
                        .padding(.horizontal, 18)
                        .padding(.vertical, 10)
                        .background(.ultraThinMaterial, in: Capsule())
                        .foregroundStyle(.white)
                }

                if let error = playerStore.errorMessage {
                    VStack(spacing: 10) {
                        Image(systemName: "exclamationmark.triangle.fill")
                        Text(error).multilineTextAlignment(.center)
                    }
                    .font(.subheadline)
                    .padding(18)
                    .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16))
                    .foregroundStyle(.white)
                    .padding(24)
                }
            }
            .contentShape(Rectangle())
            .onTapGesture { playerStore.controlsVisible.toggle() }
            .gesture(edgeGesture(size: geometry.size))
        }
        .aspectRatio(16 / 9, contentMode: .fit)
        .background(Color.black)
    }

    private func edgeGesture(size: CGSize) -> some Gesture {
        DragGesture(minimumDistance: 12)
            .onChanged { value in
                if gestureSide == 0 {
                    if value.startLocation.x < size.width * 0.30 {
                        gestureSide = 1
                        startBrightness = UIScreen.main.brightness
                    } else if value.startLocation.x > size.width * 0.70 {
                        gestureSide = 2
                        startVolume = playerStore.systemVolume
                    } else {
                        gestureSide = -1
                    }
                }
                guard gestureSide > 0 else { return }
                let change = Float(-value.translation.height / max(1, size.height)) * 1.25
                if gestureSide == 1 {
                    let next = min(1, max(0.05, startBrightness + CGFloat(change)))
                    UIScreen.main.brightness = next
                    feedback = "☀︎  Độ sáng \(Int(next * 100))%"
                } else {
                    let next = min(1, max(0, startVolume + change))
                    playerStore.setSystemVolume(next)
                    feedback = "🔊  Âm lượng \(Int(next * 100))%"
                }
            }
            .onEnded { _ in
                gestureSide = 0
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.55) { feedback = nil }
            }
    }
}

struct FullscreenPlayerView: View {
    @ObservedObject var playerStore: PlayerStore
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ZStack(alignment: .topLeading) {
            Color.black.ignoresSafeArea()
            NM7PlayerPane(playerStore: playerStore, fullscreen: { dismiss() })
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            Button(action: { dismiss() }) {
                Image(systemName: "xmark")
                    .font(.headline)
                    .frame(width: 44, height: 44)
                    .background(.ultraThinMaterial, in: Circle())
            }
            .foregroundStyle(.white)
            .padding()
        }
        .statusBarHidden()
    }
}
