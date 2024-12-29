import { QUERY_CLIENT } from '@/lib/queries/index.ts';
import { IPlaylist, ISong } from '@/type';
import { skipToken, useMutation, useQuery } from '@tanstack/react-query';

import { IMMUTABLE_REQUEST, request } from './utils.ts';

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

const CreatePlaylistMutationDocument = /* GraphQL */ `
  mutation CreatePlaylist($name: String!) {
    CreatePlaylist(name: $name)
  }
`;

export async function CreatePlaylist(name?: string) {
  if (!name) {
    return;
  }
  await request(CreatePlaylistMutationDocument, { name });
}

const DeletePlaylistMutationDocument = /* GraphQL */ `
  mutation DeletePlaylist($name: String!) {
    DeletePlaylist(name: $name)
  }
`;

export function DeletePlaylistMutation() {
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
      QUERY_CLIENT.invalidateQueries({ queryKey: [PlaylistsQueryDocument] });
    },
  });
}

const RenamePlaylistMutationDocument = /* GraphQL */ `
  mutation RenamePlaylist($name: String!, $newName: String!) {
    RenamePlaylist(name: $name, newName: $newName)
  }
`;

export async function RenamePlaylist(name?: string, newName?: string) {
  if (!name || !newName) {
    return;
  }
  await request(RenamePlaylistMutationDocument, { name, newName });
}

const AddSongToPlaylistMutationDocument = /* GraphQL */ `
  mutation AddSongToPlaylist($name: String!, $songPath: String!) {
    AddSongToPlaylist(name: $name, songPath: $songPath)
  }
`;

export function AddSongToPlaylistMutation() {
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
      QUERY_CLIENT.invalidateQueries({ queryKey: [PlaylistsQueryDocument] });
      QUERY_CLIENT.invalidateQueries({
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

export function DeleteSongFromPlaylistMutation() {
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
      QUERY_CLIENT.invalidateQueries({ queryKey: [PlaylistsQueryDocument] });
      QUERY_CLIENT.invalidateQueries({
        queryKey: [SongsForPlaylistQueryDocument, variables.name],
      });
    },
  });
}
