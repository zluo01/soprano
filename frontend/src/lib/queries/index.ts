import {
  GeneralTag,
  IAlbum,
  IGeneralTag,
  IPlaybackStatus,
  IPlaylist,
  IQueueSong,
  ISearchResponse,
  ISong,
  IStats,
} from '@/type';
import { QueryClient, skipToken, useQuery } from '@tanstack/react-query';
import isEmpty from 'lodash/isEmpty';

import { request } from './utils';

export const QUERY_CLIENT = new QueryClient();

const IMMUTABLE_REQUEST = {
  staleTime: Infinity,
  refetchOnWindowFocus: false,
  refetchOnReconnect: false,
};

const AlbumsQueryDocument = /*GraphQL*/ `
    query {
        Albums {
            id
            name
            date
            artist
            addTime
        }
    }
`;

export function GetDisplayAlbumsQuery() {
  return useQuery({
    queryKey: [AlbumsQueryDocument],
    queryFn: async () => request<{ Albums: IAlbum[] }>(AlbumsQueryDocument),
    ...IMMUTABLE_REQUEST,
  });
}

const AlbumDetailQueryDocument = /*GraphQL*/ `
    query AlbumDetail($id: Int!) {
        Album(id: $id) {
            id
            name
            date
            artist
            songs {
                name,
                artists,
                path
            }
            totalDuration
        }
    }
`;

export function GetAlbumDetailQuery(albumId?: string) {
  return useQuery({
    queryKey: [AlbumDetailQueryDocument, albumId],
    queryFn: !isEmpty(albumId)
      ? async () =>
          request<{ Album: IAlbum }>(AlbumDetailQueryDocument, {
            id: parseInt(albumId!),
          })
      : skipToken,
    ...IMMUTABLE_REQUEST,
  });
}

const PlaylistsQueryDocument = /*GraphQL*/ `
    query {
        Playlists {
            name
            modifiedTime
            songCount
            coverId
        }
    }
`;

export function GetPlaylistsQuery() {
  return useQuery({
    queryKey: [PlaylistsQueryDocument],
    queryFn: async () =>
      request<{ Playlists: IPlaylist[] }>(PlaylistsQueryDocument),
    ...IMMUTABLE_REQUEST,
  });
}

const SongsForPlaylistQueryDocument = /*GraphQL*/ `
    query PlaylistSongs($name: String!) {
        PlaylistSongs(name: $name) {
            name
            artists
            path
            duration
            albumId
        }
    }
`;

export function GetSongsForPlaylistQuery(name?: string) {
  return useQuery({
    queryKey: [SongsForPlaylistQueryDocument, name],
    queryFn: name
      ? async () =>
          request<{ PlaylistSongs: ISong[] }>(SongsForPlaylistQueryDocument, {
            name,
          })
      : skipToken,
    ...IMMUTABLE_REQUEST,
  });
}

const GenreQueryDocument = /*GraphQL*/ `
    query {
        Genres {
            id
            name
            albumCount
        }
    }
`;

const ArtistQueryDocument = /*GraphQL*/ `
    query {
        Artists {
            id
            name
            albumCount
        }
    }
`;

const AlbumArtistQueryDocument = /*GraphQL*/ `
    query {
        AlbumArtists {
            id
            name
            albumCount
        }
    }
`;

function generalTagQuery(tag: GeneralTag): string {
  switch (tag) {
    case GeneralTag.GENRE:
      return GenreQueryDocument;
    case GeneralTag.ARTIST:
      return ArtistQueryDocument;
    case GeneralTag.ALBUM_ARTIST:
      return AlbumArtistQueryDocument;
  }
}

interface GetGeneralTagQueryResponse {
  data?: IGeneralTag[];
  isLoading: boolean;
}

export function GetGeneralTagQuery(
  tag: GeneralTag,
): GetGeneralTagQueryResponse {
  const query = generalTagQuery(tag);
  const { data, isLoading } = useQuery({
    queryKey: [query],
    queryFn: async () =>
      request<{
        Genres?: IGeneralTag[];
        Artists?: IGeneralTag[];
        AlbumArtists?: IGeneralTag[];
      }>(query),
    ...IMMUTABLE_REQUEST,
  });

  const response = (() => {
    switch (tag) {
      case GeneralTag.GENRE:
        return data?.Genres;
      case GeneralTag.ARTIST:
        return data?.Artists;
      case GeneralTag.ALBUM_ARTIST:
        return data?.AlbumArtists;
    }
  })();

  return {
    data: response,
    isLoading: isLoading,
  };
}

