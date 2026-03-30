import { useQuery } from '@tanstack/react-query';
import type { IPlaybackStatus, IQueueSong } from '@/type';

import { request } from './utils.ts';

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

const playbackStatusQueryKey = [PlaybackStatusQueryDocument];

export function useGetPlaybackStatusQuery(options?: {
	enabled?: boolean;
	refetchInterval?: number;
}) {
	return useQuery({
		queryKey: playbackStatusQueryKey,
		queryFn: async () =>
			request<{
				PlaybackStatus: IPlaybackStatus;
			}>(PlaybackStatusQueryDocument),
		...options,
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

export function useGetSongInQueueQuery(enabled: boolean) {
	return useQuery({
		queryKey: [SongsInQueueQueryDocument],
		queryFn: async () =>
			request<{
				SongsInQueue: IQueueSong[];
			}>(SongsInQueueQueryDocument),
		enabled,
		refetchInterval: 1000,
		refetchIntervalInBackground: false,
	});
}

const PlaySongMutationDocument = /* GraphQL */ `
  mutation PlaySong($songPath: String!) {
    PlaySong(songPath: $songPath)
  }
`;

export async function playSong(songPath: string) {
	await request(PlaySongMutationDocument, { songPath });
}

const PlayPlaylistMutationDocument = /* GraphQL */ `
  mutation PlayPlaylist($playlistName: String!) {
    PlayPlaylist(playlistName: $playlistName)
  }
`;

export async function playPlaylist(playlistName?: string) {
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

export async function playAlbum(id?: number) {
	if (id == null) {
		return;
	}
	await request(PlayAlbumMutationDocument, { id });
}

const PauseSongMutationDocument = /* GraphQL */ `
  mutation {
    PauseSong
  }
`;

export async function pauseSong() {
	await request(PauseSongMutationDocument);
}

const NextSongMutationDocument = /* GraphQL */ `
  mutation {
    NextSong
  }
`;

export async function nextSong() {
	await request(NextSongMutationDocument);
}

const PrevSongMutationDocument = /* GraphQL */ `
  mutation {
    PrevSong
  }
`;

export async function prevSong() {
	await request(PrevSongMutationDocument);
}

const ToggleLoopMutationDocument = /* GraphQL */ `
  mutation ToggleLoop {
    ToggleLoop
  }
`;

export async function toggleLoop() {
	await request(ToggleLoopMutationDocument);
}

const PlaySongInAtPositionMutationDocument = /* GraphQL */ `
  mutation PlaySongInQueueAtPosition($position: Int!) {
    PlaySongInQueueAtPosition(position: $position)
  }
`;

export async function playSongInAtPosition(position?: number) {
	if (position == null) {
		return;
	}
	await request(PlaySongInAtPositionMutationDocument, { position });
}

const AddSongsToQueueMutationDocument = /* GraphQL */ `
  mutation AddSongsToQueue($songPaths: [String!]!) {
    AddSongsToQueue(songPaths: $songPaths)
  }
`;

export async function addSongsToQueue(songPaths: string[]) {
	await request(AddSongsToQueueMutationDocument, { songPaths });
}

const RemoveSongFromQueueMutationDocument = /* GraphQL */ `
  mutation RemoveSongFromQueue($position: Int!) {
    RemoveSongFromQueue(position: $position)
  }
`;

export async function removeSongFromQueue(position?: number) {
	if (position == null) {
		return;
	}
	await request(RemoveSongFromQueueMutationDocument, { position });
}

const ClearQueueMutationDocument = /* GraphQL */ `
  mutation {
    ClearQueue
  }
`;

export async function clearQueue() {
	await request(ClearQueueMutationDocument);
}

export const OnPlaybackSongUpdateDocument = /* GraphQL */ `
  subscription OnPlaybackSongUpdate {
    OnPlaybackSongUpdate
  }
`;
