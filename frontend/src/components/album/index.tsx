import Cover from '@/components/cover';
import { SwipeActions } from '@/components/swipe';
import { Button } from '@/components/ui/button.tsx';
import { Separator } from '@/components/ui/separator.tsx';
import { useFavorStore } from '@/lib/context';
import {
  AddSongsToQueue,
  GetAlbumDetailQuery,
  PlayAlbum,
  PlaySong,
} from '@/lib/queries';
import { formatTime } from '@/lib/utils.ts';
import { HeartFilledIcon, PlusIcon } from '@radix-ui/react-icons';
import { ListPlus } from 'lucide-react';
import { useParams } from 'react-router';

export default function Album() {
  const { id } = useParams();
  const { data } = GetAlbumDetailQuery(id);

  const { openFavorModal } = useFavorStore();

  return (
    <>
      <div className="w-full px-6 py-3">
        <Separator />
      </div>

      <div className="flex w-full flex-row items-center justify-center py-3">
        <div className="mx-0 aspect-square w-[61.8%]">
          <Cover
            albumId={data?.Album.id}
            height={300}
            width={300}
            alt={data?.Album.name || ''}
          />
        </div>
      </div>

      <div className="w-full px-6">
        <p className="w-full font-semibold">{data?.Album.name}</p>
        <p className="w-full truncate font-bold">{data?.Album.artist}</p>
      </div>

      <div className="sticky top-[calc(env(safe-area-inset-top)+44px)] z-10  px-6">
        <div className="flex w-full flex-row flex-nowrap items-center justify-between bg-background py-3">
          <div className="flex flex-col flex-nowrap text-sm opacity-35">
            <span>Album</span>
            <span>
              {data?.Album.songs.length} songs •{' '}
              {formatTime(data?.Album.totalDuration || 0)}
            </span>
            <span>{data?.Album.date.split('T')[0]}</span>
          </div>

          <div className="flex flex-row flex-nowrap items-center gap-1.5">
            <Button
              variant="ghost"
              size={'icon'}
              className="rounded-full"
              onClick={() =>
                AddSongsToQueue(data?.Album?.songs.map(o => o.path) || [])
              }
              disabled={!data}
            >
              <PlusIcon className="size-7 opacity-35" />
            </Button>
            <Button
              size={'icon'}
              className="size-12 rounded-full"
              onClick={() => PlayAlbum(data?.Album.id)}
              disabled={!data}
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
                fill="currentColor"
                className="size-7"
              >
                <path
                  fillRule="evenodd"
                  d="M4.5 5.653c0-1.427 1.529-2.33 2.779-1.643l11.54 6.347c1.295.712 1.295 2.573 0 3.286L7.28 19.99c-1.25.687-2.779-.217-2.779-1.643V5.653Z"
                  clipRule="evenodd"
                />
              </svg>
            </Button>
          </div>
        </div>
        <Separator />
      </div>

      <div className="flex w-full flex-col px-6 pt-2">
        {data?.Album.songs.map(song => (
          <SwipeActions.Root key={song.path} className="w-full">
            <SwipeActions.Trigger className="w-full cursor-grab border-0 bg-background">
              <div
                className="flex cursor-pointer flex-col flex-nowrap items-start justify-center py-2.5"
                onClick={() => PlaySong(song.path)}
              >
                <p className="w-full truncate font-medium">{song.name}</p>
                <p className="w-full truncate text-sm opacity-35">
                  {song.artists}
                </p>
              </div>
            </SwipeActions.Trigger>
            <SwipeActions.Actions>
              <SwipeActions.Action
                className="bg-add"
                onClick={() => AddSongsToQueue([song.path])}
              >
                <ListPlus className="size-6" />
              </SwipeActions.Action>
              <SwipeActions.Action
                className="bg-favor"
                onClick={() => openFavorModal(song.path)}
              >
                <HeartFilledIcon className="size-6" />
              </SwipeActions.Action>
            </SwipeActions.Actions>
          </SwipeActions.Root>
        ))}
      </div>
    </>
  );
}
