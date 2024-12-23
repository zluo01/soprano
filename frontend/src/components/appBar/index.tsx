import Search from '@/components/search';
import Settings from '@/components/settings';
import { Button } from '@/components/ui/button.tsx';
import { useSearchStore } from '@/lib/context';
import {
  MagnifyingGlassIcon,
  GearIcon,
  CaretLeftIcon,
} from '@radix-ui/react-icons';
import { useState } from 'react';
import { Link } from 'react-router';
import { useParams } from 'react-router';
import { useLocation, useNavigate } from 'react-router-dom';

interface ISecondaryHeader {
  header?: string;
}

function SecondaryHeader({ header }: ISecondaryHeader) {
  const navigate = useNavigate();

  return (
    <div className="flex w-full max-w-[61.2%] select-none flex-row flex-nowrap items-center gap-1 overflow-hidden">
      <Button
        size="icon"
        variant="ghost"
        onClick={() => navigate(-1)}
        className="mt-1"
      >
        <CaretLeftIcon className="size-10 stroke-1 dark:stroke-white [&>path]:stroke-inherit" />
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
    if (pathname.startsWith('/playlists')) {
      return <SecondaryHeader header={playlistName} />;
    } else if (
      pathname.startsWith('/genres') ||
      pathname.startsWith('/artists') ||
      pathname.startsWith('/albumArtists')
    ) {
      return <SecondaryHeader header={name} />;
    } else if (pathname.startsWith('/albums')) {
      return <SecondaryHeader header={''} />;
    }
    return (
      <p className="cursor-pointer select-none text-4xl font-semibold">
        <Link to="/">Mesa</Link>
      </p>
    );
  }

  return (
    <>
      <div className="flex flex-row flex-nowrap items-center p-6 sm:hidden">
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