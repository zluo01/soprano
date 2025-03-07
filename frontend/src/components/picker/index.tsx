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
        'relative mr-auto ml-auto flex min-h-full w-full max-w-[30rem]',
        "before:pointer-events-none before:absolute before:right-0 before:left-0 before:z-1 before:block before:h-[calc(50%-32px/2)] before:content-['']",
        "after:pointer-events-none after:absolute after:right-0 after:left-0 after:z-1 after:block after:h-[calc(50%-32px/2)] after:content-['']",
        'before:-top-[0.5px] before:border-b-[0.5px] before:border-solid before:border-black/30 before:bg-linear-to-t before:from-background/65 before:to-background dark:before:border-white/30',
        'after:-bottom-[0.5px] after:border-t-[0.5px] after:border-solid after:border-black/30 after:bg-linear-to-b after:from-background/65 after:to-background dark:after:border-white/30',
      )}
    >
      <PickerItems {...props} />
    </div>
  );
}
