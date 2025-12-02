import { Button } from '@/components/ui/button.tsx';
import { nextSong, pauseSong, prevSong } from '@/lib/queries';
import { TrackNextIcon, TrackPreviousIcon } from '@radix-ui/react-icons';

interface IControlProps {
  playing: boolean;
}

export default function Control({ playing }: IControlProps) {
  return (
    <div className="flex flex-row flex-nowrap items-center justify-center space-x-2">
      <Button
        variant="ghost"
        className="size-10 rounded-full p-0"
        onClick={prevSong}
      >
        <TrackPreviousIcon className="size-6" />
      </Button>
      <Button
        variant="ghost"
        className="size-16 rounded-full p-0"
        onClick={pauseSong}
      >
        {playing ? (
          <svg
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 24 24"
            fill="currentColor"
            className="size-14"
          >
            <path
              fillRule="evenodd"
              d="M6.75 5.25a.75.75 0 0 1 .75-.75H9a.75.75 0 0 1 .75.75v13.5a.75.75 0 0 1-.75.75H7.5a.75.75 0 0 1-.75-.75V5.25Zm7.5 0A.75.75 0 0 1 15 4.5h1.5a.75.75 0 0 1 .75.75v13.5a.75.75 0 0 1-.75.75H15a.75.75 0 0 1-.75-.75V5.25Z"
              clipRule="evenodd"
            />
          </svg>
        ) : (
          <svg
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 24 24"
            fill="currentColor"
            className="size-14"
          >
            <path
              fillRule="evenodd"
              d="M4.5 5.653c0-1.427 1.529-2.33 2.779-1.643l11.54 6.347c1.295.712 1.295 2.573 0 3.286L7.28 19.99c-1.25.687-2.779-.217-2.779-1.643V5.653Z"
              clipRule="evenodd"
            />
          </svg>
        )}
      </Button>
      <Button
        variant="ghost"
        className="size-10 rounded-full p-0"
        onClick={nextSong}
      >
        <TrackNextIcon className="size-6" />
      </Button>
    </div>
  );
}
