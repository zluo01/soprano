import { createFileRoute } from '@tanstack/react-router';
import { GeneralTagView } from '@/components/general';
import { LoadingList } from '@/components/loading';
import { generalTagQueryOptions } from '@/lib/queries';
import { GeneralTag } from '@/type';

export const Route = createFileRoute('/albumArtists/')({
	loader: ({ context: { queryClient } }) =>
		queryClient.ensureQueryData(
			generalTagQueryOptions(GeneralTag.ALBUM_ARTIST),
		),
	pendingComponent: LoadingList,
	component: () => <GeneralTagView tag={GeneralTag.ALBUM_ARTIST} />,
});
