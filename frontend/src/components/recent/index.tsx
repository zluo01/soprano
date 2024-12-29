import { LoadingAlbums } from '@/components/loading';
import { AlbumGridView } from '@/components/shares';
import { ScrollArea } from '@/components/ui/scroll-area.tsx';
import { GetDisplayAlbumsQuery } from '@/lib/queries';
import orderBy from 'lodash/orderBy';

export default function RecentlyAdded() {
  const { data, isLoading } = GetDisplayAlbumsQuery();

  if (isLoading) {
    return <LoadingAlbums />;
  }

  const recentAddedAlbums = orderBy(data!.Albums, ['addTime'], 'desc').slice(
    0,
    30,
  );

  return (
    <ScrollArea className="h-[calc(100%-100px)] w-full pt-2">
      <AlbumGridView albums={recentAddedAlbums} />
    </ScrollArea>
  );
}
