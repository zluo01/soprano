import Cover from '@/components/cover';
import { LoadingList } from '@/components/loading';
import { ScrollArea } from '@/components/ui/scroll-area.tsx';
import { GetPlaylistsQuery } from '@/lib/queries';
import { Music2 } from 'lucide-react';
import { useNavigate } from 'react-router';

export default function Playlists() {
  const navigate = useNavigate();

  const { data, isLoading } = GetPlaylistsQuery();
  if (isLoading) {
    return <LoadingList />;
  }
  return (
    <ScrollArea className="h-full py-3 pb-[70px]">
      <div className="flex select-none flex-col px-6">
        {data?.Playlists.map(o => (
          <div
            key={o.name}
            className="flex w-full cursor-pointer select-none flex-row flex-nowrap items-center space-x-2 py-2"
            onClick={() => navigate(`/playlists/${o.name}`)}
          >
            <Cover
              albumId={o.coverId}
              height={48}
              width={48}
              alt={o.name}
              style={'rounded'}
            />
            <div className="flex w-[calc(100%-60px)] cursor-pointer select-none flex-col justify-center space-y-1">
              <p className="truncate font-medium">{o.name}</p>
              <div className="flex flex-row flex-nowrap items-center gap-1.5 text-sm opacity-35">
                <Music2 className="size-3" />
                <span>
                  {o.songCount} album{o.songCount > 1 && 's'}
                </span>
              </div>
            </div>
          </div>
        ))}
      </div>
    </ScrollArea>
  );
}
