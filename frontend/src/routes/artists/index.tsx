import { GeneralTagView } from '@/components/general';
import { LoadingList } from '@/components/loading';
import { generalTagQueryOptions } from '@/lib/queries';
import { GeneralTag } from '@/type';
import { createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute('/artists/')({
  loader: ({ context: { queryClient } }) =>
    queryClient.ensureQueryData(generalTagQueryOptions(GeneralTag.ARTIST)),
  pendingComponent: LoadingList,
  component: () => <GeneralTagView tag={GeneralTag.ARTIST} />,
});
