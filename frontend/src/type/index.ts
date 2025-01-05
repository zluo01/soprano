export interface ISong {
  name: string;
  artists: string;
  albumId: number;
  album: string;
  path: string;
  date: string;
  genre: string;
  composer: string;
  performer: string;
  disc: number;
  trackNum: number;
  duration: number;
  modifiedTime: number;
  addTime: number;
}

export interface IQueueSong extends ISong {
  playing: boolean;
  position: number;
}

export interface IAlbum {
  id: number;
  name: string;
  date: string;
  artist: string;
  addTime: number;
  songs: ISong[];
  totalDuration: number;
}

export interface IPlaylist {
  name: string;
  modifiedTime: number;
  songCount: number;
  coverId?: number;
}

export enum GeneralTag {
  GENRE,
  ARTIST,
  ALBUM_ARTIST,
}

export interface IGeneralTag {
  id: number;
  name: string;
  albumCount: number;
}

export interface IPlaybackStatus {
  playing: boolean;
  elapsed: number;
  loopId: number;
  song?: ISong;
}

export interface IStats {
  albums: number;
  artists: number;
  songs: number;
}

export interface ISearchResponse {
  albums: IAlbum[];
  artists: IGeneralTag[];
  songs: ISong[];
}

export interface IImageLoaderPops {
  albumId?: number;
  alt: string;
  width: number;
  height: number;
  style?: string;
}
