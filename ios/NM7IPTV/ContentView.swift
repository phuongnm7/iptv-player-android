import SwiftUI

struct ContentView: View {
    @ObservedObject var store: AppStore
    @ObservedObject var playerStore: PlayerStore

    @State private var showSources = false
    @State private var showFullscreen = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                header
                sectionBar
                searchBar
                groupBar

                if let current = playerStore.currentChannel {
                    NM7PlayerPane(playerStore: playerStore, fullscreen: { showFullscreen = true })
                    HStack(spacing: 8) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(current.name).font(.headline).lineLimit(1)
                            Text(playerStore.statusText).font(.caption).foregroundStyle(.secondary)
                        }
                        Spacer()
                        Button {
                            playerStore.stop()
                            store.clearPlaying()
                        } label: {
                            Image(systemName: "xmark.circle.fill").font(.title3)
                        }
                        .buttonStyle(.plain)
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 7)
                    Divider()
                }

                if store.isLoading {
                    ProgressView("Đang tải playlist…")
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if store.filteredChannels.isEmpty {
                    ContentUnavailableView(
                        "Không tìm thấy kênh",
                        systemImage: "tv.slash",
                        description: Text("Chọn nguồn IPTV hoặc thay đổi bộ lọc.")
                    )
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    List(store.filteredChannels) { channel in
                        ChannelRow(
                            channel: channel,
                            playing: store.currentChannelID == channel.id,
                            favorite: store.isFavorite(channel),
                            onFavorite: { store.toggleFavorite(channel) }
                        )
                        .contentShape(Rectangle())
                        .onTapGesture {
                            store.recordPlaying(channel)
                            playerStore.play(channel)
                        }
                        .listRowInsets(EdgeInsets(top: 4, leading: 10, bottom: 4, trailing: 10))
                        .listRowBackground(
                            store.currentChannelID == channel.id
                                ? Color.accentColor.opacity(0.20)
                                : Color.clear
                        )
                    }
                    .listStyle(.plain)
                }
            }
            .navigationBarHidden(true)
        }
        .sheet(isPresented: $showSources) {
            PlaylistManagerView(store: store)
        }
        .fullScreenCover(isPresented: $showFullscreen) {
            FullscreenPlayerView(playerStore: playerStore)
        }
        .alert("NM7 IPTV", isPresented: Binding(
            get: { store.errorMessage != nil },
            set: { if !$0 { store.errorMessage = nil } }
        )) {
            Button("Đóng", role: .cancel) { store.errorMessage = nil }
        } message: {
            Text(store.errorMessage ?? "")
        }
        .task { await store.start() }
    }

    private var header: some View {
        HStack(spacing: 10) {
            ZStack {
                RoundedRectangle(cornerRadius: 11)
                    .fill(.thinMaterial)
                Image(systemName: "play.tv.fill")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundStyle(.tint)
            }
            .frame(width: 46, height: 46)

            Text("NM7 IPTV")
                .font(.system(size: 24, weight: .black, design: .rounded))
                .tracking(1.2)
            Spacer()
            Button { showSources = true } label: {
                Image(systemName: "rectangle.stack.badge.plus")
                    .font(.title3)
                    .frame(width: 42, height: 42)
            }
            .buttonStyle(.bordered)
            .accessibilityLabel("Quản lý nguồn IPTV")
        }
        .padding(.horizontal, 14)
        .padding(.top, 8)
        .padding(.bottom, 6)
    }

    private var sectionBar: some View {
        HStack(spacing: 8) {
            ForEach(ChannelSection.allCases) { item in
                Button {
                    store.section = item
                } label: {
                    Label(item.rawValue, systemImage: sectionIcon(item))
                        .font(.subheadline.weight(.semibold))
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(store.section == item ? .borderedProminent : .bordered)
            }
        }
        .padding(.horizontal, 12)
        .padding(.bottom, 7)
    }

    private var searchBar: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass").foregroundStyle(.secondary)
            TextField("Tìm kênh, nhóm hoặc URL…", text: $store.searchText)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
            if !store.searchText.isEmpty {
                Button { store.searchText = "" } label: { Image(systemName: "xmark.circle.fill") }
                    .buttonStyle(.plain)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.horizontal, 12)
        .frame(height: 42)
        .background(Color.secondary.opacity(0.10), in: RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal, 12)
        .padding(.bottom, 6)
    }

    private var groupBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 7) {
                groupButton("Tất cả nhóm", value: "")
                ForEach(store.groups, id: \.self) { group in
                    groupButton(group, value: group)
                }
            }
            .padding(.horizontal, 12)
        }
        .frame(height: 43)
        .padding(.bottom, 4)
    }

    private func groupButton(_ title: String, value: String) -> some View {
        Button(title) { store.selectedGroup = value }
            .buttonStyle(store.selectedGroup == value ? .borderedProminent : .bordered)
            .font(.caption.weight(.semibold))
            .lineLimit(1)
    }

    private func sectionIcon(_ section: ChannelSection) -> String {
        switch section {
        case .all: return "rectangle.grid.1x2"
        case .favorites: return "star.fill"
        case .recent: return "clock.arrow.circlepath"
        }
    }
}

private struct ChannelRow: View {
    let channel: IPTVChannel
    let playing: Bool
    let favorite: Bool
    let onFavorite: () -> Void

    var body: some View {
        HStack(spacing: 10) {
            AsyncImage(url: URL(string: channel.logo)) { phase in
                if case .success(let image) = phase {
                    image.resizable().scaledToFit()
                } else {
                    ZStack {
                        RoundedRectangle(cornerRadius: 8).fill(Color.secondary.opacity(0.12))
                        Text(String(channel.name.prefix(1)).uppercased())
                            .font(.headline.weight(.bold))
                            .foregroundStyle(.tint)
                    }
                }
            }
            .frame(width: 44, height: 44)

            VStack(alignment: .leading, spacing: 3) {
                HStack(spacing: 6) {
                    if playing {
                        Image(systemName: "waveform")
                            .foregroundStyle(.tint)
                    }
                    Text(channel.name)
                        .font(.body.weight(playing ? .bold : .semibold))
                        .lineLimit(1)
                }
                Text(channel.group)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            Spacer(minLength: 6)
            Button(action: onFavorite) {
                Image(systemName: favorite ? "star.fill" : "star")
                    .font(.title3)
                    .foregroundStyle(favorite ? Color.yellow : Color.secondary)
                    .frame(width: 38, height: 38)
            }
            .buttonStyle(.plain)
        }
        .padding(.vertical, 3)
    }
}
