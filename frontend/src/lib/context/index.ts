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
