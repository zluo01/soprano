import { LoadingList } from '@/components/loading';
import { GetPlaylistsQuery } from '@/lib/queries';
import { useNavigate } from 'react-router-dom';

export default function Playlists() {
  const navigate = useNavigate();

  const { data, isLoading } = GetPlaylistsQuery();
  if (isLoading) {
    return <LoadingList />;
  }
  return (
    <div className="flex select-none flex-col">
      {data?.Playlists.map(o => (
        <div
          key={o.name}
          className="cursor-pointer py-2 font-medium"
          onClick={() => navigate(`/playlists/${o.name}`)}
        >
          {o.name}
        </div>
      ))}
    </div>
  );
}
