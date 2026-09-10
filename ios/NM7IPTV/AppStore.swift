import Foundation
import Combine

@MainActor
final class AppStore: ObservableObject {
    static let defaultURL = "https://iptv-live-merge.phuongnm7-iptv.workers.dev/playlist.m3u"

    @Published var sources: [PlaylistSource] = []
    @Published var channels: [IPTVChannel] = []
    @Published var selectedSourceID: UUID?
    @Published var selectedGroup = ""
    @Published var searchText = ""
    @Published var section: ChannelSection = .all
    @Published var currentChannelID: String?
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var epgURL = ""

    private let defaults = UserDefaults.standard
    private let parser = M3UParser()
    private var favorites = Set<String>()
    private var recent: [String] = []

    init() {
        restoreLocalState()
    }

    var selectedSource: PlaylistSource? {
        guard let selectedSourceID else { return nil }
        return sources.first { $0.id == selectedSourceID }
    }

    var groups: [String] {
        Array(Set(channels.map(\.group))).sorted { $0.localizedCaseInsensitiveCompare($1) == .orderedAscending }
    }

    var filteredChannels: [IPTVChannel] {
        let query = searchText.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        var result = channels.filter { channel in
            let groupOK = selectedGroup.isEmpty || channel.group == selectedGroup
            let sectionOK: Bool
            switch section {
            case .all: sectionOK = true
            case .favorites: sectionOK = favorites.contains(channel.id)
            case .recent: sectionOK = recent.contains(channel.id)
            }
            let queryOK = query.isEmpty
                || channel.name.lowercased().contains(query)
                || channel.group.lowercased().contains(query)
                || channel.url.lowercased().contains(query)
            return groupOK && sectionOK && queryOK
        }
        if section == .recent {
            let order = Dictionary(uniqueKeysWithValues: recent.enumerated().map { ($0.element, $0.offset) })
            result.sort { (order[$0.id] ?? Int.max) < (order[$1.id] ?? Int.max) }
        }
        return result
    }

    func start() async {
        if sources.isEmpty {
            let source = PlaylistSource(name: "Nguồn IPTV mặc định", url: Self.defaultURL)
            sources = [source]
            selectedSourceID = source.id
            persistSources()
        }
        if selectedSourceID == nil { selectedSourceID = sources.first?.id }
        if let source = selectedSource { await load(source) }
    }

    func load(_ source: PlaylistSource) async {
        guard let url = URL(string: source.url), ["http", "https"].contains(url.scheme?.lowercased() ?? "") else {
            errorMessage = "Link playlist phải bắt đầu bằng http:// hoặc https://"
            return
        }
        isLoading = true
        errorMessage = nil
        do {
            var request = URLRequest(url: url, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: 35)
            request.setValue("NM7-IPTV-iOS/0.1", forHTTPHeaderField: "User-Agent")
            request.setValue("application/vnd.apple.mpegurl,application/x-mpegURL,text/plain,*/*", forHTTPHeaderField: "Accept")
            let (data, response) = try await URLSession.shared.data(for: request)
            if let http = response as? HTTPURLResponse, !(200..<300).contains(http.statusCode) {
                throw StoreError.message("HTTP \(http.statusCode)")
            }
            guard data.count <= 12 * 1024 * 1024 else { throw StoreError.message("Playlist lớn hơn 12 MB") }
            guard let text = String(data: data, encoding: .utf8) ?? String(data: data, encoding: .isoLatin1) else {
                throw StoreError.message("Không đọc được nội dung playlist")
            }
            let effectiveURL = response.url ?? url
            let parsed = try parser.parse(text, baseURL: effectiveURL)
            guard !parsed.channels.isEmpty else { throw StoreError.message("Playlist không có kênh hợp lệ") }
            channels = parsed.channels
            epgURL = parsed.epgURL
            selectedSourceID = source.id
            selectedGroup = ""
            searchText = ""
            persistSources()
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    func addSource(name: String, url: String) async {
        let cleanURL = url.trimmingCharacters(in: .whitespacesAndNewlines)
        guard M3UParser.isNetworkURL(cleanURL), cleanURL.lowercased().hasPrefix("http") else {
            errorMessage = "Link playlist phải bắt đầu bằng http:// hoặc https://"
            return
        }
        sources.removeAll { $0.url == cleanURL }
        let host = URL(string: cleanURL)?.host ?? "Nguồn IPTV"
        let cleanName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        let source = PlaylistSource(name: cleanName.isEmpty ? host : String(cleanName.prefix(80)), url: cleanURL)
        sources.insert(source, at: 0)
        if sources.count > 50 { sources = Array(sources.prefix(50)) }
        persistSources()
        await load(source)
    }

    func updateSource(_ source: PlaylistSource, name: String, url: String) async {
        guard let index = sources.firstIndex(where: { $0.id == source.id }) else { return }
        let cleanURL = url.trimmingCharacters(in: .whitespacesAndNewlines)
        guard cleanURL.hasPrefix("https://") || cleanURL.hasPrefix("http://") else {
            errorMessage = "Link playlist phải bắt đầu bằng http:// hoặc https://"
            return
        }
        let cleanName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        sources[index].name = cleanName.isEmpty ? (URL(string: cleanURL)?.host ?? "Nguồn IPTV") : String(cleanName.prefix(80))
        sources[index].url = cleanURL
        persistSources()
        await load(sources[index])
    }

    func removeSource(_ source: PlaylistSource) {
        sources.removeAll { $0.id == source.id }
        if selectedSourceID == source.id {
            selectedSourceID = sources.first?.id
            channels = []
            currentChannelID = nil
        }
        persistSources()
    }

    func toggleFavorite(_ channel: IPTVChannel) {
        if favorites.contains(channel.id) { favorites.remove(channel.id) } else { favorites.insert(channel.id) }
        persistFavorites()
        objectWillChange.send()
    }

    func isFavorite(_ channel: IPTVChannel) -> Bool { favorites.contains(channel.id) }

    func recordPlaying(_ channel: IPTVChannel) {
        currentChannelID = channel.id
        recent.removeAll { $0 == channel.id }
        recent.insert(channel.id, at: 0)
        if recent.count > 30 { recent = Array(recent.prefix(30)) }
        defaults.set(recent, forKey: "nm7.recent")
    }

    func clearPlaying() { currentChannelID = nil }

    private func restoreLocalState() {
        if let data = defaults.data(forKey: "nm7.sources"), let value = try? JSONDecoder().decode([PlaylistSource].self, from: data) {
            sources = value
        }
        if sources.isEmpty {
            let source = PlaylistSource(name: "Nguồn IPTV mặc định", url: Self.defaultURL)
            sources = [source]
            selectedSourceID = source.id
        }
        if let id = defaults.string(forKey: "nm7.selectedSource"), let uuid = UUID(uuidString: id), sources.contains(where: { $0.id == uuid }) {
            selectedSourceID = uuid
        } else if selectedSourceID == nil {
            selectedSourceID = sources.first?.id
        }
        favorites = Set(defaults.stringArray(forKey: "nm7.favorites") ?? [])
        recent = defaults.stringArray(forKey: "nm7.recent") ?? []
    }

    private func persistSources() {
        if let data = try? JSONEncoder().encode(sources) { defaults.set(data, forKey: "nm7.sources") }
        defaults.set(selectedSourceID?.uuidString, forKey: "nm7.selectedSource")
    }

    private func persistFavorites() { defaults.set(Array(favorites), forKey: "nm7.favorites") }
}

enum StoreError: LocalizedError {
    case message(String)
    var errorDescription: String? { if case .message(let value) = self { return value }; return nil }
}
