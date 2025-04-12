import { GeneralTagAlbumsView } from '@/components/general';
import { generalTagAlbumsQueryOptions } from '@/lib/queries';
import { GeneralTag } from '@/type';
import { createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute('/albumArtists/$id/$name')({
  loader: ({ context: { queryClient }, params: { id } }) => {
    return queryClient.ensureQueryData(
      generalTagAlbumsQueryOptions(GeneralTag.ALBUM_ARTIST, id),
    );
  },
  component: AlbumArtistAlbumView,
});

function AlbumArtistAlbumView() {
  const id = Route.useParams().id;
  return <GeneralTagAlbumsView id={id} tag={GeneralTag.ALBUM_ARTIST} />;
}
