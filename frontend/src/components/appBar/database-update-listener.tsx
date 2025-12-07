import { OnDatabaseUpdateSubscriptionDocument } from '@/lib/queries';
import { graphQLWSClient } from '@/lib/queries/utils.ts';
import { useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { toast } from 'sonner';

function DatabaseUpdateListener() {
  const queryClient = useQueryClient();

  useEffect(() => {
    const unsubscribe = graphQLWSClient.subscribe<{
      OnDatabaseUpdate: boolean;
    }>(
      {
        query: OnDatabaseUpdateSubscriptionDocument,
      },
      {
        next: data => {
          const updateStatus = data.data?.OnDatabaseUpdate;
          if (updateStatus === true) {
            toast('Finish updating database.');
            queryClient.invalidateQueries();
          } else if (updateStatus === false) {
            toast('Fail to update database.');
          }
        },
        error: () => {
          toast('Fail to update database.');
        },
        complete: () => {
          /* empty */
        },
      },
    );

    return () => {
      unsubscribe();
    };
  }, [queryClient]);

  return null;
}

export default DatabaseUpdateListener;
