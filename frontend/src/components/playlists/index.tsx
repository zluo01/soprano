import Cover from '@/components/cover';
import { LoadingList } from '@/components/loading';
import { SwipeActions } from '@/components/swipe';
import { DeletePlaylist, GetPlaylistsQuery } from '@/lib/queries';
import { Pencil2Icon, TrashIcon } from '@radix-ui/react-icons';
import { Music2 } from 'lucide-react';
import { Link } from 'react-router';

export default function Playlists() {
  const { data, isLoading } = GetPlaylistsQuery();
  if (isLoading) {
    return <LoadingList />;
  }
  return (
    <div className="py-3">
      <div className="flex select-none flex-col px-6">
        {data?.Playlists.map(o => (
          <SwipeActions.Root key={o.name} className="w-full">
            <SwipeActions.Trigger className="w-full cursor-grab border-0">
              <Link to={`/playlists/${o.name}`}>
                <div className=" flex select-none flex-row flex-nowrap items-center space-x-3 bg-background py-2">
                  <Cover
                    albumId={o.coverId}
                    height={50}
                    width={50}
                    alt={o.name}
                    style={'rounded'}
                  />
                  <div className="flex w-[calc(100%-60px)] cursor-pointer select-none flex-col justify-center space-y-1">
                    <p className="truncate font-medium">{o.name}</p>
                    <div className="flex flex-row flex-nowrap items-center gap-1.5 text-sm opacity-35">
                      <Music2 className="size-3" />
                      <span>
                        {o.songCount} album{o.songCount > 1 && 's'}
                      </span>
                    </div>
                  </div>
                </div>
              </Link>
            </SwipeActions.Trigger>
            <SwipeActions.Actions>
              <SwipeActions.Action
                className="bg-delete"
                onClick={() => DeletePlaylist(o.name)}
              >
                <TrashIcon className="size-6" />
              </SwipeActions.Action>
              <SwipeActions.Action className="bg-edit">
                <Pencil2Icon className="ml-0.5 size-6" />
              </SwipeActions.Action>
            </SwipeActions.Actions>
          </SwipeActions.Root>
        ))}
      </div>
    </div>
  );
}
