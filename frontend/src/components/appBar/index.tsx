import { Button } from '@/components/ui/button.tsx';
import { useDatabaseUpdateSubscription } from '@/hooks/useDatabaseUpdateSubscription.ts';
import { useSearchStore, useSettingStore } from '@/lib/context';
import {
  CaretLeftIcon,
  GearIcon,
  MagnifyingGlassIcon,
} from '@radix-ui/react-icons';
import {
  Link,
  useLocation,
  useParams,
  useRouter,
} from '@tanstack/react-router';
import { lazy, Suspense } from 'react';

const Search = lazy(() => import('@/components/search'));
const Settings = lazy(() => import('@/components/settings'));

interface ISecondaryHeader {
  header?: string;
}

function SecondaryHeader({ header }: ISecondaryHeader) {
  const router = useRouter();

  return (
    <div className="flex w-full max-w-[61.2%] flex-row flex-nowrap items-center space-x-1 overflow-hidden select-none">
      <Button
        variant="ghost"
        size="icon"
        onClick={() => router.history.back()}
        className="p-0"
      >
        <CaretLeftIcon className="size-6 stroke-1 dark:stroke-white [&>path]:stroke-inherit" />
      </Button>
      <span className="truncate font-semibold">{header}</span>
    </div>
  );
}

function Header({ name, pathname }: { name?: string; pathname: string }) {
  if (
    pathname.startsWith('/playlists/') ||
    pathname.startsWith('/genres/') ||
    pathname.startsWith('/artists/') ||
    pathname.startsWith('/albumArtists/')
  ) {
    return <SecondaryHeader header={name} />;
  } else if (pathname.startsWith('/albums/')) {
    return <SecondaryHeader header={''} />;
  }
  return (
    <p className="cursor-pointer font-mono text-2xl font-semibold select-none">
      <Link to="/">Soprano</Link>
    </p>
  );
}

export default function AppBar() {
  useDatabaseUpdateSubscription();
  const location = useLocation();
  const { name } = useParams({ strict: false });

  const { updateSearchModalState } = useSearchStore();
  const { updateSettingModalState } = useSettingStore();

  return (
    <>
      <div className="flex max-h-11 flex-row flex-nowrap items-center px-6 pt-3 sm:hidden">
        <Header name={name} pathname={location.pathname} />
        <div className="flex-1" />
        <Button
          variant="ghost"
          size="icon"
          onClick={() => updateSearchModalState(true)}
          className="p-0"
        >
          <MagnifyingGlassIcon className="size-6" />
        </Button>
        <Button
          variant="ghost"
          size="icon"
          onClick={() => updateSettingModalState(true)}
          className="p-0"
        >
          <GearIcon className="size-6" />
        </Button>
      </div>
      <Suspense>
        <Search />
        <Settings />
      </Suspense>
    </>
  );
}
