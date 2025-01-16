import PickerItems from '@/components/picker/PickerItems.tsx';
import { cn } from '@/lib/utils.ts';
import { Dispatch, SetStateAction } from 'react';

interface IPickerProps {
  slides: string[];
  select: Dispatch<SetStateAction<number>>;
}

export default function Picker(props: IPickerProps) {
  return (
    <div
      className={cn(
        'relative ml-auto mr-auto flex min-h-full w-full max-w-[30rem]',
        "before:absolute before:left-0 before:right-0 before:block before:z-[1] before:content-[''] before:pointer-events-none before:h-[calc(50%-32px/2)]",
        "after:absolute after:left-0 after:right-0 after:block after:z-[1] after:content-[''] after:pointer-events-none after:h-[calc(50%-32px/2)]",
        'before:-top-[0.5px] before:border-b-[0.5px] before:border-solid before:border-black/30 before:dark:border-white/30 before:bg-gradient-to-t before:from-background/65 before:to-background',
        'after:-bottom-[0.5px] after:border-t-[0.5px] after:border-solid after:border-black/30 after:dark:border-white/30 after:bg-gradient-to-b after:from-background/65 after:to-background',
      )}
    >
      <PickerItems {...props} />
    </div>
  );
}
