import Settings from '@/components/settings';
import { Button } from '@/components/ui/button.tsx';
import { useSearchStore } from '@/lib/context';
import {
  MagnifyingGlassIcon,
  GearIcon,
  CaretLeftIcon,
} from '@radix-ui/react-icons';
import { lazy, useState } from 'react';
import { Link } from 'react-router';
import { useParams } from 'react-router';
import { useLocation, useNavigate } from 'react-router';

const Search = lazy(() => import('@/components/search'));

interface ISecondaryHeader {
  header?: string;
}

function SecondaryHeader({ header }: ISecondaryHeader) {
  const navigate = useNavigate();

  return (
    <div className="flex w-full max-w-[61.2%] select-none flex-row flex-nowrap items-center space-x-1 overflow-hidden">
      <Button variant="ghost" size="sm" onClick={() => navigate(-1)}>
        <CaretLeftIcon className="size-6 stroke-1 dark:stroke-white [&>path]:stroke-inherit" />
      </Button>
      <span className="truncate font-semibold">{header}</span>
    </div>
  );
}

export default function AppBar() {
  const location = useLocation();
  const { playlistName, name } = useParams();

  const { updateSearchModalState } = useSearchStore();

  const [openSetting, setOpenSetting] = useState(false);

  function Header() {
    const pathname = location.pathname;
    if (pathname.startsWith('/playlists/')) {
      return <SecondaryHeader header={playlistName} />;
    } else if (
      pathname.startsWith('/genres/') ||
      pathname.startsWith('/artists/') ||
      pathname.startsWith('/albumArtists/')
    ) {
      return <SecondaryHeader header={name} />;
    } else if (pathname.startsWith('/albums/')) {
      return <SecondaryHeader header={''} />;
    }
    return (
      <p className="cursor-pointer select-none text-2xl font-semibold">
        <Link to="/">Mesa</Link>
      </p>
    );
  }

  return (
    <>
      <div className="flex max-h-11 flex-row flex-nowrap items-center px-6 pt-3 sm:hidden">
        <Header />
        <div className="flex-1" />
        <Button
          variant="ghost"
          size="sm"
          onClick={() => updateSearchModalState(true)}
        >
          <MagnifyingGlassIcon className="size-6" />
        </Button>
        <Button variant="ghost" size="sm" onClick={() => setOpenSetting(true)}>
          <GearIcon className="size-6" />
        </Button>
      </div>
      <Search />
      <Settings open={openSetting} close={setOpenSetting} />
    </>
  );
}
