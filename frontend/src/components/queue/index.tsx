import { LoadingSongs } from '@/components/loading';
import { Song } from '@/components/song';
import { Button } from '@/components/ui/button.tsx';
import {
  Drawer,
  DrawerContent,
  DrawerFooter,
} from '@/components/ui/drawer.tsx';
import { useFavorStore, usePlaybackQueueStore } from '@/lib/context';
import {
  ClearQueue,
  GetSongInQueueQuery,
  PlaySongInAtPosition,
  RemoveSongFromQueue,
} from '@/lib/queries';
import { HeartFilledIcon, TrashIcon } from '@radix-ui/react-icons';
import isEmpty from 'lodash/isEmpty';
import slice from 'lodash/slice';

export default function PlaybackQueue() {
  const { playbackQueueModalState, updatePlaybackQueueModalState } =
    usePlaybackQueueStore();
  const { openFavorModal } = useFavorStore();

  const { data, isLoading } = GetSongInQueueQuery();

  const isPlaying = data?.SongsInQueue[0]
    ? data?.SongsInQueue[0].playing
    : false;

  return (
    <Drawer
      open={playbackQueueModalState}
      onOpenChange={open => updatePlaybackQueueModalState(open)}
    >
      <DrawerContent className="size-full pt-[env(safe-area-inset-top)]">
        <div className="flex size-full flex-col flex-nowrap overflow-y-scroll p-6">
          <p className="font-bold">Now Playing</p>
          {isPlaying ? (
            <Song
              play={() => PlaySongInAtPosition(data!.SongsInQueue[0].position)}
              song={data!.SongsInQueue[0]}
            />
          ) : (
            <div className="h-16 w-full" />
          )}
          <p className="font-bold">Continue Playing</p>

          {isLoading ? (
            <LoadingSongs />
          ) : (
            slice(data?.SongsInQueue, isPlaying ? 1 : 0).map(song => (
              <Song
                key={song.path}
                play={() => PlaySongInAtPosition(song.position)}
                song={song}
                actions={[
                  {
                    className: 'bg-delete',
                    action: () => RemoveSongFromQueue(song.position),
                    children: <TrashIcon className="size-6" />,
                  },
                  {
                    className: 'bg-favor',
                    action: () => openFavorModal(song.path),
                    children: <HeartFilledIcon className="ml-1 size-6" />,
                  },
                ]}
              />
            ))
          )}
        </div>
        <DrawerFooter className="items-center">
          <Button
            variant="ghost"
            size="icon"
            onClick={ClearQueue}
            disabled={isEmpty(data?.SongsInQueue)}
            className="size-10 rounded-full p-1.5"
          >
            <TrashIcon className="size-6" />
          </Button>
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  );
}
