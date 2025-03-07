import Cover from '@/components/cover';
import { usePlaybackStore } from '@/lib/context';
import { GetPlaybackStatusQuery } from '@/lib/queries';
import { lazy, Suspense } from 'react';

const PlaybackDrawer = lazy(() => import('src/components/playback'));

export default function Preview() {
  const { data } = GetPlaybackStatusQuery();

  const { updatePlaybackModalState } = usePlaybackStore();

  return (
    <>
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
      <Suspense>
        <PlaybackDrawer status={data?.PlaybackStatus} />
      </Suspense>
    </>
  );
}
