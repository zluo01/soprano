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
import { ToggleLoop } from '@/lib/queries';
import { IPlaybackStatus } from '@/type';
import { HeartFilledIcon, ListBulletIcon } from '@radix-ui/react-icons';
import { Repeat, Repeat1 } from 'lucide-react';
import { useNavigate } from 'react-router';

interface IPlaybackDrawerProps {
  status?: IPlaybackStatus;
}

function LoopIcon({ id }: { id: number }) {
  switch (id) {
    case 1:
      return <Repeat className="size-6 text-blue dark:text-char" />;
    case 2:
      return <Repeat1 className="size-6 text-blue dark:text-char" />;
    default:
      return <Repeat className="size-6" />;
  }
}

export default function PlaybackDrawer({ status }: IPlaybackDrawerProps) {
  const navigate = useNavigate();

  const { playbackModalState, updatePlaybackModalState } = usePlaybackStore();
  const { updatePlaybackQueueModalState } = usePlaybackQueueStore();
  const { openFavorModal } = useFavorStore();

  async function toAlbumPage() {
    await navigate(`/albums/${status?.song?.albumId}`);
    updatePlaybackModalState(false);
  }

  return (
    <Drawer
      open={playbackModalState}
      onOpenChange={open => updatePlaybackModalState(open)}
    >
      <DrawerContent className="h-full pt-[env(safe-area-inset-top)]">
        <div className="flex h-full flex-col items-center justify-between px-6 py-12">
          <div className="flex w-full items-center justify-center">
            <Cover
              albumId={status?.song?.albumId}
              height={400}
              width={400}
              alt={status?.song?.name || ''}
            />
          </div>

          <div className="flex min-h-16 w-full select-none flex-col flex-wrap items-center gap-3 text-center">
            <span className="w-full truncate text-xl font-extrabold">
              {status?.song?.name}
            </span>
            <span
              className="w-full cursor-pointer truncate text-primary/35"
              onClick={toAlbumPage}
            >
              {status?.song?.album}
            </span>
          </div>

          <ProgressPreview
            elapsed={status?.elapsed || 0}
            duration={status?.song?.duration || 0}
          />
          <Control playing={status?.playing || false} />
        </div>
        <DrawerFooter className="bottom-0 flex flex-row flex-nowrap items-center justify-between px-6">
          <Button
            variant="ghost"
            className="size-10 rounded-full p-1"
            onClick={() => ToggleLoop(status?.loopId)}
          >
            <LoopIcon id={status?.loopId || 0} />
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
            onClick={() => openFavorModal(status?.song?.path)}
          >
            <HeartFilledIcon className="size-6" />
            <span className="sr-only">Favor album</span>
          </Button>
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  );
}
