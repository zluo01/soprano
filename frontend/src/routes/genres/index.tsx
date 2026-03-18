import { createFileRoute } from '@tanstack/react-router';
import { GeneralTagView } from '@/components/general';
import { LoadingList } from '@/components/loading';
import { generalTagQueryOptions } from '@/lib/queries';
import { GeneralTag } from '@/type';

export const Route = createFileRoute('/genres/')({
	loader: ({ context: { queryClient } }) =>
		queryClient.ensureQueryData(generalTagQueryOptions(GeneralTag.GENRE)),
	pendingComponent: LoadingList,
	component: () => <GeneralTagView tag={GeneralTag.GENRE} />,
});
