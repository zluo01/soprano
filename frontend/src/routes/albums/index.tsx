import { useSuspenseQuery } from '@tanstack/react-query';
import { createFileRoute } from '@tanstack/react-router';
import sortBy from 'lodash-es/sortBy';
import { LoadingAlbums } from '@/components/loading';
import { AlbumGridView } from '@/components/shares';
import { displayAlbumsQueryOptions } from '@/lib/queries';

export const Route = createFileRoute('/albums/')({
	loader: ({ context: { queryClient } }) =>
		queryClient.ensureQueryData(displayAlbumsQueryOptions),
	pendingComponent: LoadingAlbums,
	component: Albums,
});

function Albums() {
	const { data } = useSuspenseQuery(displayAlbumsQueryOptions);
	return (
		<div className="w-full pt-2">
			<AlbumGridView albums={sortBy(data!.Albums, ['name'])} />
		</div>
	);
}
