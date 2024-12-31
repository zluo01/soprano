import { create } from 'zustand';

interface IPlaybackState {
  playbackModalState: boolean;
  updatePlaybackModalState: (state: boolean) => void;
}

export const usePlaybackStore = create<IPlaybackState>()(set => ({
  playbackModalState: false,
  updatePlaybackModalState: state => set({ playbackModalState: state }),
}));

interface IPlaybackQueueState {
  playbackQueueModalState: boolean;
  updatePlaybackQueueModalState: (state: boolean) => void;
}

export const usePlaybackQueueStore = create<IPlaybackQueueState>()(set => ({
  playbackQueueModalState: false,
  updatePlaybackQueueModalState: state =>
    set({ playbackQueueModalState: state }),
}));

interface ISearchState {
  searchModalState: boolean;
  updateSearchModalState: (state: boolean) => void;
}

export const useSearchStore = create<ISearchState>()(set => ({
  searchModalState: false,
  updateSearchModalState: state => set({ searchModalState: state }),
}));

interface ISettingState {
  settingModalState: boolean;
  updateSettingModalState: (state: boolean) => void;
}

export const useSettingStore = create<ISettingState>()(set => ({
  settingModalState: false,
  updateSettingModalState: state => set({ settingModalState: state }),
}));

interface IFavorPayload {
  open: boolean;
  songPath?: string;
}

interface IFavorState {
  favorModalState: IFavorPayload;
  openFavorModal: (songPath?: string) => void;
  closeFavorModal: () => void;
}

export const useFavorStore = create<IFavorState>()(set => ({
  favorModalState: { open: false },
  openFavorModal: songPath =>
    set({ favorModalState: { open: true, songPath } }),
  closeFavorModal: () => set({ favorModalState: { open: false } }),
}));

interface IRenamePlaylistPayload {
  open: boolean;
  name?: string;
  readonly prevName?: string;
}

interface IRenamePlaylistState {
  renamePlaylistModalState: IRenamePlaylistPayload;
  updateName: (name: string) => void;
  openRenamePlaylistModal: (name: string) => void;
  closeRenamePlaylistModal: () => void;
}

export const useRenamePlaylistStore = create<IRenamePlaylistState>()(set => ({
  renamePlaylistModalState: { open: false },
  updateName: name =>
    set(state => ({
      renamePlaylistModalState: { ...state.renamePlaylistModalState, name },
    })),
  openRenamePlaylistModal: name =>
    set({ renamePlaylistModalState: { open: true, name, prevName: name } }),
  closeRenamePlaylistModal: () =>
    set({ renamePlaylistModalState: { open: false } }),
}));

interface ICreatePlaylistState {
  createPlaylistModalState: boolean;
  updateCreatePlaylistModalState: (state: boolean) => void;
}

export const useCreatePlaylistStore = create<ICreatePlaylistState>()(set => ({
  createPlaylistModalState: false,
  updateCreatePlaylistModalState: state =>
    set({ createPlaylistModalState: state }),
}));
