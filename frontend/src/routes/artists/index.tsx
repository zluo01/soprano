import { createFileRoute } from '@tanstack/react-router';
import { GeneralTagView } from '@/components/general';
import { LoadingList } from '@/components/loading';
import { generalTagQueryOptions } from '@/lib/queries';
import { GeneralTag } from '@/type';

export const Route = createFileRoute('/artists/')({
	loader: ({ context: { queryClient } }) =>
		queryClient.ensureQueryData(generalTagQueryOptions(GeneralTag.ARTIST)),
	pendingComponent: LoadingList,
	component: () => <GeneralTagView tag={GeneralTag.ARTIST} />,
});
