import { Skeleton } from '@/components/ui/skeleton.tsx';
import { cn } from '@/lib/utils.ts';

export function LoadingSongs() {
  return (
    <div className="flex size-full flex-col overflow-hidden px-6">
      {Array.from({ length: 14 }, (_v, i) => (
        <div
          key={i}
          className="flex w-full select-none flex-row flex-nowrap items-center space-x-2 py-2"
        >
          <Skeleton className="aspect-square size-12 rounded" />
          <div className="flex flex-1 flex-col justify-center gap-1">
            <Skeleton className="h-4 w-full rounded" />
            <Skeleton className="h-3 w-[38.2%] rounded" />
          </div>
        </div>
      ))}
    </div>
  );
}

export function LoadingList() {
  return (
    <div className="flex size-full flex-col overflow-hidden px-6">
      {Array.from({ length: 14 }, (_v, i) => (
        <div
          key={i}
          className="flex w-full select-none flex-col flex-nowrap space-y-1 py-1.5"
        >
          <Skeleton className="h-6 w-full rounded" />
          <Skeleton className="h-5 w-[38.2%] rounded" />
        </div>
      ))}
    </div>
  );
}

export function LoadingAlbums() {
  return (
    <div
      className={cn(
        'm-0 border-0 px-6 py-3',
        'grid grid-flow-dense auto-rows-fr',
        'grid-cols-2 gap-x-8 gap-y-4',
      )}
    >
      {Array.from({ length: 8 }, (_v, i) => (
        <div className="relative isolate w-full" key={i}>
          <Skeleton className="aspect-square h-auto w-full rounded-full" />
          <Skeleton className="mt-1 h-6 w-3/5 rounded" />
        </div>
      ))}
    </div>
  );
}
