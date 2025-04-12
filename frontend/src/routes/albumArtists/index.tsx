import { GeneralTagView } from '@/components/general';
import { generalTagQueryOptions } from '@/lib/queries';
import { GeneralTag } from '@/type';
import { createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute('/albumArtists/')({
  loader: ({ context: { queryClient } }) =>
    queryClient.ensureQueryData(
      generalTagQueryOptions(GeneralTag.ALBUM_ARTIST),
    ),
  component: () => <GeneralTagView tag={GeneralTag.ALBUM_ARTIST} />,
});