const AlbumForGenreQueryDocument = /*GraphQL*/ `
    query AlbumForGenre($id: Int!) {
        GenreAlbums(id: $id) {
            id
            name
            date
            artist
            addTime
        }
    }
`;

const AlbumForArtistQueryDocument = /*GraphQL*/ `
    query AlbumForArtist($id: Int!) {
        ArtistAlbums(id: $id) {
            id
            name
            date
            artist
            addTime
        }
    }
`;

const AlbumForAlbumArtistQueryDocument = /*GraphQL*/ `
    query AlbumForAlbumArtist($id: Int!) {
        AlbumArtistAlbums(id: $id) {
            id
            name
            date
            artist
            addTime
        }
    }
`;

function albumsForTagQuery(tag: GeneralTag): string {
  switch (tag) {
    case GeneralTag.GENRE:
      return AlbumForGenreQueryDocument;
    case GeneralTag.ARTIST:
      return AlbumForArtistQueryDocument;
    case GeneralTag.ALBUM_ARTIST:
      return AlbumForAlbumArtistQueryDocument;
  }
}

export function GetGeneralTagAlbumsQuery(tag: GeneralTag, id?: string) {
  const query = albumsForTagQuery(tag);
  const { data, isLoading } = useQuery({
    queryKey: [query, id],
    queryFn: id
      ? async () =>
          request<{
            GenreAlbums?: IAlbum[];
            ArtistAlbums?: IAlbum[];
            AlbumArtistAlbums?: IAlbum[];
          }>(query, { id })
      : skipToken,
    ...IMMUTABLE_REQUEST,
  });

  const response = (() => {
    switch (tag) {
      case GeneralTag.GENRE:
        return data?.GenreAlbums;
      case GeneralTag.ARTIST:
        return data?.ArtistAlbums;
      case GeneralTag.ALBUM_ARTIST:
        return data?.AlbumArtistAlbums;
    }
  })();

  return {
    data: response,
    isLoading,
  };
}

const StatsQueryDocument = /* GraphQL */ `
  query {
    Stats {
      albums
      artists
      songs
    }
  }
`;

export function GetStatsQuery() {
  return useQuery({
    queryKey: [StatsQueryDocument],
    queryFn: async () => request<{ Stats: IStats }>(StatsQueryDocument),
    ...IMMUTABLE_REQUEST,
  });
}

const SearchQueryDocument = /* GraphQL */ `
  query Search($searchText: String!) {
    Search(key: $searchText) {
      albums {
        id
        name
        artist
      }
      artists {
        id
        name
        albumCount
      }
      songs {
        name
        artists
        path
        duration
        albumId
      }
    }
  }
`;

export function GetSearchQuery(searchText: string) {
  return useQuery({
    queryKey: [SearchQueryDocument, searchText],
    queryFn: !isEmpty(searchText)
      ? async () =>
          request<{
            Search: ISearchResponse;
          }>(SearchQueryDocument, { searchText })
      : skipToken,
  });
}

const PlaybackStatusQueryDocument = /* GraphQL */ `
  query {
    PlaybackStatus {
      playing
      elapsed
      song {
        name
        artists
        albumId
        album
        duration
      }
    }
  }
`;

export function GetPlaybackStatusQuery() {
  return useQuery({
    queryKey: [PlaybackStatusQueryDocument],
    queryFn: async () =>
      request<{
        PlaybackStatus: IPlaybackStatus;
      }>(PlaybackStatusQueryDocument),
    refetchInterval: 1000,
    refetchIntervalInBackground: false,
  });
}

const SongsInQueueQueryDocument = /* GraphQL */ `
  query {
    SongsInQueue {
      playing
      position
      name
      artists
      albumId
    }
  }
`;

export function GetSongInQueueQuery() {
  return useQuery({
    queryKey: [SongsInQueueQueryDocument],
    queryFn: async () =>
      request<{
        SongsInQueue: IQueueSong[];
      }>(SongsInQueueQueryDocument),
    refetchInterval: 1000,
    refetchIntervalInBackground: false,
  });
}

export * from './player-mutation.ts';
export * from './playlist-mutation.ts';
export * from './database-mutation.ts';
export { constructImg } from './utils.ts';
