import { cn } from '@/lib/utils.ts';
import { HTMLProps, ReactNode, useCallback, useEffect, useRef } from 'react';

import { usePickerActions, usePickerData } from './Picker';
import { useColumnData } from './PickerColumn';

interface PickerItemRenderProps {
  selected: boolean;
}

export interface PickerItemProps
  extends Omit<HTMLProps<HTMLDivElement>, 'value' | 'children'> {
  children: ReactNode | ((renderProps: PickerItemRenderProps) => ReactNode);
  value: string | number;
}

// eslint-disable-next-line
function isFunction(functionToCheck: any): functionToCheck is Function {
  return typeof functionToCheck === 'function';
}

function PickerItem({
  className,
  children,
  value,
  ...restProps
}: PickerItemProps) {
  const optionRef = useRef<HTMLDivElement | null>(null);
  const { value: pickerValue } = usePickerData('Picker.Item');
  const pickerActions = usePickerActions('Picker.Item');
  const { key } = useColumnData('Picker.Item');

  useEffect(
    () => pickerActions.registerOption(key, { value, element: optionRef }),
    [key, pickerActions, value],
  );

  const handleClick = useCallback(() => {
    pickerActions.change(key, value);
  }, [pickerActions, key, value]);

  return (
    <div
      className={cn('flex h-9 items-center justify-center', className)}
      ref={optionRef}
      onClick={handleClick}
      {...restProps}
    >
      {isFunction(children)
        ? children({ selected: pickerValue[key] === value })
        : children}
    </div>
  );
}

export default PickerItem;
