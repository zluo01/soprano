import Cover from '@/components/cover';
import { usePlaybackStore } from '@/lib/context';
import { useWebSocketClient } from '@/lib/context/WebSocketContext.tsx';
import {
  OnPlaybackSongUpdateDocument,
  useGetPlaybackSongQuery,
} from '@/lib/queries';
import { useEffect } from 'react';

export default function Preview() {
  const { data, refetch } = useGetPlaybackSongQuery();
  const { updatePlaybackModalState } = usePlaybackStore();
  const graphQLClient = useWebSocketClient();

  useEffect(() => {
    const unsubscribe = graphQLClient.subscribe<{
      OnPlaybackSongUpdate: boolean;
    }>(
      {
        query: OnPlaybackSongUpdateDocument,
      },
      {
        next: data => {
          if (data.data?.OnPlaybackSongUpdate === true) {
            refetch();
          }
        },
        error: error => {
          console.error('Playback subscription error:', error);
        },
        complete: () => {
          /* empty */
        },
      },
    );

    return () => unsubscribe();
  }, [graphQLClient, refetch]);

  return (
    <div className="w-full shadow-md sm:hidden">
      <div className="mx-2 my-1 flex h-fit flex-row flex-nowrap rounded-lg border py-1.5 shadow-2xl">
        <div
          className="flex size-full cursor-pointer flex-row flex-nowrap items-center gap-3 px-4 select-none"
          onClick={() => updatePlaybackModalState(true)}
        >
          <Cover
            albumId={data?.PlaybackStatus.song?.albumId}
            height={50}
            width={50}
            alt={data?.PlaybackStatus.song?.name || ''}
            style={'rounded'}
          />
          <div className="flex h-full w-[calc(100%-80px)] flex-col justify-center gap-0.5">
            <p className="w-full truncate">
              {data?.PlaybackStatus.song?.name || ''}
            </p>
            <div className="truncate overflow-hidden text-sm">
              {data?.PlaybackStatus.song?.artists || ''}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
