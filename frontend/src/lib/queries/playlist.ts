import { IPlaylist, ISong } from '@/type';
import { queryOptions, useMutation, useQuery } from '@tanstack/react-query';

import { IMMUTABLE_REQUEST, request, queryClient } from './utils.ts';

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

export const playlistQueryOptions = queryOptions({
  queryKey: [PlaylistsQueryDocument],
  queryFn: async () =>
    request<{ Playlists: IPlaylist[] }>(PlaylistsQueryDocument),
  ...IMMUTABLE_REQUEST,
});

export function useGetPlaylistsQuery() {
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

export const songsInPlaylistQueryOptions = (name: string) =>
  queryOptions({
    queryKey: [SongsForPlaylistQueryDocument, name],
    queryFn: async () =>
      request<{ PlaylistSongs: ISong[] }>(SongsForPlaylistQueryDocument, {
        name,
      }),
    ...IMMUTABLE_REQUEST,
  });

const CreatePlaylistMutationDocument = /* GraphQL */ `
  mutation CreatePlaylist($name: String!) {
    CreatePlaylist(name: $name)
  }
`;

export function useCreatePlaylistMutation() {
  return useMutation({
    mutationFn: async ({ name }: { name?: string }) => {
      if (!name) {
        return;
      }
      await request(CreatePlaylistMutationDocument, { name });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [PlaylistsQueryDocument] });
    },
  });
}

const DeletePlaylistMutationDocument = /* GraphQL */ `
  mutation DeletePlaylist($name: String!) {
    DeletePlaylist(name: $name)
  }
`;

export function useDeletePlaylistMutation() {
  return useMutation({
    mutationFn: async ({ name }: { name?: string }) => {
      if (!name) {
        return;
      }

      await request(DeletePlaylistMutationDocument, {
        name,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [PlaylistsQueryDocument] });
    },
  });
}

const RenamePlaylistMutationDocument = /* GraphQL */ `
  mutation RenamePlaylist($name: String!, $newName: String!) {
    RenamePlaylist(name: $name, newName: $newName)
  }
`;

export function useRenamePlaylistMutation() {
  return useMutation({
    mutationFn: async ({
      name,
      newName,
    }: {
      name?: string;
      newName?: string;
    }) => {
      if (!name || !newName) {
        return;
      }

      await request(RenamePlaylistMutationDocument, { name, newName });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [PlaylistsQueryDocument] });
    },
  });
}

const AddSongToPlaylistMutationDocument = /* GraphQL */ `
  mutation AddSongToPlaylist($name: String!, $songPath: String!) {
    AddSongToPlaylist(name: $name, songPath: $songPath)
  }
`;

export function useAddSongToPlaylistMutation() {
  return useMutation({
    mutationFn: async ({
      name,
      songPath,
    }: {
      name?: string;
      songPath?: string;
    }) => {
      if (!name || !songPath) {
        return;
      }

      await request(AddSongToPlaylistMutationDocument, {
        name,
        songPath,
      });
    },
    onSuccess: (_data, variables, _context) => {
      queryClient.invalidateQueries({ queryKey: [PlaylistsQueryDocument] });
      queryClient.invalidateQueries({
        queryKey: [SongsForPlaylistQueryDocument, variables.name],
      });
    },
  });
}

const DeleteSongFromPlaylistMutationDocument = /* GraphQL */ `
  mutation DeleteSongFromPlaylist($name: String!, $songPath: String!) {
    DeleteSongFromPlaylist(name: $name, songPath: $songPath)
  }
`;

export function useDeleteSongFromPlaylistMutation() {
  return useMutation({
    mutationFn: async ({
      name,
      songPath,
    }: {
      name?: string;
      songPath?: string;
    }) => {
      if (!name || !songPath) {
        return;
      }

      await request(DeleteSongFromPlaylistMutationDocument, {
        name,
        songPath,
      });
    },
    onSuccess: (_data, variables, _context) => {
      queryClient.invalidateQueries({ queryKey: [PlaylistsQueryDocument] });
      queryClient.invalidateQueries({
        queryKey: [SongsForPlaylistQueryDocument, variables.name],
      });
    },
  });
}
