import Cover from '@/components/cover';
import { SwipeAction, SwipeActionProps } from '@/components/swipe';
import { ISong } from '@/type';

interface ISongProps {
  song: ISong;
  play: VoidFunction;
  actions?: SwipeActionProps[];
}

export function Song({ song, play, actions }: ISongProps) {
  return (
    <SwipeAction
      main={
        <div
          className="flex w-full flex-row flex-nowrap items-center space-x-3 py-2"
          onClick={play}
        >
          <Cover
            albumId={song.albumId}
            height={50}
            width={50}
            alt={song.name}
            style={'rounded'}
          />
          <div className="flex w-[calc(100%-60px)] flex-col justify-center">
            <p className="w-full truncate font-medium">{song.name}</p>
            <div className="truncate text-sm opacity-35">{song.artists}</div>
          </div>
        </div>
      }
      actions={actions}
    />
  );
}
