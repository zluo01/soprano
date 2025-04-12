import { GeneralTagAlbumsView } from '@/components/general';
import { generalTagAlbumsQueryOptions } from '@/lib/queries';
import { GeneralTag } from '@/type';
import { createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute('/artists/$id/$name')({
  loader: ({ context: { queryClient }, params: { id } }) => {
    return queryClient.ensureQueryData(
      generalTagAlbumsQueryOptions(GeneralTag.ARTIST, id),
    );
  },
  component: ArtistAlbumView,
});

function ArtistAlbumView() {
  const id = Route.useParams().id;
  return <GeneralTagAlbumsView id={id} tag={GeneralTag.ARTIST} />;
}
