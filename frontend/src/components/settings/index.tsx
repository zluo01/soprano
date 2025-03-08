import ModeToggle from '@/components/theme/mode-toggle.tsx';
import { Button } from '@/components/ui/button';
import {
  Drawer,
  DrawerContent,
  DrawerFooter,
  DrawerHeader,
  DrawerTitle,
} from '@/components/ui/drawer';
import { useSettingStore } from '@/lib/context';
import { GetStatsQuery, UpdateDatabase } from '@/lib/queries';
import { UpdateIcon } from '@radix-ui/react-icons';
import { toast } from 'sonner';

export default function Settings() {
  const { settingModalState, updateSettingModalState } = useSettingStore();
  const { data } = GetStatsQuery();

  return (
    <Drawer
      open={settingModalState}
      onOpenChange={status => updateSettingModalState(status)}
    >
      <DrawerContent>
        <DrawerHeader className="text-left">
          <DrawerTitle>Settings</DrawerTitle>
        </DrawerHeader>
        <div className="table w-full px-4 select-none">
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
              <div className="table-cell py-1 text-right opacity-50">
                {data?.Stats.artists || 'Unknown'}
              </div>
            </div>
            <div className="table-row">
              <div className="table-cell py-1">Albums</div>
              <div className="table-cell py-1 text-right opacity-50">
                {data?.Stats.albums || 'Unknown'}
              </div>
            </div>
            <div className="table-row">
              <div className="table-cell py-1">Songs</div>
              <div className="table-cell py-1 text-right opacity-50">
                {data?.Stats.songs || 'Unknown'}
              </div>
            </div>
          </div>
        </div>
        <DrawerFooter className="flex flex-row justify-around pb-4">
          <Button
            variant="outline"
            className="size-12 rounded-full p-2"
            onClick={() =>
              UpdateDatabase().then(() => toast('Updating database.'))
            }
          >
            <UpdateIcon className="size-[1.2rem]" />
          </Button>
          <ModeToggle />
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  );
}
