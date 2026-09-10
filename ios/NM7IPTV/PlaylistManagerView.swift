import SwiftUI

struct PlaylistManagerView: View {
    @ObservedObject var store: AppStore
    @Environment(\.dismiss) private var dismiss
    @State private var editor: SourceEditorState?

    var body: some View {
        NavigationStack {
            List {
                Section("Nguồn IPTV") {
                    ForEach(store.sources) { source in
                        HStack(spacing: 10) {
                            VStack(alignment: .leading, spacing: 3) {
                                Text(source.name).font(.headline)
                                Text(source.url)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                                    .lineLimit(2)
                            }
                            Spacer()
                            if store.selectedSourceID == source.id {
                                Image(systemName: "checkmark.circle.fill").foregroundStyle(.tint)
                            }
                        }
                        .contentShape(Rectangle())
                        .onTapGesture {
                            Task {
                                await store.load(source)
                                if store.errorMessage == nil { dismiss() }
                            }
                        }
                        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                            Button(role: .destructive) { store.removeSource(source) } label: {
                                Label("Xóa", systemImage: "trash")
                            }
                            Button { editor = SourceEditorState(source: source) } label: {
                                Label("Sửa", systemImage: "pencil")
                            }
                            .tint(.blue)
                        }
                    }
                }

                Section {
                    Button {
                        editor = SourceEditorState(source: nil)
                    } label: {
                        Label("Thêm nguồn IPTV", systemImage: "plus.circle.fill")
                    }
                } footer: {
                    Text("Có thể lưu tối đa 50 playlist. Chạm vào một nguồn để chuyển playlist.")
                }
            }
            .navigationTitle("Quản lý nguồn IPTV")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Đóng") { dismiss() }
                }
                ToolbarItem(placement: .primaryAction) {
                    Button { editor = SourceEditorState(source: nil) } label: { Image(systemName: "plus") }
                }
            }
            .sheet(item: $editor) { state in
                SourceEditorView(store: store, state: state)
            }
        }
    }
}

private struct SourceEditorState: Identifiable {
    let id = UUID()
    let source: PlaylistSource?
}

private struct SourceEditorView: View {
    @ObservedObject var store: AppStore
    let state: SourceEditorState
    @Environment(\.dismiss) private var dismiss
    @State private var name: String
    @State private var url: String
    @State private var saving = false

    init(store: AppStore, state: SourceEditorState) {
        self.store = store
        self.state = state
        _name = State(initialValue: state.source?.name ?? "")
        _url = State(initialValue: state.source?.url ?? "")
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Tên nguồn") {
                    TextField("Ví dụ: Thể thao", text: $name)
                }
                Section("Link M3U") {
                    TextField("https://.../playlist.m3u", text: $url)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .keyboardType(.URL)
                }
            }
            .navigationTitle(state.source == nil ? "Thêm playlist" : "Sửa playlist")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Hủy") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button(saving ? "Đang lưu…" : "Lưu") {
                        saving = true
                        Task {
                            if let source = state.source {
                                await store.updateSource(source, name: name, url: url)
                            } else {
                                await store.addSource(name: name, url: url)
                            }
                            saving = false
                            if store.errorMessage == nil { dismiss() }
                        }
                    }
                    .disabled(saving || url.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
    }
}
