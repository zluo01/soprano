import ModeToggle from '@/components/theme/mode-toggle.tsx';
import { Button } from '@/components/ui/button';
import {
  Drawer,
  DrawerContent,
  DrawerFooter,
  DrawerHeader,
  DrawerTitle,
} from '@/components/ui/drawer';
import { BuildDatabase, GetStatsQuery } from '@/lib/queries';
import { ReloadIcon, UpdateIcon } from '@radix-ui/react-icons';

interface ISettingsPros {
  open: boolean;
  close: (status: boolean) => void;
}

export default function Settings({ open, close }: ISettingsPros) {
  const { data } = GetStatsQuery();

  return (
    <Drawer open={open} onOpenChange={close}>
      <DrawerContent className="pb-[env(safe-area-inset-bottom)]">
        <DrawerHeader className="text-left">
          <DrawerTitle>Settings</DrawerTitle>
        </DrawerHeader>
        <div className="table w-full select-none px-4">
          <div className="table-header-group">
            <div className="table-row">
              <div className="table-cell py-1.5 text-left font-bold">
                Category
              </div>
              <div className="table-cell py-1.5 text-right font-bold">
                Count
              </div>
            </div>
          </div>
          <div className="table-row-group">
            <div className="table-row">
              <div className="table-cell py-1">Artists</div>
              <div className="table-cell py-1  text-right opacity-50">
                {data?.Stats.artists || 'Unknown'}
              </div>
            </div>
            <div className="table-row">
              <div className="table-cell py-1">Albums</div>
              <div className="table-cell py-1 text-right opacity-50 ">
                {data?.Stats.albums || 'Unknown'}
              </div>
            </div>
            <div className="table-row">
              <div className="table-cell py-1">Songs</div>
              <div className="table-cell py-1 text-right opacity-50 ">
                {data?.Stats.songs || 'Unknown'}
              </div>
            </div>
          </div>
        </div>
        <DrawerFooter className="flex flex-row items-center justify-around py-3">
          <Button variant="outline" className="size-12 rounded-full p-2">
            <UpdateIcon className="size-[1.2rem]" />
          </Button>
          <Button
            variant="outline"
            className="size-12 rounded-full p-2"
            onClick={BuildDatabase}
          >
            <ReloadIcon className="size-[1.2rem]" />
          </Button>
          <ModeToggle />
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  );
}
