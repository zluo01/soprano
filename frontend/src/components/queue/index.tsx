import { LoadingSongs } from '@/components/loading';
import { Song } from '@/components/song';
import { SwipeActions } from '@/components/swipe';
import { Button } from '@/components/ui/button.tsx';
import { Drawer, DrawerContent } from '@/components/ui/drawer.tsx';
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
      <DrawerContent className="size-full rounded-2xl pb-[env(safe-area-inset-bottom)] pt-[env(safe-area-inset-top)]">
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
          <div className="flex w-full flex-row flex-nowrap justify-between">
            <p className="font-bold">Continue Playing</p>
            <Button
              variant="link"
              className="dark:text-char"
              onClick={ClearQueue}
              disabled={isEmpty(data?.SongsInQueue)}
            >
              Clear
            </Button>
          </div>

          {isLoading ? (
            <LoadingSongs />
          ) : (
            slice(data?.SongsInQueue, isPlaying ? 1 : 0).map(song => (
              <Song
                key={song.path}
                play={() => PlaySongInAtPosition(song.position)}
                song={song}
                actions={
                  <>
                    <SwipeActions.Action
                      className="bg-delete"
                      onClick={() => RemoveSongFromQueue(song.position)}
                    >
                      <TrashIcon className="size-6" />
                    </SwipeActions.Action>
                    <SwipeActions.Action
                      className="bg-favor"
                      onClick={() => openFavorModal(song.path)}
                    >
                      <HeartFilledIcon className="ml-1 size-6" />
                    </SwipeActions.Action>
                  </>
                }
              />
            ))
          )}
        </div>
      </DrawerContent>
    </Drawer>
  );
}
