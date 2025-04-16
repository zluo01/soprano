import { GeneralTagView } from '@/components/general';
import { LoadingList } from '@/components/loading';
import { generalTagQueryOptions } from '@/lib/queries';
import { GeneralTag } from '@/type';
import { createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute('/genres/')({
  loader: ({ context: { queryClient } }) =>
    queryClient.ensureQueryData(generalTagQueryOptions(GeneralTag.GENRE)),
  pendingComponent: LoadingList,
  component: () => <GeneralTagView tag={GeneralTag.GENRE} />,
});
