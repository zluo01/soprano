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
  clearQueue,
  useGetSongInQueueQuery,
  playSongInAtPosition,
  removeSongFromQueue,
} from '@/lib/queries';
import { HeartFilledIcon, TrashIcon } from '@radix-ui/react-icons';
import isEmpty from 'lodash-es/isEmpty';
import slice from 'lodash-es/slice';

export default function PlaybackQueue() {
  const { playbackQueueModalState, updatePlaybackQueueModalState } =
    usePlaybackQueueStore();
  const { openFavorModal } = useFavorStore();

  const { data, isLoading } = useGetSongInQueueQuery();

  const isPlaying = data?.SongsInQueue[0]
    ? data?.SongsInQueue[0].playing
    : false;

  return (
    <Drawer
      open={playbackQueueModalState}
      onOpenChange={open => updatePlaybackQueueModalState(open)}
    >
      <DrawerContent className="size-full data-[vaul-drawer-direction=bottom]:mt-0 data-[vaul-drawer-direction=bottom]:max-h-screen">
        <div className="flex size-full flex-col flex-nowrap overflow-y-scroll p-6">
          <p className="font-bold">Now Playing</p>
          {isPlaying ? (
            <Song
              play={() => playSongInAtPosition(data!.SongsInQueue[0].position)}
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
                play={() => playSongInAtPosition(song.position)}
                song={song}
                actions={[
                  {
                    className: 'bg-delete',
                    action: () => removeSongFromQueue(song.position),
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
            onClick={clearQueue}
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
