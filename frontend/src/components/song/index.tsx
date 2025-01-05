import Cover from '@/components/cover';
import { SwipeActions } from '@/components/swipe';
import { ISong } from '@/type';
import { ReactNode } from 'react';

interface ISongProps {
  song: ISong;
  play: VoidFunction;
  actions?: ReactNode;
}

export function Song({ song, play, actions }: ISongProps) {
  return (
    <SwipeActions.Root className="w-full">
      <SwipeActions.Trigger className="w-full border-0 bg-background">
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
      </SwipeActions.Trigger>
      <SwipeActions.Actions>{actions}</SwipeActions.Actions>
    </SwipeActions.Root>
  );
}
