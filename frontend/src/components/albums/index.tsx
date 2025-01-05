import { LoadingAlbums } from '@/components/loading';
import ScrollContainer from '@/components/scroll';
import { AlbumGridView } from '@/components/shares';
import { GetDisplayAlbumsQuery } from '@/lib/queries';
import sortBy from 'lodash/sortBy';

export default function Albums() {
  const { data, isLoading } = GetDisplayAlbumsQuery();

  if (isLoading) {
    return <LoadingAlbums />;
  }
  return (
    <ScrollContainer className="h-[calc(100%-100px)] w-full pt-2">
      <AlbumGridView albums={sortBy(data!.Albums, ['name'])} />
    </ScrollContainer>
  );
}
