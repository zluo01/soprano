import { LoadingSongs } from '@/components/loading';
import ScrollContainer from '@/components/scroll';
import { Song } from '@/components/song';
import { Button } from '@/components/ui/button.tsx';
import { Separator } from '@/components/ui/separator.tsx';
import {
  AddSongsToQueue,
  DeleteSongFromPlaylistMutation,
  PlayPlaylist,
  PlaySong,
  songsInPlaylistQueryOptions,
} from '@/lib/queries';
import { PlusIcon, TrashIcon } from '@radix-ui/react-icons';
import { useSuspenseQuery } from '@tanstack/react-query';
import { createFileRoute } from '@tanstack/react-router';
import { ListPlus } from 'lucide-react';

export const Route = createFileRoute('/playlists/$name')({
  loader: ({ context: { queryClient }, params: { name } }) => {
    return queryClient.ensureQueryData(songsInPlaylistQueryOptions(name));
  },
  component: Playlist,
});

function Playlist() {
  const name = Route.useParams().name;
  const { data, isLoading } = useSuspenseQuery(
    songsInPlaylistQueryOptions(name),
  );

  const mutation = DeleteSongFromPlaylistMutation();

  return (
    <ScrollContainer className="h-[calc(100%-45px)] w-full pt-2">
      <div className="sticky top-0 z-20 flex w-full flex-row flex-nowrap items-center gap-2 bg-background px-6 py-3">
        <Separator className="flex-1" />
        <>
          <Button
            variant="ghost"
            size={'icon'}
            className="rounded-full"
            onClick={() =>
              AddSongsToQueue(data?.PlaylistSongs?.map(o => o.path) || [])
            }
            disabled={!data}
          >
            <PlusIcon className="size-6" />
          </Button>
          <Button
            size={'icon'}
            className="rounded-full"
            onClick={() => PlayPlaylist(name)}
            disabled={!data}
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 24 24"
              fill="currentColor"
              className="size-5"
            >
              <path
                fillRule="evenodd"
                d="M4.5 5.653c0-1.427 1.529-2.33 2.779-1.643l11.54 6.347c1.295.712 1.295 2.573 0 3.286L7.28 19.99c-1.25.687-2.779-.217-2.779-1.643V5.653Z"
                clipRule="evenodd"
              />
            </svg>
          </Button>
        </>
      </div>
      {isLoading ? (
        <LoadingSongs />
      ) : (
        <div className="flex size-full flex-col px-6 duration-200 animate-in slide-in-from-right-1/2 sm:animate-none">
          {data?.PlaylistSongs.map(o => (
            <Song
              key={o.path}
              play={() => PlaySong(o.path)}
              song={o}
              actions={[
                {
                  className: 'bg-delete',
                  action: () => mutation.mutate({ name, songPath: o.path }),
                  children: <TrashIcon className="size-6" />,
                },
                {
                  className: 'bg-add',
                  action: () => AddSongsToQueue([o.path]),
                  children: <ListPlus className="ml-1.5 size-6" />,
                },
              ]}
            />
          ))}
        </div>
      )}
    </ScrollContainer>
  );
}
