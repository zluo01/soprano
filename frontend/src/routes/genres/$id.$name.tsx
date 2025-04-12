import { GeneralTagAlbumsView } from '@/components/general';
import { generalTagAlbumsQueryOptions } from '@/lib/queries';
import { GeneralTag } from '@/type';
import { createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute('/genres/$id/$name')({
  loader: ({ context: { queryClient }, params: { id } }) => {
    return queryClient.ensureQueryData(
      generalTagAlbumsQueryOptions(GeneralTag.GENRE, id),
    );
  },
  component: GenreAlbumView,
});

function GenreAlbumView() {
  const id = Route.useParams().id;
  return <GeneralTagAlbumsView id={id} tag={GeneralTag.GENRE} />;
}
