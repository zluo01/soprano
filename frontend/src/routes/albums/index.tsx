import { LoadingAlbums } from '@/components/loading';
import ScrollContainer from '@/components/scroll';
import { AlbumGridView } from '@/components/shares';
import { displayAlbumsQueryOptions } from '@/lib/queries';
import { useSuspenseQuery } from '@tanstack/react-query';
import { createFileRoute } from '@tanstack/react-router';
import sortBy from 'lodash/sortBy';

export const Route = createFileRoute('/albums/')({
  loader: ({ context: { queryClient } }) =>
    queryClient.ensureQueryData(displayAlbumsQueryOptions),
  component: Albums,
});

function Albums() {
  const { data, isLoading } = useSuspenseQuery(displayAlbumsQueryOptions);

  if (isLoading) {
    return <LoadingAlbums />;
  }
  return (
    <ScrollContainer className="h-[calc(100%-100px)] w-full pt-2">
      <AlbumGridView albums={sortBy(data!.Albums, ['name'])} />
    </ScrollContainer>
  );
}
