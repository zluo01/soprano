import {
  GeneralTag,
  IAlbum,
  IGeneralTag,
  ISearchResponse,
  IStats,
} from '@/type';
import { queryOptions, skipToken, useQuery } from '@tanstack/react-query';
import isEmpty from 'lodash-es/isEmpty';

import { IMMUTABLE_REQUEST, request } from './utils';

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

export const displayAlbumsQueryOptions = queryOptions({
  queryKey: [AlbumsQueryDocument],
  queryFn: async () => request<{ Albums: IAlbum[] }>(AlbumsQueryDocument),
  ...IMMUTABLE_REQUEST,
});

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

export const albumDetailQueryOptions = (albumId: string) =>
  queryOptions({
    queryKey: [AlbumDetailQueryDocument, albumId],
    queryFn: async () =>
      request<{ Album: IAlbum }>(AlbumDetailQueryDocument, {
        id: parseInt(albumId!),
      }),
    ...IMMUTABLE_REQUEST,
  });

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

export const generalTagQueryOptions = (tag: GeneralTag) => {
  const query = generalTagQuery(tag);
  return queryOptions({
    queryKey: [query],
    queryFn: async () =>
      request<{
        Genres?: IGeneralTag[];
        Artists?: IGeneralTag[];
        AlbumArtists?: IGeneralTag[];
      }>(query).then(response => {
        switch (tag) {
          case GeneralTag.GENRE:
            return response.Genres;
          case GeneralTag.ARTIST:
            return response.Artists;
          case GeneralTag.ALBUM_ARTIST:
            return response.AlbumArtists;
          default:
            throw Error('Unknown tag: ' + tag);
        }
      }),
    ...IMMUTABLE_REQUEST,
  });
};

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

export const generalTagAlbumsQueryOptions = (tag: GeneralTag, id: string) => {
  const query = albumsForTagQuery(tag);
  return queryOptions({
    queryKey: [query, id],
    queryFn: async () =>
      request<{
        GenreAlbums?: IAlbum[];
        ArtistAlbums?: IAlbum[];
        AlbumArtistAlbums?: IAlbum[];
      }>(query, { id }).then(response => {
        switch (tag) {
          case GeneralTag.GENRE:
            return response.GenreAlbums;
          case GeneralTag.ARTIST:
            return response.ArtistAlbums;
          case GeneralTag.ALBUM_ARTIST:
            return response.AlbumArtistAlbums;
          default:
            throw Error('Unknown tag: ' + tag);
        }
      }),
    ...IMMUTABLE_REQUEST,
  });
};

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

export * from './playback.ts';
export * from './playlist.ts';
export * from './database-mutation.ts';
export { constructImg, queryClient } from './utils.ts';
