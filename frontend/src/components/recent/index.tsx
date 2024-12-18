import { LoadingAlbums } from '@/components/loading';
import { AlbumGridView } from '@/components/shares';
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

  return <AlbumGridView albums={recentAddedAlbums} />;
}
