import Cover from '@/components/cover';
import Control from '@/components/playback/control.tsx';
import ProgressPreview from '@/components/playback/progress.tsx';
import { Button } from '@/components/ui/button.tsx';
import {
  Drawer,
  DrawerContent,
  DrawerFooter,
} from '@/components/ui/drawer.tsx';
import {
  useFavorStore,
  usePlaybackQueueStore,
  usePlaybackStore,
} from '@/lib/context';
import { toggleLoop, useGetPlaybackStatusQuery } from '@/lib/queries';
import { HeartFilledIcon, ListBulletIcon } from '@radix-ui/react-icons';
import { useNavigate } from '@tanstack/react-router';
import { Repeat, Repeat1 } from 'lucide-react';

function LoopIcon({ id }: { id: number }) {
  switch (id) {
    case 1:
      return <Repeat1 className="size-6 text-blue dark:text-char" />;
    case 2:
      return <Repeat className="size-6 text-blue dark:text-char" />;
    default:
      return <Repeat className="size-6" />;
  }
}

export default function PlaybackDrawer() {
  const navigate = useNavigate();

  const { playbackModalState, updatePlaybackModalState } = usePlaybackStore();
  const { updatePlaybackQueueModalState } = usePlaybackQueueStore();
  const { openFavorModal } = useFavorStore();

  const { data } = useGetPlaybackStatusQuery(playbackModalState);

  async function toAlbumPage() {
    if (!data?.PlaybackStatus.song) {
      return;
    }
    await navigate({
      to: '/albums/$id',
      params: { id: data.PlaybackStatus.song.albumId.toString() },
    });
    updatePlaybackModalState(false);
  }

  return (
    <Drawer
      open={playbackModalState}
      onOpenChange={open => updatePlaybackModalState(open)}
    >
      <DrawerContent className="h-full data-[vaul-drawer-direction=bottom]:mt-0 data-[vaul-drawer-direction=bottom]:max-h-screen">
        <div className="flex h-full flex-col items-center justify-between px-6 py-12">
          <div className="flex w-full items-center justify-center">
            <Cover
              albumId={data?.PlaybackStatus.song?.albumId}
              height={400}
              width={400}
              alt={data?.PlaybackStatus.song?.name || ''}
            />
          </div>

          <div className="flex min-h-16 w-full flex-col flex-wrap items-center gap-3 text-center select-none">
            <span className="w-full truncate text-xl font-extrabold">
              {data?.PlaybackStatus.song?.name}
            </span>
            <span
              className="w-full cursor-pointer truncate text-primary/35"
              onClick={toAlbumPage}
            >
              {data?.PlaybackStatus.song?.album}
            </span>
          </div>

          <ProgressPreview
            elapsed={data?.PlaybackStatus.elapsed || 0}
            duration={data?.PlaybackStatus.song?.duration || 0}
          />
          <Control playing={data?.PlaybackStatus.playing || false} />
        </div>
        <DrawerFooter className="bottom-0 flex flex-row flex-nowrap items-center justify-between px-6">
          <Button
            variant="ghost"
            className="size-10 rounded-full p-1"
            onClick={toggleLoop}
          >
            <LoopIcon id={data?.PlaybackStatus.loopId || 0} />
          </Button>
          <Button
            variant="ghost"
            className="size-10 rounded-full p-1"
            onClick={() => updatePlaybackQueueModalState(true)}
          >
            <ListBulletIcon className="size-6" />
          </Button>
          <Button
            variant="ghost"
            className="size-10 rounded-full p-1"
            onClick={() => openFavorModal(data?.PlaybackStatus.song?.path)}
          >
            <HeartFilledIcon className="size-6" />
            <span className="sr-only">Favor album</span>
          </Button>
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  );
}
