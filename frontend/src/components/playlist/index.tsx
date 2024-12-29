import { LoadingSongs } from '@/components/loading';
import { Song } from '@/components/song';
import { SwipeActions } from '@/components/swipe';
import { Button } from '@/components/ui/button.tsx';
import { Separator } from '@/components/ui/separator.tsx';
import {
  AddSongsToQueue,
  DeleteSongFromPlaylistMutation,
  GetSongsForPlaylistQuery,
  PlayPlaylist,
  PlaySong,
} from '@/lib/queries';
import { PlusIcon, TrashIcon } from '@radix-ui/react-icons';
import { ListPlus } from 'lucide-react';
import { useParams } from 'react-router';

export default function Playlist() {
  const { playlistName } = useParams();
  const { data, isLoading } = GetSongsForPlaylistQuery(playlistName!);

  const mutation = DeleteSongFromPlaylistMutation();

  return (
    <>
      <div className="sticky top-[calc(env(safe-area-inset-top)+44px)] z-10 flex w-full flex-row flex-nowrap items-center gap-2 bg-background px-6 py-3">
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
            onClick={() => PlayPlaylist(playlistName)}
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
        <div className="flex size-full flex-col px-6">
          {data?.PlaylistSongs.map(o => (
            <Song
              key={o.path}
              play={() => PlaySong(o.path)}
              song={o}
              actions={
                <>
                  <SwipeActions.Action
                    className="bg-delete"
                    onClick={() =>
                      mutation.mutate({ name: playlistName, songPath: o.path })
                    }
                  >
                    <TrashIcon className="size-6" />
                  </SwipeActions.Action>
                  <SwipeActions.Action
                    className="bg-add"
                    onClick={() => AddSongsToQueue([o.path])}
                  >
                    <ListPlus className="ml-1.5 size-6" />
                  </SwipeActions.Action>
                </>
              }
            />
          ))}
        </div>
      )}
    </>
  );
}
