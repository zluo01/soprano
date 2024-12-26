import { LoadingAlbums } from '@/components/loading';
import { AlbumGridView } from '@/components/shares';
import { GetDisplayAlbumsQuery } from '@/lib/queries';
import { sortBy } from 'lodash';

export default function Albums() {
  const { data, isLoading } = GetDisplayAlbumsQuery();

  if (isLoading) {
    return <LoadingAlbums />;
  }
  return <AlbumGridView albums={sortBy(data!.Albums, ['name'])} />;
}
