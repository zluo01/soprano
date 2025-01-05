import { request } from '@/lib/queries/utils.ts';
import { IPlaybackStatus, IQueueSong } from '@/type';
import { useQuery } from '@tanstack/react-query';

const PlaybackStatusQueryDocument = /* GraphQL */ `
  query {
    PlaybackStatus {
      playing
      elapsed
      loopId
      song {
        name
        path
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
      path
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

const PlaySongMutationDocument = /* GraphQL */ `
  mutation PlaySong($songPath: String!) {
    PlaySong(songPath: $songPath)
  }
`;

export async function PlaySong(songPath: string) {
  await request(PlaySongMutationDocument, { songPath });
}

const PlayPlaylistMutationDocument = /* GraphQL */ `
  mutation PlayPlaylist($playlistName: String!) {
    PlayPlaylist(playlistName: $playlistName)
  }
`;

export async function PlayPlaylist(playlistName?: string) {
  if (!playlistName) {
    return;
  }
  await request(PlayPlaylistMutationDocument, { playlistName });
}

const PlayAlbumMutationDocument = /* GraphQL */ `
  mutation PlayAlbum($id: Int!) {
    PlayAlbum(id: $id)
  }
`;

export async function PlayAlbum(id?: number) {
  if (!id) {
    return;
  }
  await request(PlayAlbumMutationDocument, { id });
}

const PauseSongMutationDocument = /* GraphQL */ `
  mutation {
    PauseSong
  }
`;

export async function PauseSong() {
  await request(PauseSongMutationDocument);
}

const NextSongMutationDocument = /* GraphQL */ `
  mutation {
    NextSong
  }
`;

export async function NextSong() {
  await request(NextSongMutationDocument);
}

const PrevSongMutationDocument = /* GraphQL */ `
  mutation {
    PrevSong
  }
`;

export async function PrevSong() {
  await request(PrevSongMutationDocument);
}

const ToggleLoopMutationDocument = /* GraphQL */ `
  mutation ToggleLoop($id: Int!) {
    ToggleLoop(id: $id)
  }
`;

export async function ToggleLoop(currentId?: number) {
  if (currentId === undefined) {
    return;
  }
  const id = (currentId + 1) % 3;
  await request(ToggleLoopMutationDocument, { id });
}

const PlaySongInAtPositionMutationDocument = /* GraphQL */ `
  mutation PlaySongInQueueAtPosition($position: Int!) {
    PlaySongInQueueAtPosition(position: $position)
  }
`;

export async function PlaySongInAtPosition(position?: number) {
  if (!position) {
    return;
  }
  await request(PlaySongInAtPositionMutationDocument, { position });
}

const AddSongsToQueueMutationDocument = /* GraphQL */ `
  mutation AddSongsToQueue($songPaths: [String!]!) {
    AddSongsToQueue(songPaths: $songPaths)
  }
`;

export async function AddSongsToQueue(songPaths: string[]) {
  await request(AddSongsToQueueMutationDocument, { songPaths });
}

const RemoveSongFromQueueMutationDocument = /* GraphQL */ `
  mutation RemoveSongFromQueue($position: Int!) {
    RemoveSongFromQueue(position: $position)
  }
`;

export async function RemoveSongFromQueue(position?: number) {
  if (!position) {
    return;
  }
  await request(RemoveSongFromQueueMutationDocument, { position });
}

const ClearQueueMutationDocument = /* GraphQL */ `
  mutation {
    ClearQueue
  }
`;

export async function ClearQueue() {
  await request(ClearQueueMutationDocument);
}
