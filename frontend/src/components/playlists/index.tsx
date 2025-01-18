import Cover from '@/components/cover';
import { LoadingSongs } from '@/components/loading';
import ScrollContainer from '@/components/scroll';
import { SwipeAction } from '@/components/swipe';
import { Button } from '@/components/ui/button.tsx';
import { useCreatePlaylistStore, useRenamePlaylistStore } from '@/lib/context';
import { DeletePlaylistMutation, GetPlaylistsQuery } from '@/lib/queries';
import { Pencil2Icon, PlusIcon, TrashIcon } from '@radix-ui/react-icons';
import { Music2 } from 'lucide-react';
import { lazy, Suspense } from 'react';
import { Link } from 'react-router';

const CreatePlaylistModal = lazy(() => import('@/components/create'));
const RenamePlaylist = lazy(() => import('@/components/rename'));

function PlaylistsView() {
  const { data, isLoading } = GetPlaylistsQuery();
  const mutation = DeletePlaylistMutation();

  const { openRenamePlaylistModal } = useRenamePlaylistStore();
  const { updateCreatePlaylistModalState } = useCreatePlaylistStore();

  if (isLoading) {
    return <LoadingSongs />;
  }
  return (
    <div className="flex h-[calc(100%-100px)] w-full flex-col pt-2">
      <ScrollContainer className="size-full px-6">
        {data?.Playlists.map(o => (
          <SwipeAction
            key={o.name}
            main={
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
            }
            actions={[
              {
                className: 'bg-delete',
                action: () => mutation.mutate({ name: o.name }),
                children: <TrashIcon className="size-6" />,
              },
              {
                className: 'bg-edit',
                action: () => openRenamePlaylistModal(o.name),
                children: <Pencil2Icon className="ml-0.5 size-6" />,
              },
            ]}
          />
        ))}
      </ScrollContainer>
      <div className="static bottom-0 z-10 px-6 py-1.5">
        <Button
          variant="outline"
          className="w-full bg-accent opacity-75"
          onClick={() => updateCreatePlaylistModalState(true)}
        >
          <PlusIcon className="size-6" />
        </Button>
      </div>
    </div>
  );
}

export default function Playlists() {
  return (
    <>
      <PlaylistsView />
      <Suspense>
        <CreatePlaylistModal />
        <RenamePlaylist />
      </Suspense>
    </>
  );
}
