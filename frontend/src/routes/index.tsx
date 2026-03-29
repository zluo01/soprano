import { useSuspenseQuery } from '@tanstack/react-query';
import { createFileRoute } from '@tanstack/react-router';
import orderBy from 'lodash-es/orderBy';
import { LoadingAlbums } from '@/components/loading';
import { AlbumGridView } from '@/components/shares';
import { displayAlbumsQueryOptions } from '@/lib/queries';

export const Route = createFileRoute('/')({
	loader: ({ context: { queryClient } }) =>
		queryClient.ensureQueryData(displayAlbumsQueryOptions),
	pendingComponent: LoadingAlbums,
	component: RecentlyAdded,
});

function RecentlyAdded() {
	const { data } = useSuspenseQuery(displayAlbumsQueryOptions);

	const recentAddedAlbums = orderBy(data!.Albums, ['addTime'], 'desc').slice(
		0,
		30,
	);

	return (
		<div className="w-full pt-2">
			<AlbumGridView albums={recentAddedAlbums} />
		</div>
	);
}
