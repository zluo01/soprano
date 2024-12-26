import Cover from '@/components/cover';
import { Button } from '@/components/ui/button.tsx';
import { Separator } from '@/components/ui/separator.tsx';
import { GetAlbumDetailQuery, PlayAlbum, PlaySong } from '@/lib/queries';
import { formatTime } from '@/lib/utils.ts';
import { PlusIcon } from '@radix-ui/react-icons';
import { useParams } from 'react-router';

export default function Album() {
  const { id } = useParams();
  const { data } = GetAlbumDetailQuery(id);

  return (
    <div className="flex size-full select-none flex-col flex-nowrap items-center overflow-y-scroll pb-[72px]">
      <div className="w-full px-6 py-3">
        <Separator />
      </div>

      <div className="mx-0 flex w-[61.8%] items-center justify-center py-3">
        <Cover
          albumId={data?.Album.id}
          height={800}
          width={800}
          alt={data?.Album.name || ''}
        />
      </div>

      <div className="w-full px-6">
        <p className="w-full font-semibold">{data?.Album.name}</p>
        <p className="w-full truncate font-bold">{data?.Album.artist}</p>
        <div className="sticky top-0 z-10 flex w-full flex-row flex-nowrap items-center justify-between border-b bg-background py-3">
          <div className="flex flex-col flex-nowrap bg-inherit text-sm opacity-35">
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

        <div className="flex flex-col pt-2">
          {data?.Album.songs.map(song => (
            <div
              key={song.path}
              className="flex w-full cursor-pointer flex-col flex-nowrap items-start justify-center py-2.5"
              onClick={() => PlaySong(song.path)}
            >
              <span className="truncate font-medium">{song.name}</span>
              <span className="truncate text-sm opacity-35">
                {song.artists}
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
