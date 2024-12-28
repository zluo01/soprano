import { request } from '@/lib/queries/utils.ts';

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