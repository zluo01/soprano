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
import { IPlaybackStatus } from '@/type';
import {
  HeartFilledIcon,
  ShuffleIcon,
  ListBulletIcon,
} from '@radix-ui/react-icons';

interface IPlaybackDrawerProps {
  status?: IPlaybackStatus;
}

export default function PlaybackDrawer({ status }: IPlaybackDrawerProps) {
  const { playbackModalState, updatePlaybackModalState } = usePlaybackStore();
  const { updatePlaybackQueueModalState } = usePlaybackQueueStore();
  const { openFavorModal } = useFavorStore();

  return (
    <Drawer
      open={playbackModalState}
      onOpenChange={open => updatePlaybackModalState(open)}
    >
      <DrawerContent className="h-full rounded-2xl pb-[env(safe-area-inset-bottom)] pt-[env(safe-area-inset-top)]">
        <div className="flex h-full flex-col items-center justify-between px-6 pb-20 pt-12">
          <div className="flex w-full items-center justify-center">
            <Cover
              albumId={status?.song?.albumId}
              height={400}
              width={400}
              alt={status?.song?.name || ''}
            />
          </div>

          <div className="flex w-full select-none flex-col  flex-wrap  items-center gap-3 text-center">
            <span className="w-full truncate text-xl font-extrabold">
              {status?.song?.name}
            </span>
            <span className="w-full truncate text-primary/35">
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
          <Button variant="ghost" className="size-10 rounded-full p-1">
            <ShuffleIcon className="size-6" />
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
